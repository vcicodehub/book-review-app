#!/usr/bin/env bash
set -euo pipefail

cd /opt/book-review

echo "Logging into GHCR..."
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin

echo "Pulling latest frontend image..."
docker pull ghcr.io/vcicodehub/book-review-frontend:latest

echo "Refreshing shared compose stack..."
cd /opt/noor-luca
docker compose pull book-review-frontend
docker compose up -d book-review-frontend

echo "Frontend deployment complete."