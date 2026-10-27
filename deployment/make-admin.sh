#!/usr/bin/env bash
# Run ONCE, after you registered your own account on the live website:
#   bash ~/upload/deployment/make-admin.sh
# Turns that account into the admin and removes the development admin (admin@hostel.local).
set -euo pipefail

ENV_FILE=/etc/hostel-mess/backend.env
get() { sudo grep "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2-; }

DB_URL="$(get DB_URL)"
DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^:/]+).*|\1|')"
export PGPASSWORD="$(get DB_PASSWORD)"
PSQL=(psql "host=$DB_HOST port=5432 dbname=hostel_mess user=$(get DB_USERNAME) sslmode=require" -v ON_ERROR_STOP=1)

read -rp "Email of the account you registered on the website: " EMAIL
EMAIL="$(echo "$EMAIL" | tr -d '[:space:]')"
if [ -z "$EMAIL" ]; then
  echo "The email cannot be empty."
  exit 1
fi

"${PSQL[@]}" -v email="$EMAIL" <<'SQL'
BEGIN;

UPDATE users SET role = 'ADMIN', room_no = NULL
 WHERE email = lower(:'email') AND role = 'STUDENT';

UPDATE medicines SET updated_by = n.id
  FROM users n
 WHERE n.email = lower(:'email') AND n.role = 'ADMIN'
   AND medicines.updated_by = (SELECT id FROM users WHERE email = 'admin@hostel.local');

UPDATE leave_requests SET reviewed_by = n.id
  FROM users n
 WHERE n.email = lower(:'email') AND n.role = 'ADMIN'
   AND leave_requests.reviewed_by = (SELECT id FROM users WHERE email = 'admin@hostel.local');

DELETE FROM users
 WHERE email = 'admin@hostel.local'
   AND EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN' AND email <> 'admin@hostel.local');

COMMIT;

SELECT id, full_name, email, role FROM users WHERE role = 'ADMIN';
SQL

echo
echo "If the list above shows your email with role ADMIN, log in on the website with it."
echo "If the list still shows admin@hostel.local, the email was not found: check the spelling."
