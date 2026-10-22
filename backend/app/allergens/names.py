"""Comparing allergy names the way people type them."""
import re
import unicodedata

# "Sin alergias conocidas", "Ninguna", "No", "N/A"...: the old app pre-filled the first one, so many children have it
_NO_ALLERGY = re.compile(r"^(sin\b|ningun|no\b|n/?a$|nada$)")


def normalize(text: str) -> str:
    """Case and accent insensitive key: "Lácteos " and "lacteos" match."""
    decomposed = unicodedata.normalize("NFKD", text.strip().casefold())
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def is_no_allergy(text: str) -> bool:
    """True for entries that say the child has no allergies rather than naming one."""
    return bool(_NO_ALLERGY.match(normalize(text)))
