#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Adds Kejafy (a Spring Boot app) to the shared MVP server. Run HolidayHub's
# deploy/mvp/server-setup.sh on the server first: it installs nginx,
# PostgreSQL, Redis and certbot, which this script reuses.
#
#   sudo bash add-app.sh <app> <domain> <port> [--no-tls]
#   sudo bash add-app.sh kejafy kejafy.unclecanvas.com 8081
#
# Creates, for this app only:
#   Java 21 runtime                              (installed once, shared)
#   /var/www/<app>/{releases,current,shared}     releases + shared .env and logs
#   a PostgreSQL database and user               (password written to shared/.env)
#   a systemd service                            <app>.service, java -jar current/app.jar
#   an nginx site proxying to 127.0.0.1:<port>   and its HTTPS certificate
#   a sudoers rule so the deploy user can restart the service
#
# <port> must be free and different for every Java MVP on the server.
# Never overwrites an existing shared/.env. Safe to run again.
# Options via environment: DEPLOY_USER (ubuntu), REDIS_DB (5), JAVA_XMX (384m),
# ACME_EMAIL.
# ---------------------------------------------------------------------------
set -euo pipefail

APP="${1:-}"; DOMAIN="${2:-}"; PORT="${3:-}"; TLS=true
[ "${4:-}" = "--no-tls" ] && TLS=false
DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
REDIS_DB="${REDIS_DB:-5}"
JAVA_XMX="${JAVA_XMX:-384m}"
ACME_EMAIL="${ACME_EMAIL:-admin@${DOMAIN#*.}}"

log() { printf '\033[0;36m[%s]\033[0m %s\n' "$APP" "$*"; }
svc() { if [ -d /run/systemd/system ]; then systemctl "$1" "$2"; else service "$2" "$1"; fi; }

if [ "$(id -u)" -ne 0 ]; then echo "Run as root: sudo bash $0 <app> <domain> <port>" >&2; exit 1; fi
if ! [[ "$APP" =~ ^[a-z][a-z0-9-]{1,30}$ ]] || [ -z "$DOMAIN" ] || ! [[ "$PORT" =~ ^[0-9]{4,5}$ ]]; then
    echo "Usage: sudo bash $0 <app: lowercase letters, digits, -> <domain> <port> [--no-tls]" >&2; exit 1
fi

ROOT="/var/www/$APP"
DB="${APP//-/_}"
SCHEME=$([ "$TLS" = true ] && echo https || echo http)

log "Java 21…"
if ! java -version 2>&1 | grep -q '"21'; then
    # Try the cached package lists first: an unrelated third-party repo
    # (e.g. a PPA that changed its Label) can make `apt-get update` fail.
    apt-get install -y openjdk-21-jre-headless \
        || { apt-get update -y; apt-get install -y openjdk-21-jre-headless; }
fi

log "Folders…"
mkdir -p "$ROOT"/{releases,shared/logs}
chown -R "$DEPLOY_USER":"$DEPLOY_USER" "$ROOT"
chmod 750 "$ROOT"

log "Database…"
if ! sudo -u postgres psql -Atc "select 1 from pg_roles where rolname='$DB'" | grep -q 1; then
    DB_PASSWORD="$(openssl rand -hex 24)"
    sudo -u postgres psql -qc "create role \"$DB\" login password '$DB_PASSWORD'"
    sudo -u postgres psql -qc "create database \"$DB\" owner \"$DB\""
else
    DB_PASSWORD=""
fi

# Only this app's own user may connect to its database.
sudo -u postgres psql -qc "revoke connect on database \"$DB\" from public"
sudo -u postgres psql -qc "grant connect on database \"$DB\" to \"$DB\""

if [ ! -f "$ROOT/shared/.env" ]; then
    log "Writing shared/.env…"
    [ -z "$DB_PASSWORD" ] && { echo "Database user exists but shared/.env is missing; set POSTGRES_PASSWORD by hand." >&2; DB_PASSWORD=CHANGE_ME; }
    # systemd reads this file (EnvironmentFile), so: KEY=value, no "export".
    cat > "$ROOT/shared/.env" <<ENV
SERVER_PORT=${PORT}
SERVER_ADDRESS=127.0.0.1
# nginx terminates HTTPS: trust its X-Forwarded-* headers, so OAuth redirect
# URIs and generated links use https://${DOMAIN}.
SERVER_FORWARD_HEADERS_STRATEGY=framework
JAVA_OPTS=-Xms128m -Xmx${JAVA_XMX} -XX:+UseSerialGC -XX:MaxMetaspaceSize=192m

POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DB=${DB}
POSTGRES_USER=${DB}
POSTGRES_PASSWORD=${DB_PASSWORD}
SPRING_JPA_SHOW_SQL=false

# Redis is shared by every MVP: a database index of its own keeps Kejafy's
# keys apart (Laravel MVPs use 0 and 1).
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
SPRING_DATA_REDIS_DATABASE=${REDIS_DB}

SWAGGER_UI_ENABLED=false
APP_CORS_ALLOWED_ORIGINS=${SCHEME}://${DOMAIN}
APP_FRONTEND_BASE_URL=${SCHEME}://${DOMAIN}

# Google console → authorised redirect URI:
#   ${SCHEME}://${DOMAIN}/login/oauth2/code/google
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

MPESA_BASE_URL=https://sandbox.safaricom.co.ke
MPESA_CONSUMER_KEY=
MPESA_CONSUMER_SECRET=
MPESA_SHORTCODE=
MPESA_PASSKEY=
MPESA_CALLBACK_URL=${SCHEME}://${DOMAIN}/api/v1/payments/mpesa/callback

STRIPE_SECRET_KEY=
STRIPE_PUBLISHABLE_KEY=
STRIPE_WEBHOOK_SECRET=
ENV
    chown "$DEPLOY_USER":"$DEPLOY_USER" "$ROOT/shared/.env"
    chmod 600 "$ROOT/shared/.env"
fi

log "systemd service…"
cat > "/etc/systemd/system/${APP}.service" <<UNIT
[Unit]
Description=${APP} (Spring Boot)
After=network.target postgresql.service redis-server.service
ConditionPathExists=${ROOT}/current/app.jar

[Service]
User=${DEPLOY_USER}
WorkingDirectory=${ROOT}/current
EnvironmentFile=${ROOT}/shared/.env
ExecStart=/usr/bin/java \$JAVA_OPTS -jar ${ROOT}/current/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=30
StandardOutput=append:${ROOT}/shared/logs/app.log
StandardError=append:${ROOT}/shared/logs/app.log

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable "${APP}.service" >/dev/null

# Rotate the app log so it never fills the disk.
cat > "/etc/logrotate.d/${APP}" <<ROT
${ROOT}/shared/logs/*.log {
    daily
    rotate 14
    compress
    missingok
    notifempty
    copytruncate
}
ROT

log "Deploy user may restart ${APP}…"
cat > "/etc/sudoers.d/mvp-${APP}" <<SUDO
${DEPLOY_USER} ALL=(root) NOPASSWD: /usr/bin/systemctl restart ${APP}, /usr/bin/systemctl stop ${APP}, /usr/bin/systemctl start ${APP}, /usr/bin/systemctl status ${APP}
SUDO
chmod 440 "/etc/sudoers.d/mvp-${APP}"
visudo -cf "/etc/sudoers.d/mvp-${APP}" >/dev/null

log "nginx site…"
cat > "/etc/nginx/sites-available/${APP}" <<NGINX
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN};

    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location / {
        proxy_pass http://127.0.0.1:${PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host \$host;
        proxy_read_timeout 120s;
    }

    access_log /var/log/nginx/${APP}.access.log;
    error_log /var/log/nginx/${APP}.error.log warn;
}
NGINX
ln -sfn "/etc/nginx/sites-available/${APP}" "/etc/nginx/sites-enabled/${APP}"
nginx -t
svc reload nginx

if [ "$TLS" = true ]; then
    log "HTTPS certificate for ${DOMAIN}…"
    certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$ACME_EMAIL" --redirect
fi

log "Ready. Fill in the API keys in ${ROOT}/shared/.env, then deploy from GitHub Actions."
