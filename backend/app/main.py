import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app import starter
from app.config import settings
from app.routers import storage as storage_router
from app.storage import StorageService
from app.storage.policy import load_policy
from app.storage.rate_limit import InMemoryRateLimiter

# Toda esta función corre cuando se inicia fastapi
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Creates the DB/tables if empty (and sample data when DEBUG/TEST is set). Blocking, so off the event loop.
    await asyncio.to_thread(starter.run)
    app.state.storage = StorageService(settings, load_policy(settings), InMemoryRateLimiter())
    try:
        app.state.storage.ensure_buckets()
    except Exception as e:  # storage down shouldn't take the whole API down
        logging.getLogger("uvicorn.error").warning("Could not ensure S3 buckets: %s", e)
    yield

app = FastAPI(title="ff_api", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], #TODO: ** Cambiar a que solo la app la pueda mandar a llamar
    allow_credentials=True,
    allow_methods=["POST", "GET", "PUT", "PATCH", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(storage_router.router)

@app.get("/health")
def health():
    return {"status": "ok"}
