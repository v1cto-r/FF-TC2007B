import secrets
from pathlib import Path

import bcrypt

WORDLIST_PATH = Path(__file__).with_name("wordlist.txt")
WORDS: list[str] = []  # filled by load_wordlist() when the API starts


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def verify_password(password: str, password_hash: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode(), password_hash.encode())
    except ValueError:  # e.g. password longer than bcrypt's 72 bytes
        return False


def load_wordlist(path: Path = WORDLIST_PATH) -> None:
    """Read the words (one per line) into memory."""
    WORDS[:] = [w for w in (line.strip() for line in path.read_text(encoding="utf-8").splitlines()) if w]
    if not WORDS:
        raise RuntimeError(f"Wordlist {path} is empty")


def placeholder_password() -> str:
    """Three random words joined by dashes, e.g. "pero-bien-para"."""
    if not WORDS:
        raise RuntimeError("Wordlist not loaded")
    return "-".join(secrets.choice(WORDS) for _ in range(3))


# Checked when the email doesn't exist, so unknown emails take as long as wrong passwords
DUMMY_HASH = hash_password("dummy")
