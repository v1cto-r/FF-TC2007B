"""Write logs on stdout and Prometheus metrics."""
import json
import logging
import urllib.request
from datetime import date

import pytest
from prometheus_client import REGISTRY

from app.database import models as m
from app.observability import metrics, write_log


@pytest.fixture
def lines():
    """The dicts written to the write log during the test."""
    captured = []

    class Capture(logging.Handler):
        def emit(self, record):
            captured.append(record.msg)

    handler = Capture()
    write_log.log.addHandler(handler)
    yield captured
    write_log.log.removeHandler(handler)


def of(lines, event):
    return [line for line in lines if line["event"] == event]


# --- write log ------------------------------------------------------------------------------

def test_a_write_logs_the_request_and_each_changed_record(client, world, auth, lines):
    r = client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1), json={"name": "Renamed", "address": "Calle 9"})
    assert r.status_code == 200
    [http] = of(lines, "http_write")
    assert http["method"] == "PUT" and http["route"] == "/estancias/{estancia_id}" and http["status"] == 200
    assert http["actor_id"] == str(world.admin1.id) and http["request_id"] == r.headers["x-request-id"]
    [record] = of(lines, "record_write")
    assert record == {"event": "record_write", "request_id": http["request_id"], "action": "UPDATE",
                      "resource": "estancias", "resource_id": str(world.e1.id), "actor_id": str(world.admin1.id),
                      "fields": ["address", "name"]}


def test_the_log_names_fields_but_never_their_values(client, world, auth, lines):
    client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1), json={"name": "Secret name", "address": "Calle 9"})
    assert lines and "Secret name" not in json.dumps(lines)


def test_reads_are_not_logged(client, world, auth, lines):
    r = client.get(f"/estancias/{world.e1.id}", headers=auth(world.admin1))
    assert r.status_code == 200 and r.headers["x-request-id"]
    assert lines == []


def test_a_refused_write_logs_the_request_but_no_record(client, world, auth, lines):
    r = client.put(f"/estancias/{world.e2.id}", headers=auth(world.admin1), json={"name": "Hijack"})
    assert r.status_code == 404
    assert [(line["event"], line["status"]) for line in lines] == [("http_write", 404)]


def test_writes_without_a_request_are_logged_as_the_system(db, world, lines):
    db.add(m.Notice(estancia_id=world.e1.id, name="n", description="d", date=date(2026, 1, 1)))
    db.commit()
    [record] = lines
    assert record["action"] == "CREATE" and record["actor_id"] is None and record["request_id"] is None


# --- metrics --------------------------------------------------------------------------------

def test_metrics_count_requests_and_writes(client, world, auth):
    http = {"handler": "/estancias/{estancia_id}", "method": "PUT", "status": "200"}
    writes = {"resource": "estancias", "action": "UPDATE"}
    before = (REGISTRY.get_sample_value("http_requests_total", http) or 0,
              REGISTRY.get_sample_value("ff_record_writes_total", writes) or 0)
    client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1), json={"name": "Renamed"})
    assert REGISTRY.get_sample_value("http_requests_total", http) == before[0] + 1
    assert REGISTRY.get_sample_value("ff_record_writes_total", writes) == before[1] + 1


def test_metrics_report_pool_and_chat_state():
    for name in ("ff_db_pool_size", "ff_db_pool_checked_out", "ff_db_pool_open"):
        assert REGISTRY.get_sample_value(name) is not None, name
    assert REGISTRY.get_sample_value("ff_chat_connections") == 0


def test_metrics_are_served_on_their_own_port():
    server = metrics.serve(0)  # any free port
    try:
        body = urllib.request.urlopen(f"http://127.0.0.1:{server.server_port}/metrics", timeout=5).read().decode()
    finally:
        server.shutdown()
        server.server_close()
    assert "ff_db_pool_checked_out" in body and "ff_chat_connections" in body
