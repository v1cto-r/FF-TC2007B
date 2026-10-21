"""Weekly PDF report of a child's daily logs."""
from datetime import date

import pytest

from app.reports import weekly

from .test_logs_api import CHECK_IN, CHECK_OUT, STAY, put_log


def report(client, headers, child, year=2026, week=10):
    return client.get("/daily-logs/report", headers=headers,
                      params=dict(child_id=str(child.id), year=year, week=week))


def test_week_range_is_monday_to_friday_of_the_iso_week():
    assert weekly.week_range(2026, 10) == (date(2026, 3, 2), date(2026, 3, 6))
    assert weekly.week_range(2026, 1) == (date(2025, 12, 29), date(2026, 1, 2))
    assert weekly.week_range(2026, 53) == (date(2026, 12, 28), date(2027, 1, 1))
    with pytest.raises(ValueError):
        weekly.week_range(2025, 53)


def test_docente_gets_the_week_as_a_pdf(client, world, auth):
    h = auth(world.d1)
    assert put_log(client, h, world.c1, check_in=CHECK_IN, stay=STAY, check_out=CHECK_OUT).status_code == 201
    assert put_log(client, h, world.c1, day="2026-03-04", check_in=CHECK_IN).status_code == 201
    r = report(client, h, world.c1)
    assert r.status_code == 200
    assert r.headers["content-type"] == "application/pdf"
    assert r.content.startswith(b"%PDF-")
    assert "2026-W10.pdf" in r.headers["content-disposition"]


def test_an_empty_week_is_still_a_report(client, world, auth):
    r = report(client, auth(world.d1), world.c1, week=20)
    assert r.status_code == 200 and r.content.startswith(b"%PDF-")


def test_parent_can_get_the_report_of_their_child(client, world, auth):
    assert report(client, auth(world.p1), world.c1).status_code == 200


def test_children_out_of_scope_are_not_found(client, world, auth):
    h = auth(world.d1)
    assert report(client, h, world.c3).status_code == 404
    assert report(client, h, world.c4).status_code == 404
    assert report(client, auth(world.p2), world.c1).status_code == 404


def test_a_week_that_does_not_exist_is_rejected(client, world, auth):
    h = auth(world.d1)
    assert report(client, h, world.c1, year=2025, week=53).status_code == 422
    assert report(client, h, world.c1, week=60).status_code == 422
