#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "${BASH_SOURCE[0]}")/.."
[[ -f .env.prod ]] || { echo 'Missing .env.prod' >&2; exit 2; }
mkdir -p backups
backup="backups/postgres-$(date -u +%Y%m%dT%H%M%SZ).dump"
if ! docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres \
    sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup"; then
    echo "Backup failed; incomplete file: $backup" >&2
    exit 1
fi
[[ -s "$backup" ]] || { echo 'Backup is empty' >&2; exit 1; }
printf 'Database backup: %s\n' "$backup"
