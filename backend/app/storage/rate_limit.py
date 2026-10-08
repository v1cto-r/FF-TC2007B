import threading
import time
from collections import defaultdict, deque
from typing import Callable, Protocol

from .errors import RateLimitExceeded
from .policy import Limit


class RateLimiter(Protocol):
    def check(self, user_id: str, action: str, limit: Limit) -> None: ...


class InMemoryRateLimiter:
    """Per-process sliding window (minute) plus daily counter. Swap for Redis to share across workers."""

    def __init__(self, clock: Callable[[], float] = time.time):
        self._clock = clock
        self._lock = threading.Lock()
        self._minute: dict[tuple[str, str], deque[float]] = defaultdict(deque)
        self._day: dict[tuple[str, str], deque[float]] = defaultdict(deque)

    @staticmethod
    def _prune(q: deque[float], cutoff: float) -> None:
        while q and q[0] <= cutoff:
            q.popleft()

    def check(self, user_id: str, action: str, limit: Limit) -> None:
        key = (user_id, action)
        now = self._clock()
        with self._lock:
            minute, day = self._minute[key], self._day[key]
            self._prune(minute, now - 60)
            self._prune(day, now - 86400)
            if limit.per_minute is not None and len(minute) >= limit.per_minute:
                raise RateLimitExceeded(max(1, int(minute[0] + 60 - now) + 1))
            if limit.per_day is not None and len(day) >= limit.per_day:
                raise RateLimitExceeded(max(1, int(day[0] + 86400 - now) + 1))
            minute.append(now)
            day.append(now)
