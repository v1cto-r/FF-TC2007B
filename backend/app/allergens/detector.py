"""Which allergens a meal may contain, judged by Jev (TypeSafe System One).

One request per meal, one yes/no question (Noul) per allergen: the answer is the probability that the meal
contains it. Meals are checked a few at a time over one shared client, closed on app shutdown.
"""
import unicodedata
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from typing import Protocol

from typesafe_sdk import Noul, NoulCriteria, TypeSafeClient

# At or above this a meal counts as containing the allergen. Low on purpose: a missed allergen is worse
# than a false alarm, and the admin sees the probability.
THRESHOLD = 0.35
MAX_PARALLEL = 4


@dataclass(frozen=True)
class Meal:
    name: str
    ingredients: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class AllergenRef:
    name: str
    keywords: str


class AllergenDetector(Protocol):
    def detect(self, meals: list[Meal], allergens: list[AllergenRef]) -> list[dict[str, float]]:
        """For each meal (same order), allergen name -> probability that the meal contains it."""
        ...


def normalize(text: str) -> str:
    """Case and accent insensitive key: "Lácteos " and "lacteos" match."""
    decomposed = unicodedata.normalize("NFKD", text.strip().casefold())
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def _questions(allergens: list[AllergenRef]) -> dict[str, Noul]:
    return {
        f"a{i}": Noul(
            instructions=(f"¿El platillo `platillo` contiene o puede contener {a.name}, ya sea como ingrediente "
                          f"directo, derivado u oculto en su preparación habitual?"),
            criteria=NoulCriteria(
                true=f"El platillo o alguno de sus ingredientes es o suele llevar {a.name} (p. ej. {a.keywords})",
                false=f"Ni el platillo ni sus ingredientes llevan {a.name}",
            ),
        )
        for i, a in enumerate(allergens)
    }


class JevDetector:
    def __init__(self, api_key: str, model: str = "jev-latest", timeout: float = 30):
        self._client = TypeSafeClient(api_key=api_key, model=model, timeout=timeout)

    def _detect_one(self, meal: Meal, allergens: list[AllergenRef], questions: dict[str, Noul]) -> dict[str, float]:
        state = {"platillo": {"nombre": meal.name, "ingredientes_y_posibles_alergenos": meal.ingredients}}
        response = self._client.system_one(state=state, questions=questions)
        return {a.name: response.nouls[f"a{i}"].noul for i, a in enumerate(allergens)}

    def detect(self, meals: list[Meal], allergens: list[AllergenRef]) -> list[dict[str, float]]:
        if not meals or not allergens:
            return [{} for _ in meals]
        questions = _questions(allergens)
        with ThreadPoolExecutor(max_workers=min(MAX_PARALLEL, len(meals))) as pool:
            return list(pool.map(lambda m: self._detect_one(m, allergens, questions), meals))

    def close(self) -> None:
        self._client.close()
