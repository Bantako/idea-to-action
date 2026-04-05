#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR/android-app"

log() {
  printf '\n==> %s\n' "$1"
}

if [ ! -d "$APP_DIR" ]; then
  echo "android-app directory not found: $APP_DIR" >&2
  exit 1
fi

log "doctor"
cd "$ROOT_DIR"
./scripts/doctor.sh

log "gradle stop"
cd "$APP_DIR"
./gradlew --stop || true

log "assemble debug"
./gradlew assembleDebug
