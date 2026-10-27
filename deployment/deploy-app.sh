#!/usr/bin/env bash
# Run after every upload:  bash ~/upload/deployment/deploy-app.sh
# Installs the uploaded JAR and frontend files and restarts the services.
set -euo pipefail

UPLOAD="$HOME/upload"

if [ ! -f "$UPLOAD/hostel-mess.jar" ] || [ ! -d "$UPLOAD/frontend" ]; then
  echo "Nothing to deploy. Run deployment\\upload-to-aws.bat on your computer first."
  exit 1
fi
if [ ! -f /etc/hostel-mess/backend.env ]; then
  echo "Missing /etc/hostel-mess/backend.env. Run create-env.sh first."
  exit 1
fi

echo "== Stopping the backend =="
sudo systemctl stop hostel-backend || true

echo "== Installing the backend JAR =="
sudo cp "$UPLOAD/hostel-mess.jar" /opt/hostel-mess/hostel-mess.jar
sudo chown hostel:hostel /opt/hostel-mess/hostel-mess.jar

echo "== Installing the frontend files =="
sudo rm -rf /var/www/hostel-mess/*
sudo cp -r "$UPLOAD/frontend/." /var/www/hostel-mess/
# The deployed copy calls the API on the same origin through Nginx.
sudo tee /var/www/hostel-mess/js/config.js >/dev/null <<'EOF'
// API base URL in production: same origin, proxied by Nginx to the backend.
const API_BASE_URL = "/api";
EOF
sudo chown -R www-data:www-data /var/www/hostel-mess

echo "== Reloading Nginx =="
sudo nginx -t
sudo systemctl enable nginx >/dev/null 2>&1 || true
sudo systemctl restart nginx

echo "== Starting the backend =="
sudo systemctl start hostel-backend

echo "Waiting for the backend to start (up to 2 minutes) ..."
CODE="000"
for _ in $(seq 1 60); do
  CODE="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/ping || true)"
  if [ "$CODE" != "000" ]; then
    break
  fi
  sleep 2
done

if [ "$CODE" = "000" ]; then
  echo
  echo "The backend did not start. Last log lines:"
  sudo journalctl -u hostel-backend -n 40 --no-pager
  exit 1
fi

echo
echo "Backend answers directly:       HTTP $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/ping)  (401 is correct)"
echo "Backend answers through Nginx:  HTTP $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/api/ping)  (401 is correct)"
echo "Frontend page:                  HTTP $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/)  (200 is correct)"
echo
sudo systemctl --no-pager --lines=0 status hostel-backend | head -n 5
echo
echo "Done. Open http://YOUR_EC2_PUBLIC_IP in a browser."
