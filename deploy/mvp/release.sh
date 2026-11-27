#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Activates an uploaded release on the shared MVP server. Run by the deploy
# workflow, as the deploy user, from inside the release it activates:
#
#   bash /var/www/<app>/releases/<id>/deploy/mvp/release.sh <app> <id>
#
#   1. switches /var/www/<app>/current to the release in one step
#   2. restarts the <app> service (Hibernate updates the schema on startup)
#   3. waits for /actuator/health to report UP, switching back to the
#      previous release if it does not
#   4. keeps the last 5 releases
# ---------------------------------------------------------------------------
set -euo pipefail

APP="$1"; ID="$2"
ROOT="/var/www/$APP"
REL="$ROOT/releases/$ID"
log() { printf '\033[0;36m[release %s]\033[0m %s\n' "$ID" "$*"; }

[ -f "$REL/app.jar" ] || { echo "No app.jar in $REL" >&2; exit 1; }
[ -f "$ROOT/shared/.env" ] || { echo "Missing $ROOT/shared/.env (run add-app.sh)" >&2; exit 1; }

PORT="$(grep -E '^SERVER_PORT=' "$ROOT/shared/.env" | cut -d= -f2- | tr -d '"')"
[ -n "$PORT" ] || { echo "SERVER_PORT is not set in $ROOT/shared/.env" >&2; exit 1; }

PREVIOUS="$(readlink -f "$ROOT/current" 2>/dev/null || true)"

switch_to() {
    ln -sfn "$1" "$ROOT/current.next"
    mv -Tf "$ROOT/current.next" "$ROOT/current"
    sudo -n systemctl restart "$APP"
}

healthy() {
    # Spring takes a while to boot on a small server: allow up to ~2 minutes.
    for _ in $(seq 1 40); do
        if curl -fsS --max-time 5 "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null | grep -q '"UP"'; then
            return 0
        fi
        sleep 3
    done
    return 1
}

log "Switching current → $ID and restarting $APP…"
switch_to "$REL"

log "Waiting for /actuator/health…"
if ! healthy; then
    echo "--- last lines of $ROOT/shared/logs/app.log ---" >&2
    tail -n 60 "$ROOT/shared/logs/app.log" >&2 || true
    if [ -n "$PREVIOUS" ] && [ "$PREVIOUS" != "$REL" ] && [ -f "$PREVIOUS/app.jar" ]; then
        echo "The new release did not become healthy; switching back to $(basename "$PREVIOUS")." >&2
        switch_to "$PREVIOUS"
    else
        echo "The release did not become healthy and there is no previous release to go back to." >&2
    fi
    exit 1
fi

log "Live. Keeping the last 5 releases…"
cd "$ROOT/releases"
ls -1dt -- */ 2>/dev/null | tail -n +6 | xargs -r rm -rf --
log "Done."
