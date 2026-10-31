#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "${BASH_SOURCE[0]}")/.."
if [[ $# -ne 2 || ! "$1" =~ ^sha-[0-9a-f]{40}$ || ! "$2" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
    echo "Usage: bash scripts/deploy.sh sha-<40-character-commit> wallet.example.com" >&2
    exit 2
fi
[[ -f .env.prod ]] || { echo 'Missing environment/.env.prod' >&2; exit 2; }
export IMAGE_TAG="$1" DOMAIN="$2"
exec 9>.deploy.lock
flock -n 9 || { echo 'Another deployment is running.' >&2; exit 1; }
compose=(docker compose --env-file .env.prod -f docker-compose.prod.yml)
"${compose[@]}" config --quiet
"${compose[@]}" pull
# Start dependencies and app first; recreate Nginx afterwards to refresh Docker DNS.
"${compose[@]}" up -d --wait --wait-timeout 300 postgres redis kafka minio minio-init backend frontend
"${compose[@]}" exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/lock-bootstrap-admin.sql
"${compose[@]}" up -d --no-deps --force-recreate --wait --wait-timeout 60 nginx
curl --fail --silent --show-error --retry 5 --retry-delay 3 "https://${DOMAIN}/health"
curl --fail --silent --show-error --output /dev/null "https://${DOMAIN}/vi"
if [[ -f .deployed-image-tag ]]; then cp .deployed-image-tag .previous-image-tag; fi
printf '%s\n' "$IMAGE_TAG" > .deployed-image-tag
printf 'Deployed %s at https://%s\n' "$IMAGE_TAG" "$DOMAIN"
