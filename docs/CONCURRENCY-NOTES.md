# Concurrency notes (for the report)

## How concurrent requests are handled
- The backend is a Spring Boot application with an embedded Tomcat server. Tomcat keeps a pool of worker threads (200 by default) and gives every incoming HTTP request to a free thread, so many clients are served at the same time. No manual threading is needed.
- Authentication is stateless (JWT), so any thread can serve any request without shared session state.
- Database access goes through a connection pool (HikariCP, 10 connections by default), so requests share a limited number of PostgreSQL connections.

## How data stays correct when requests overlap
| Risk | Protection |
|---|---|
| Two users register the same email | Unique constraint on `users.email`; the second request gets 409. |
| A student saves the same day twice | Unique constraint on `(user_id, meal_date)`; the second save updates the record. |
| The same student and month are billed twice | Unique constraint on `(user_id, bill_month, bill_year)`; regenerating updates the existing bill. |
| Leave approval changes several tables | The whole approval (status, meals, daily counts, bills) runs in one `@Transactional` method; if any step fails, nothing is saved. |
| Many students save meals for the same date at once | The daily count row for that date is created with `INSERT ... ON CONFLICT DO NOTHING` and locked with `SELECT ... FOR UPDATE`, so recounts run one after another and the count always matches the saved meals. |

## Evidence
1. Load test: 30 and 50 simultaneous users log in, save a meal for the same date and search medicines against the live URL (JMeter report, error rate 0%).
2. After the test, `check-loadtest.sh` shows that the daily counts equal the number of saved meals and that no bill is duplicated.
3. Two devices (laptop and phone) used the system at the same time: a stock change made by the admin appeared for the student after a refresh.
