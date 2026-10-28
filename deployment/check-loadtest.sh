#!/usr/bin/env bash
# Run after a load test:  bash ~/upload/deployment/check-loadtest.sh
# Shows that the daily counts match the saved meals and that no bill was duplicated.
set -euo pipefail

ENV_FILE=/etc/hostel-mess/backend.env
get() { sudo grep "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2-; }

DB_URL="$(get DB_URL)"
DB_HOST="$(echo "$DB_URL" | sed -E 's|^jdbc:postgresql://([^:/]+).*|\1|')"
export PGPASSWORD="$(get DB_PASSWORD)"
PSQL=(psql "host=$DB_HOST port=5432 dbname=hostel_mess user=$(get DB_USERNAME) sslmode=require" -v ON_ERROR_STOP=1)

echo "== Users created by the load test =="
"${PSQL[@]}" -c "SELECT count(*) AS load_test_users FROM users WHERE email LIKE 'loaduser%@test.com';"

echo "== Meals saved by those users, compared with the daily count table =="
echo "(each pair of columns must show the same number)"
"${PSQL[@]}" <<'SQL'
SELECT m.meal_date,
       count(*) AS meal_rows,
       count(*) FILTER (WHERE m.breakfast AND NOT m.on_leave) AS breakfasts_in_meals,
       max(d.breakfast_count) AS breakfasts_in_count_table,
       count(*) FILTER (WHERE m.lunch AND NOT m.on_leave) AS lunches_in_meals,
       max(d.lunch_count) AS lunches_in_count_table
  FROM meal_records m
  JOIN users u ON u.id = m.user_id AND u.email LIKE 'loaduser%@test.com'
  LEFT JOIN daily_mess_count d ON d.count_date = m.meal_date
 GROUP BY m.meal_date
 ORDER BY m.meal_date;
SQL

echo "== Duplicate bills (must return no rows) =="
"${PSQL[@]}" -c "SELECT user_id, bill_month, bill_year, count(*) FROM bills GROUP BY user_id, bill_month, bill_year HAVING count(*) > 1;"
