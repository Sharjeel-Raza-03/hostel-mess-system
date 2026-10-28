#!/usr/bin/env bash
# Run after you saved your screenshots:  bash ~/upload/deployment/cleanup-loadtest.sh
# Deletes the load-test users (loaduser...@test.com) and everything they created,
# then recounts the daily mess counts.
set -euo pipefail

ENV_FILE=/etc/hostel-mess/backend.env
get() { sudo grep "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2-; }

DB_URL="$(get DB_URL)"
DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^:/]+).*|\1|')"
export PGPASSWORD="$(get DB_PASSWORD)"
PSQL=(psql "host=$DB_HOST port=5432 dbname=hostel_mess user=$(get DB_USERNAME) sslmode=require" -v ON_ERROR_STOP=1)

read -rp "This deletes every user whose email starts with 'loaduser'. Type YES to continue: " ANSWER
if [ "$ANSWER" != "YES" ]; then
  echo "Cancelled. Nothing was changed."
  exit 0
fi

"${PSQL[@]}" <<'SQL'
BEGIN;

DELETE FROM bills          WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'loaduser%@test.com');
DELETE FROM leave_requests WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'loaduser%@test.com');
DELETE FROM meal_records   WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'loaduser%@test.com');
DELETE FROM users          WHERE email LIKE 'loaduser%@test.com';

UPDATE daily_mess_count d SET
  breakfast_count = (SELECT count(*) FROM meal_records m WHERE m.meal_date = d.count_date AND m.breakfast AND NOT m.on_leave),
  lunch_count     = (SELECT count(*) FROM meal_records m WHERE m.meal_date = d.count_date AND m.lunch     AND NOT m.on_leave),
  dinner_count    = (SELECT count(*) FROM meal_records m WHERE m.meal_date = d.count_date AND m.dinner    AND NOT m.on_leave);

COMMIT;

SELECT count(*) AS remaining_load_test_users FROM users WHERE email LIKE 'loaduser%@test.com';
SQL

echo "Done."
