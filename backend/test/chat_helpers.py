"""Sending chat messages in tests: opens the socket, sends one frame and answers like the old REST route did."""
import uuid
from contextlib import contextmanager


class Sent:
    """The outcome of one message: `status_code` is 201 (stored), 200 (a retry of a stored one) or the error's."""

    def __init__(self, frame):
        self.frame = frame
        if frame["type"] == "ack":
            self.status_code, self._json = (201 if frame["created"] else 200), frame["message"]
        else:
            self.status_code, self._json = frame["status"], {"detail": frame["detail"]}

    def json(self):
        return self._json


def frame_for(body: dict, mid=None) -> dict:
    return {"type": "message", "id": str(mid or uuid.uuid4()), **body}


def ws_send(client, headers, body, mid=None) -> Sent:
    with connect(client, headers) as ws:
        ws.send_json(frame_for(body, mid))
        return Sent(ws.receive_json())


@contextmanager
def connect(client, headers):
    """An open chat socket, after the server said it is ready (authenticated and registered to receive)."""
    with client.websocket_connect("/ws/chat", headers=headers) as ws:
        assert ws.receive_json() == {"type": "ready"}
        yield ws
