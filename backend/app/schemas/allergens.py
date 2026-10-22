import uuid
from typing import Annotated

from pydantic import BaseModel, Field, field_validator

from app.schemas.common import ORMModel

MealName = Annotated[str, Field(min_length=1, max_length=200)]
Ingredient = Annotated[str, Field(max_length=200)]


class AllergenOut(ORMModel):
    id: uuid.UUID
    name: str
    keywords: str


class MealIn(BaseModel):
    """A dish of the week ("Sopa de fideo") and, optionally, ingredients or anything that could cause a reaction."""
    name: MealName
    ingredients: list[Ingredient] = Field(default_factory=list, max_length=30)

    @field_validator("name")
    @classmethod
    def _strip_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("the meal needs a name")
        return v.strip()

    @field_validator("ingredients")
    @classmethod
    def _clean(cls, items: list[str]) -> list[str]:
        return list(dict.fromkeys(i.strip() for i in items if i.strip()))


class MenuCheckIn(BaseModel):
    meals: list[MealIn] = Field(min_length=1, max_length=30)


class DetectedAllergen(BaseModel):
    """An allergen at least one meal may contain. `probability` is the highest among those meals."""
    name: str
    probability: float
    meals: list[str]


class AllergenConflict(BaseModel):
    allergen: str
    meals: list[str]


class ChildConflict(BaseModel):
    child_id: uuid.UUID
    full_name: str
    salon_name: str | None
    conflicts: list[AllergenConflict]


class MenuCheckOut(BaseModel):
    detected: list[DetectedAllergen]
    conflicts: list[ChildConflict]
