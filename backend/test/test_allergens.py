"""The allergen catalog and the weekly menu check. Jev is replaced by FakeDetector: no network."""
import pytest

from app.allergens.detector import AllergenRef, Meal
from app.allergens.names import is_no_allergy, normalize
from app.database import models as m
from app.main import app


class FakeDetector:
    """Answers from a fixed table: meal name -> {allergen name: probability}; anything else is 0."""

    def __init__(self, table=None, fail=False):
        self.table = table or {}
        self.fail = fail
        self.calls: list[tuple[list[Meal], list[AllergenRef]]] = []

    def detect(self, meals, allergens):
        self.calls.append((meals, allergens))
        if self.fail:
            raise RuntimeError("Jev is down")
        return [{a.name: self.table.get(meal.name, {}).get(a.name, 0.0) for a in allergens} for meal in meals]


@pytest.fixture
def detector(client):
    fake = FakeDetector()
    app.state.allergen_detector = fake
    yield fake
    app.state.allergen_detector = None


def check(client, h, estancia, *meals):
    return client.post(f"/estancias/{estancia.id}/menu-check", headers=h, json={"meals": list(meals)})


def allergic(db, child, allergies):
    child.allergies = allergies
    db.commit()


def test_catalog_is_seeded_and_any_user_reads_it(client, world, auth):
    r = client.get("/allergens", headers=auth(world.p1))
    assert r.status_code == 200
    names = [a["name"] for a in r.json()]
    assert {"Cacahuate", "Leche y lácteos", "Huevo", "Trigo y gluten"} <= set(names)
    assert names == sorted(names, key=normalize)  # the database sorts ignoring accents
    assert all("," not in n for n in names)  # a child's allergies are stored comma separated
    assert client.get("/allergens").status_code == 401


def test_conflicts_name_the_children_allergens_and_meals(client, db, world, auth, detector):
    allergic(db, world.c1, "Cacahuate,Leche y lácteos")
    allergic(db, world.c3, "huevo")  # case and accents don't matter
    allergic(db, world.c4, "Cacahuate")  # other estancia: never listed
    detector.table = {
        "Mole con pollo": {"Cacahuate": 0.9, "Ajonjolí": 0.8},
        "Hot cakes": {"Leche y lácteos": 0.7, "Huevo": 0.95, "Trigo y gluten": 0.97},
    }
    r = check(client, auth(world.admin1), world.e1,
              {"name": "Mole con pollo", "ingredients": ["cacahuate", "ajonjolí"]},
              {"name": "Hot cakes"}, {"name": "Agua de jamaica"})
    assert r.status_code == 200, r.text
    body = r.json()
    assert [d["name"] for d in body["detected"]] == ["Trigo y gluten", "Huevo", "Cacahuate", "Ajonjolí",
                                                    "Leche y lácteos"]
    assert body["detected"][2] == {"name": "Cacahuate", "probability": 0.9, "meals": ["Mole con pollo"]}
    by_child = {c["full_name"]: c for c in body["conflicts"]}
    assert set(by_child) == {"c1", "c3"}
    assert by_child["c1"]["salon_name"] == "S1"
    assert by_child["c1"]["conflicts"] == [{"allergen": "Cacahuate", "meals": ["Mole con pollo"]},
                                           {"allergen": "Leche y lácteos", "meals": ["Hot cakes"]}]
    assert by_child["c3"]["conflicts"] == [{"allergen": "huevo", "meals": ["Hot cakes"]}]
    meals, _ = detector.calls[0]
    assert meals[0] == Meal("Mole con pollo", ["cacahuate", "ajonjolí"])


def test_allergen_in_several_meals_lists_them_all(client, db, world, auth, detector):
    allergic(db, world.c1, "Huevo")
    detector.table = {"Hot cakes": {"Huevo": 0.6}, "Flan": {"Huevo": 0.99}}
    body = check(client, auth(world.admin1), world.e1, {"name": "Hot cakes"}, {"name": "Flan"}).json()
    assert body["detected"] == [{"name": "Huevo", "probability": 0.99, "meals": ["Hot cakes", "Flan"]}]
    assert body["conflicts"][0]["conflicts"] == [{"allergen": "Huevo", "meals": ["Hot cakes", "Flan"]}]


def test_below_threshold_is_not_a_conflict(client, db, world, auth, detector):
    allergic(db, world.c1, "Fresa")
    detector.table = {"Gelatina": {"Fresa": 0.34}}
    body = check(client, auth(world.admin1), world.e1, {"name": "Gelatina"}).json()
    assert body == {"detected": [], "conflicts": []}
    detector.table = {"Gelatina": {"Fresa": 0.35}}
    assert check(client, auth(world.admin1), world.e1, {"name": "Gelatina"}).json()["conflicts"][0]["full_name"] == "c1"


def test_allergies_outside_the_catalog_are_checked_too(client, db, world, auth, detector):
    allergic(db, world.c2, "Pimienta")
    detector.table = {"Bistec": {"Pimienta": 0.8}}
    body = check(client, auth(world.admin1), world.e1, {"name": "Bistec", "ingredients": ["pimienta negra"]}).json()
    _, allergens = detector.calls[0]
    assert AllergenRef("Pimienta", "Pimienta") in allergens
    assert body["conflicts"][0]["conflicts"] == [{"allergen": "Pimienta", "meals": ["Bistec"]}]


def test_no_allergy_entries_are_never_a_conflict(client, db, world, auth, detector):
    """The old app pre-filled "Sin alergias conocidas"; Jev must not be asked whether a meal contains it."""
    allergic(db, world.c1, "Sin alergias conocidas")
    allergic(db, world.c2, "Ninguna,Huevo")
    detector.table = {"Pollo con arroz": {"Sin alergias conocidas": 0.9, "Ninguna": 0.9, "Huevo": 0.5}}
    body = check(client, auth(world.admin1), world.e1, {"name": "Pollo con arroz"}).json()
    _, allergens = detector.calls[0]
    assert not any(is_no_allergy(a.name) for a in allergens)
    assert [(c["full_name"], c["conflicts"]) for c in body["conflicts"]] == \
        [("c2", [{"allergen": "Huevo", "meals": ["Pollo con arroz"]}])]


def test_deleted_children_are_left_out(client, db, world, auth, detector):
    allergic(db, world.c1, "Huevo")
    world.c1.status = m.RecordStatus.DELETED
    db.commit()
    detector.table = {"Flan": {"Huevo": 0.99}}
    assert check(client, auth(world.admin1), world.e1, {"name": "Flan"}).json()["conflicts"] == []


def test_only_the_estancias_admin_or_org(client, world, auth, detector):
    meal = {"name": "Sopa"}
    assert check(client, auth(world.admin2), world.e1, meal).status_code == 404
    assert check(client, auth(world.d1), world.e1, meal).status_code == 403
    assert check(client, auth(world.p1), world.e1, meal).status_code == 403
    assert check(client, auth(world.org), world.e1, meal).status_code == 200


def test_bad_bodies(client, world, auth, detector):
    h = auth(world.admin1)
    assert client.post(f"/estancias/{world.e1.id}/menu-check", headers=h, json={"meals": []}).status_code == 422
    assert check(client, h, world.e1, {"name": "   "}).status_code == 422
    assert check(client, h, world.e1, *[{"name": f"m{i}"} for i in range(31)]).status_code == 422
    r = check(client, h, world.e1, {"name": " Sopa ", "ingredients": [" fideo ", "", "fideo"]})
    assert r.status_code == 200
    assert detector.calls[-1][0] == [Meal("Sopa", ["fideo"])]


def test_unconfigured_or_failing_jev(client, world, auth):
    h = auth(world.admin1)
    app.state.allergen_detector = None
    assert check(client, h, world.e1, {"name": "Sopa"}).status_code == 503
    app.state.allergen_detector = FakeDetector(fail=True)
    try:
        assert check(client, h, world.e1, {"name": "Sopa"}).status_code == 502
    finally:
        app.state.allergen_detector = None


def test_is_no_allergy():
    for text in ["Sin alergias conocidas", "sin alergias registradas", "Ninguna", "NINGUNO", "No", "N/A", "na",
                 "Nada", "No tiene"]:
        assert is_no_allergy(text), text
    for text in ["Nueces y frutos secos", "Nopal", "Naranja", "Leche y lácteos", "Soya"]:
        assert not is_no_allergy(text), text


def test_normalize():
    assert normalize(" Lácteos ") == normalize("LACTEOS") == "lacteos"
    assert normalize("Ajonjolí") == "ajonjoli"
