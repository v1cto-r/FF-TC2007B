from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Toda esta función corre cuando se inicia fastapi
@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

app = FastAPI(title="ff_api", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], #TODO: ** Cambiar a que solo la app la pueda mandar a llamar
    allow_credentials=True,
    allow_methods=["POST", "GET", "PUT", "PATCH", "OPTIONS"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"status": "ok"}
