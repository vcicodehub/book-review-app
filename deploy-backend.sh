#!/usr/bin/env bash
set -euo pipefail

cd /opt/book-review

echo "Logging into GHCR..."
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin

echo "Pulling latest backend image..."
docker pull ghcr.io/vcicodehub/book-review-backend:latest

echo "Refreshing shared compose stack..."
cd /opt/noor-luca
docker compose pull book-review-backend
docker compose up -d book-review-backend

echo "Backend deployment complete."