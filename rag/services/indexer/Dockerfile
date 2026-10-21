# syntax=docker/dockerfile:1
FROM python:3.11.11-slim-bookworm

ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 PIP_NO_CACHE_DIR=1 PORT=8000
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends git \
 && rm -rf /var/lib/apt/lists/*
COPY indexer/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY ragcore/ragcore ./ragcore
COPY indexer/app ./indexer/app
COPY indexer/__init__.py ./indexer/__init__.py
EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/ready', timeout=5)"]
CMD ["sh", "-c", "uvicorn indexer.app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
