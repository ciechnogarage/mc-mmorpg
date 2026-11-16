# syntax=docker/dockerfile:1
FROM python:3.11.11-slim-bookworm AS base

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    HF_HOME=/models \
    HUGGINGFACE_HUB_CACHE=/models \
    OMP_NUM_THREADS=4 \
    RERANKER_BACKEND=mock \
    PORT=8000

WORKDIR /app

COPY requirements-mock.txt .
RUN pip install --no-cache-dir -r requirements-mock.txt

COPY app ./app

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=10s --start-period=600s --retries=10 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/ready', timeout=5)"]

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

FROM base AS mock

FROM base AS real
ENV RERANKER_BACKEND=bge
COPY requirements.txt /tmp/requirements-real.txt
RUN pip install --no-cache-dir torch==2.5.1 \
        --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir -r /tmp/requirements-real.txt
