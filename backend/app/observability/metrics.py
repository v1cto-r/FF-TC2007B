"""Prometheus metrics, served on their own port (METRICS_PORT) so they never go through the public API.

HTTP metrics (requests/s, latency, status codes and requests in progress, per route) come from
prometheus-fastapi-instrumentator. Ours: writes per table, database pool usage, open chat sockets.
"""
from prometheus_client import REGISTRY, Counter, start_http_server
from prometheus_client.core import GaugeMetricFamily
from prometheus_fastapi_instrumentator import Instrumentator

# Exposed as ff_record_writes_total; counted by the write log (see write_log.py)
RECORD_WRITES = Counter("ff_record_writes", "Creates / updates / deletes of records", ["resource", "action"])


def instrument(app) -> None:
    Instrumentator(
        should_group_status_codes=False,
        should_instrument_requests_inprogress=True,
        excluded_handlers=["/health"],          
    ).instrument(app)


class _StateCollector:
    """Values read at scrape time, so they can't drift from the real state."""

    def __init__(self, engine, hub) -> None:
        self.engine = engine
        self.hub = hub

    def collect(self):
        pool = self.engine.pool
        yield GaugeMetricFamily("ff_db_pool_size", "Connections the pool keeps open", value=pool.size())
        yield GaugeMetricFamily("ff_db_pool_checked_out", "Connections in use right now", value=pool.checkedout())
        yield GaugeMetricFamily("ff_db_pool_open", "Connections open to Postgres (idle + in use)",
                                value=pool.checkedin() + pool.checkedout())
        yield GaugeMetricFamily("ff_chat_connections", "Open chat sockets", value=self.hub.count())


def register_state(engine, hub) -> None:
    REGISTRY.register(_StateCollector(engine, hub))


def serve(port: int):
    """Starts the /metrics server in a background thread. Returns it so the caller can shut it down."""
    server, _ = start_http_server(port)
    return server
