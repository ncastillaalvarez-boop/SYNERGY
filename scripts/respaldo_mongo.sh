#!/usr/bin/env bash
# Respaldo de la base "synergy" (Linux/macOS). Cada noche con cron:
#   30 1 * * * /ruta/al/proyecto/scripts/respaldo_mongo.sh
set -euo pipefail
URI="${MONGODB_URI:-mongodb://localhost:27017}"
BASE="${MONGODB_BASE:-synergy}"
CONSERVAR="${CONSERVAR:-7}"
CARPETA="$(cd "$(dirname "$0")/.." && pwd)/respaldos"
mkdir -p "$CARPETA"
DESTINO="$CARPETA/mongo_$(date +%Y%m%d_%H%M)"
mongodump --uri "$URI" --db "$BASE" --gzip --out "$DESTINO"
echo "Respaldo guardado en $DESTINO"
ls -1d "$CARPETA"/mongo_* | sort -r | tail -n +$((CONSERVAR + 1)) | xargs -r rm -rf
