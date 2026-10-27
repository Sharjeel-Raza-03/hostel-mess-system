#!/usr/bin/env bash
# Run ONCE:  bash ~/upload/deployment/init-database.sh
# Creates the tables (schema.sql) and the starting data (seed.sql) on RDS.
set -euo pipefail

ENV_FILE=/etc/hostel-mess/backend.env
get() { sudo grep "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2-; }

DB_URL="$(get DB_URL)"
DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^:/]+).*|\1|')"
export PGPASSWORD="$(get DB_PASSWORD)"
PSQL=(psql "host=$DB_HOST port=5432 dbname=hostel_mess user=$(get DB_USERNAME) sslmode=require" -v ON_ERROR_STOP=1)

echo "Connecting to $DB_HOST ..."
EXISTS="$("${PSQL[@]}" -tAc "SELECT to_regclass('public.users') IS NOT NULL")"
if [ "$EXISTS" = "t" ]; then
  echo "The tables already exist, so nothing was changed."
  exit 0
fi

echo "Creating the tables ..."
"${PSQL[@]}" -f "$HOME/upload/database/schema.sql"
echo "Loading the starting data ..."
"${PSQL[@]}" -f "$HOME/upload/database/seed.sql"

echo
echo "== Check =="
"${PSQL[@]}" -c "SELECT count(*) AS tables FROM information_schema.tables WHERE table_schema = 'public';"
"${PSQL[@]}" -c "SELECT id, email, role FROM users;"
"${PSQL[@]}" -c "SELECT count(*) AS medicines FROM medicines;"

echo
echo "Next: bash ~/upload/deployment/deploy-app.sh"
