#!/usr/bin/env bash
# Run ONCE (or again to change the database password):  bash ~/upload/deployment/create-env.sh
# Creates /etc/hostel-mess/backend.env with the database settings and a new random JWT secret.
set -euo pipefail

ENV_FILE=/etc/hostel-mess/backend.env

read -rp "RDS endpoint (for example hostel-mess-db.abc123.ap-south-1.rds.amazonaws.com): " RDS
RDS="$(echo "$RDS" | tr -d '[:space:]')"
if [ -z "$RDS" ]; then
  echo "The endpoint cannot be empty."
  exit 1
fi

read -rsp "Database master password: " DBPW
echo
if [ -z "$DBPW" ]; then
  echo "The password cannot be empty."
  exit 1
fi

JWT="$(openssl rand -hex 32)"

sudo mkdir -p /etc/hostel-mess
sudo tee "$ENV_FILE" >/dev/null <<EOF
DB_URL=jdbc:postgresql://${RDS}:5432/hostel_mess?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=${DBPW}
JWT_SECRET=${JWT}
JWT_EXPIRATION_MS=86400000
PORT=8080
EOF
sudo chown root:root "$ENV_FILE"
sudo chmod 600 "$ENV_FILE"

echo
echo "Saved $ENV_FILE (readable by root only)."
echo "Next: bash ~/upload/deployment/init-database.sh"
