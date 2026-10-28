# Screenshots for the report

Save the files in this folder with the names below. Hide passwords, the RDS endpoint and the JWT secret before saving.

| File name | What to capture |
|---|---|
| 01-github-repo.png | GitHub repository page with the folders and commits |
| 02-login-page.png | Live login page with the address bar visible |
| 03-student-dashboard.png | Student dashboard (meals, leave, bills) |
| 04-admin-dashboard.png | Admin dashboard |
| 05-postman-403.png | Student calling an admin endpoint, status 403 |
| 06-postman-401.png | Request without a token, status 401 |
| 07-leave-pending.png | Student leave request showing Pending |
| 08-leave-approved-rejected.png | Admin approving one leave and rejecting another |
| 09-bill-before.png | Student bill before the approval |
| 10-bill-after.png | The same bill after the approval (lower total, leave days) |
| 11-mess-count-leave-day.png | Mess count for a leave day showing 0 |
| 12-medicines-search.png | Medicines page with badges and filters |
| 13-medicine-stock-change.png | Admin stock change, then the student's list after a refresh |
| 14-monthly-report.png | Admin monthly report |
| 15-postman-local-run.png | Collection Runner on Local, all tests passed |
| 16-postman-cloud-run.png | Collection Runner on Cloud, all tests passed |
| 17-unit-tests.png | `mvnw.cmd clean test` summary (Tests run: 38, Failures: 0) |
| 18-ec2-running.png | EC2 console: instance Running, public address, region |
| 19-ec2-security-group.png | EC2 inbound rules (22 from my IP, 80, 443) |
| 20-rds-available.png | RDS console: Available, Publicly accessible No, PostgreSQL |
| 21-rds-security-group.png | RDS inbound rule: port 5432 from the EC2 security group |
| 22-systemd-status.png | `sudo systemctl status hostel-backend` showing active (running) |
| 23-database-rows.png | psql query on the RDS database showing rows in users and medicines |
| 24-two-devices.png | Laptop and phone using the live site at the same time |
| 25-jmeter-30-users.png | JMeter report, 30 users, 0% errors |
| 26-jmeter-50-users.png | JMeter report, 50 users, 0% errors |
| 27-loadtest-check.png | Output of check-loadtest.sh (matching counts, no duplicate bills) |
