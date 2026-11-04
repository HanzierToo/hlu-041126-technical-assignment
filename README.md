# Master Data API

A small authenticated Spring Boot service for fictional province, branch, and store master data. It includes branch management, a global store whitelist, and transactional change records. No existing company data or external master-data connection is required.

## Prerequisites
- Java 17 or newer. Development checked with JDK 24.0.1.
- The included Maven wrapper downloads Maven 3.9.12 on first use; alternatively, use a local Maven 3.6.3 or newer.
- MySQL 8.0.18 or newer, with permission to create and use a dedicated database. Development checked with MySQL 8.0.46.

Spring Boot 3.5.16 supports Java 17 through 25. The MySQL database and a MySQL account with access to it must already exist before starting. Flyway creates the application tables and fictional sample data.

## Setup
Create an empty database using your existing administrative connection:

~~~sql
CREATE DATABASE hlu_assignment CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
~~~

Create or select a MySQL account with access to this database. Then open src/main/resources/application.yml and fill in two separate sets of credentials:

~~~yaml
spring:
  datasource:
    url: 'jdbc:mysql://localhost:3306/hlu_assignment'
    username: '<database-user>'
    password: '<database-password>'

app:
  bootstrap:
    username: '<evaluation-user>'
    password: '<evaluation-password-at-least-12-characters>'
~~~

These settings are in different sections of the same existing file. Keep the other settings already present in that file.

### Database account and evaluation user

- **Database account:** The existing MySQL username and password under `spring.datasource` let the application connect to the database. They are not API login credentials.
- **Evaluation user:** Choose a username and password under `app.bootstrap` for anyone who needs to try the API, including yourself or the assignment reviewer. This is an application account, not a MySQL account or a predetermined person. Use these credentials with `/api/auth/login`.

When the application starts, it creates the evaluation user if that username does not already exist, storing a BCrypt password hash in the application database. The bootstrap password must be at least 12 characters and no more than 72 UTF-8 bytes. Changing the password in application.yml later does not change an existing user's password. After an application user exists, the bootstrap username and password can be cleared from the file.

From the repository root, start the service:

Windows PowerShell:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

macOS or Linux:

~~~sh
sh ./mvnw spring-boot:run
~~~

The server listens on port 8080 by default. The port, page limit, and branch deletion policy can also be changed in application.yml. The default page limit is 100, with an allowed maximum from 1 to 1000. The branch deletion policy defaults to REJECT_IF_STORES_EXIST; HIDE_STORES is the alternative. Restart after changing configuration.

The application.yml file is part of the repository. Before committing or sharing changes, remove your credentials from it; do not publish real passwords in the public repository.

## Try the API
The following walkthrough uses Postman. Other HTTP clients can make the same requests as long as they retain the session cookie and send the CSRF header. The examples use `http://localhost:8080`; change the port if you configured another one.

### Log in

1. Send `GET http://localhost:8080/api/auth/csrf`. In the JSON response, note the `headerName` and `token` values. Keep the session cookie; Postman's cookie jar normally handles it for you.
2. Send `POST http://localhost:8080/api/auth/login`:
   - Under **Body**, select **x-www-form-urlencoded** and add `username` and `password` with the evaluation-user credentials from application.yml.
   - Under **Headers**, add the CSRF header: use the returned `headerName` as the key and the returned `token` as the value.
   - Send the request with the session cookie from step 1. A successful login returns HTTP 200.
3. Send `GET http://localhost:8080/api/auth/csrf` again using the authenticated session. Login rotates the token, so use this new header name and token for subsequent POST, PUT, and DELETE requests.

### Try the user stories

Append each path below to `http://localhost:8080` and keep the authenticated session cookie on every request. For each POST, PUT, and DELETE request, add the latest CSRF header from step 3.

| Request | What to expect with the initial sample data |
| --- | --- |
| `GET /api/stores?province=Bal&page=0&size=25` | Returns the two active Bali stores and the whitelisted Jakarta store. |
| `GET /api/whitelist` | Returns the whitelisted Jakarta store, ID 3. |
| `POST /api/whitelist/1` | Adds Bali store 1 to the whitelist; returns 201. |
| `DELETE /api/whitelist/1` | Removes that entry; returns 204. |
| `PUT /api/branches/1` | Updates the branch with the JSON body below; returns 204. |
| `DELETE /api/branches/4` | Soft-deletes the empty sample branch; returns 204. |
| `POST /api/auth/logout` | Ends the session; returns 204. |

For the branch update, select **Body > raw > JSON** in Postman:

~~~json
{
  "name": "Bali Central Updated",
  "active": true
}
~~~

The POST, PUT, and DELETE examples change the sample database. Their expected results assume the initial sample data; repeating an operation can produce a different response.

The fictional seed includes Bali and Jakarta. Jakarta store 3 is whitelisted initially. Store search matches province names containing the supplied text, case-insensitively. If several provinces match, stores from all of them are included. If no active province matches, the API returns 404. Results contain only active, non-deleted stores with active, non-deleted branches and provinces, ordered by store ID. Whitelisted stores are included once in the overall paginated result, not repeated on every page.

PUT /api/branches/{id} updates name and active; province reassignment is outside the specified operation. DELETE is a soft delete. By default, deletion fails with HTTP 409 if a branch has any non-deleted stores. Under HIDE_STORES, deletion succeeds and the branch's stores become invisible without being deleted. A whitelist entry can be removed by ID even if its store later becomes hidden; listing only shows currently visible entries.

See the two PDFs in docs for the API contract, assumptions and ER diagram. The query-plan evidence is in docs/performance-results.txt. Automated tests are intentionally deferred to a separate pass.

## Security and operating scope
All authenticated accounts can perform all requested operations; no registration, roles, or user administration is implemented. Use HTTPS and appropriately secured session cookies when deploying beyond localhost. Keep MySQL restricted to trusted users. Local sample credentials should not be used for deployment.

## Performance evidence

On Windows 11 with MySQL 8.0.46, a Ryzen 7 7800X3D and 32 GB RAM, an isolated local MySQL instance was populated with fictional data. The equivalent SQL search and count for one matched province were measured three times using EXPLAIN ANALYZE with page size 25. These measurements do not include the separate province-name lookup or a multiple-province search. At 20,007 stores and 105 branches, median database times were 27.0 ms for the page and 15.6 ms for the count. At 40,007 stores and 105 branches, they were 61.9 ms and 39.1 ms respectively. These are local, mostly warm-cache observations, not end-to-end API timings or service guarantees. The page query performed a store table scan and sort in this dataset; the results show increasing work as the dataset grows. Large offsets can also be expensive. Full plans and client timings are in docs/performance-results.txt.

To reproduce the measurement, use MySQL Workbench or your preferred MySQL client to create a disposable database named `hlu_performance`, then run `src/main/resources/db/migration/V1__schema.sql` and `V2__sample_data.sql` against it, in that order. Configure a MySQL client login path for an account with access to that database. With Python 3 installed, run the script from the repository root, replacing the client path, host, port, username, and login path with your own values:

~~~text
python scripts/performance.py --mysql "<path-to-mysql-client>" --host 127.0.0.1 --port 3306 --user "<database-user>" --login-path "<login-path>" --confirm-disposable
~~~

Use `python3` if that is how Python 3 is invoked on your system. Run `python scripts/performance.py --help` to see the available options.

The script refuses a schema that does not contain exactly the original fictional seed counts. It inserts data and runs ANALYZE TABLE, so never point it at a database containing valuable data. Do not put passwords in command arguments.
