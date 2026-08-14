#!/usr/bin/env bash
# =====================================================================
# One-time server preparation for the school-backend service.
#
# Run ON THE SERVER, once:
#     sudo bash setup-server.sh
#
# Idempotent: safe to re-run. It installs Java 21 if missing, creates
# the directory layout, installs the systemd unit, and seeds the env
# file with a freshly generated JWT secret.
#
# It does NOT copy the jar — deploy.ps1 / deploy.sh does that.
# =====================================================================
set -euo pipefail

APP_DIR=/opt/school-backend
ENV_DIR=/etc/school-backend
ENV_FILE="$ENV_DIR/school-backend.env"
SERVICE_USER=ubuntu

# Uploads live under the service user's home, separate from the jar, so a
# redeploy never touches them. Must match UPLOAD_DIR / UPLOAD_*_SUBDIR in the
# env file — if they disagree, uploads silently land somewhere nginx is not
# serving from.
UPLOAD_DIR=/home/ubuntu/app/uploads
UPLOAD_SUBDIRS=(student-id-pictures documents materials)

if [[ $EUID -ne 0 ]]; then
  echo "Run with sudo: sudo bash setup-server.sh" >&2
  exit 1
fi

echo "==> Java"
# The build targets Java 21; a lower JRE fails at class-load with an
# UnsupportedClassVersionError that does not obviously name the cause.
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q '"21'; then
  apt-get update -qq
  apt-get install -y -qq openjdk-21-jre-headless
fi
java -version

echo "==> Directories"
mkdir -p "$APP_DIR/logs"
chown -R "$SERVICE_USER":"$SERVICE_USER" "$APP_DIR"

for sub in "${UPLOAD_SUBDIRS[@]}"; do
  mkdir -p "$UPLOAD_DIR/$sub"
done
# The service runs as ubuntu, so it must own these to write into them.
chown -R "$SERVICE_USER":"$SERVICE_USER" "$UPLOAD_DIR"
# 755, not 700: nginx (running as www-data) needs to traverse and read these
# to serve /uploads/ directly from disk.
chmod -R 755 "$UPLOAD_DIR"
echo "    uploads: $UPLOAD_DIR/{$(IFS=,; echo "${UPLOAD_SUBDIRS[*]}")}"

echo "==> Environment file"
mkdir -p "$ENV_DIR"
if [[ -f "$ENV_FILE" ]]; then
  echo "    $ENV_FILE already exists - leaving it alone."
else
  cp "$(dirname "$0")/school-backend.env.example" "$ENV_FILE"

  # Generate a real signing key now, so the service can never accidentally
  # start on the well-known development default from application.yml.
  JWT=$(openssl rand -base64 64 | tr -d '\n')
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT}|" "$ENV_FILE"

  echo "    Created $ENV_FILE with a generated JWT_SECRET."
  echo "    >>> Still to edit by hand: DB_PASSWORD, and the CORS/FRONTEND origin."
fi
chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"

echo "==> systemd unit"
install -m 644 "$(dirname "$0")/school-backend.service" /etc/systemd/system/school-backend.service
systemctl daemon-reload
systemctl enable school-backend >/dev/null

echo
echo "Server prepared."
echo
echo "Next:"
echo "  1. sudo nano $ENV_FILE        # set DB_PASSWORD and the origins"
echo "  2. upload the jar to $APP_DIR/school-backend.jar"
echo "  3. sudo systemctl start school-backend"
echo "  4. journalctl -u school-backend -f"
