#!/usr/bin/env bash
# Run ONCE on a new EC2 server (Ubuntu 24.04):  bash ~/upload/deployment/setup-server.sh
set -euo pipefail

UPLOAD="$HOME/upload"

echo "== 1/4 Installing Java 17, Nginx and the PostgreSQL client =="
sudo apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jre-headless nginx postgresql-client

echo "== 2/4 Creating the service user and folders =="
if ! id -u hostel >/dev/null 2>&1; then
  sudo useradd --system --home /opt/hostel-mess --shell /usr/sbin/nologin hostel
fi
sudo mkdir -p /opt/hostel-mess /var/www/hostel-mess /etc/hostel-mess
sudo chown hostel:hostel /opt/hostel-mess

echo "== 3/4 Installing the Nginx configuration =="
sudo cp "$UPLOAD/deployment/nginx.conf" /etc/nginx/conf.d/hostel-mess.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t

echo "== 4/4 Installing the systemd service =="
sudo cp "$UPLOAD/deployment/hostel-backend.service" /etc/systemd/system/hostel-backend.service
sudo systemctl daemon-reload
sudo systemctl enable hostel-backend

echo
echo "Server setup finished."
echo "Next: bash ~/upload/deployment/create-env.sh"
