import gc
from typing import List, Optional

import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()

model: Optional[SentenceTransformer] = None
MODEL_NAME = "intfloat/multilingual-e5-large"


class EmbedRequest(BaseModel):
    texts: List[str]


@app.post("/load")
def load_model():
    global model

    if model is not None:
        return {"status": "already_loaded"}

    model = SentenceTransformer(MODEL_NAME, device="cuda")

    return {
        "status": "loaded",
        "model": MODEL_NAME,
        "device": "cuda"
    }


@app.post("/embed")
def embed(req: EmbedRequest):
    global model

    if model is None:
        raise HTTPException(status_code=409, detail="Model not loaded. Call /load first.")

    embeddings = model.encode(
        req.texts,
        normalize_embeddings=True,
        batch_size=32,
        show_progress_bar=False,
    )

    return {
        "dim": embeddings.shape[1],
        "count": len(req.texts),
        "embeddings": embeddings.tolist()
    }


@app.post("/unload")
def unload_model():
    global model

    if model is None:
        return {"status": "already_unloaded"}

    del model
    model = None

    gc.collect()

    if torch.cuda.is_available():
        torch.cuda.empty_cache()
        torch.cuda.ipc_collect()

    return {"status": "unloaded"}


@app.get("/status")
def status():
    gpu = torch.cuda.is_available()

    result = {
        "loaded": model is not None,
        "cuda_available": gpu,
    }

    if gpu:
        result["gpu_name"] = torch.cuda.get_device_name(0)
        result["allocated_mb"] = round(torch.cuda.memory_allocated(0) / 1024 / 1024, 1)
        result["reserved_mb"] = round(torch.cuda.memory_reserved(0) / 1024 / 1024, 1)

    return result