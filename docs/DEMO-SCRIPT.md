# Final demonstration script (about 10 minutes)

Follow the order of the teacher's checklist. Keep two devices ready: a laptop and a phone.

## Before the demo (10 minutes earlier)
1. EC2 instance is **Running** and RDS shows **Available** (AWS Console, Sydney region).
2. Open `http://PUBLIC_IP` on the laptop. The login page loads.
3. Laptop: logged in as the **admin**. Phone: logged in as a **student** (register one if needed, for example `demo.student@test.com`).
4. Postman is open with **Hostel Mess - Cloud** selected and `baseUrl` set to `http://PUBLIC_IP`.
5. Browser tabs ready: the AWS EC2 console, the AWS RDS console, the JMeter report (`docs/loadtest/results/users-30/report/index.html`).
6. Pick two dates next week for the demo leave and note them.

## Steps

| # | Checklist item | What to do | What to say |
|---|---|---|---|
| 1 | Login and role-based access | Log in as student (phone) and admin (laptop). Show the different menus. In Postman, run **1 Auth and role-based access → Student calls admin API (403)**. | Two roles, JWT login, the server checks the role on every request. |
| 2 | Student operations | Phone: add meals for tomorrow, submit a 2-day leave request. | Students manage only their own records. |
| 3 | Medicine availability | Phone: open Medicines, search, filter, tick Available only. Laptop (admin): open Manage stock and change a quantity to 0. Phone: refresh the list, it shows Out of stock. | Same database, so a change on one device shows on the other after a refresh. |
| 4 | Admin approval and rejection | Laptop: approve the leave, reject another. | Approval runs in a single database transaction. |
| 5 | Automatic bill calculation | Laptop: Bills, generate bills for the month and note the student's total. Approve the leave, generate again. Phone: My bills shows the lower total and the leave days. | Approved leave removes those meals from the mess count and the bill. |
| 6 | REST API and Postman | Postman: run the collection against the cloud environment. Show the green results. | Every feature is a REST endpoint, tested with Postman. |
| 7 | Live AWS deployment | Browser: show the EC2 instance (Running) and the RDS instance (Available). | Frontend, backend and database run on AWS, not on localhost. |
| 8 | Centralized cloud database | On the server (ssh), run `bash ~/upload/deployment/check-loadtest.sh` or a psql query on the `users` and `medicines` tables. | All data is in one PostgreSQL database on RDS. |
| 9 | Two or more devices and concurrency | Show both devices working at the same time. Open the JMeter report: users, error percentage, response times. | Spring Boot uses a thread pool, so concurrent requests are handled without manual threading. |

## If something goes wrong during the demo
- Page does not load: check the instance is Running and you used `http://`, not `https://`.
- Login fails: confirm the correct admin email, and refresh with Ctrl+F5.
- Backend down: ssh in and run `sudo systemctl restart hostel-backend`, wait 30 seconds.
