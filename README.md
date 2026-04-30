# Consulting Booking System

ConsultBridge is a consultancy booking management system built with `Maven + Spring Boot + MySQL`. The project includes both backend APIs and static frontend pages for customers, specialists, and administrators.

The system supports:

- Customer and specialist registration
- Sign-in, sign-out, password change, and password reset
- Profile maintenance
- Expertise category management
- Specialist profile management
- Specialist time slot management
- Specialist search with filtering
- Booking creation, confirmation, rejection, cancellation, rescheduling, and completion
- Conflict prevention and status tracking
- Admin reporting and record maintenance

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Jakarta Validation
- MySQL
- Maven
- Static HTML, CSS, and JavaScript frontend

## Project Structure

- Backend source: `src/main/java`
- Frontend pages: `src/main/resources/static`
- Runtime config: `src/main/resources/application.yml`
- Test config: `src/test/resources/application-test.yml`
- UI design guide: `UI_DESIGN_GUIDE.md`
- Docker files: `Dockerfile`, `docker-compose.yml`

## Frontend Pages

Current page entry points:

- Project home: `http://localhost:8080/`
- Sign-in page: `http://localhost:8080/login.html`
- Registration hub: `http://localhost:8080/register.html`
- Customer registration: `http://localhost:8080/register-customer.html`
- Specialist registration: `http://localhost:8080/register-specialist.html`
- Workspace: `http://localhost:8080/dashboard.html`
- Admin portal: `http://localhost:8080/admin.html`
- Change password: `http://localhost:8080/change-password.html`
- Forgot password: `http://localhost:8080/forgot-password.html`
- Reset password: `http://localhost:8080/reset-password.html`
- Specialist detail page: `http://localhost:8080/specialist-detail.html?id=<specialistId>`

Legacy compatibility pages still exist for customer and specialist registration:

- `customer-register.html`
- `specialist-register.html`

## Seeded Startup Data

The application seeds this administrator account on startup when the active profile is not `test`:

- Administrator username: `AdminGroup28`
- Administrator password: `Group28_CPT204`

The application also ensures these initial categories exist:

- `Career Coaching`
- `Financial Advisory`

## Local MySQL Run

The application expects a running MySQL server before `spring-boot:run` can start successfully.

Default values in `application.yml`:

- Host: `localhost`
- Port: `3306`
- Database: `consulting_booking`
- Username: `root`
- Password: `root`

Runtime JDBC template:

```text
jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
```

Recommended local run command:

```bash
cd /Users/linziwei/Desktop/TESTProgram
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD='your_password' mvn -Dmaven.repo.local=.m2/repository spring-boot:run
```

If your MySQL root account has no password, use:

```bash
cd /Users/linziwei/Desktop/TESTProgram
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD='' mvn -Dmaven.repo.local=.m2/repository spring-boot:run
```

If your MySQL user is not `root`, replace `MYSQL_USERNAME` and `MYSQL_PASSWORD` with your own credentials.

After startup, open:

- `http://localhost:8080/`
- `http://localhost:8080/login.html`
- `http://localhost:8080/admin.html`

## Docker Run

The project includes a full Docker setup for both the application and MySQL.

Important:

- Docker Desktop or another Docker daemon must be running before these commands work.
- If local MySQL or XAMPP is already using port `3306`, stop it first or change the exposed MySQL port in `docker-compose.yml`.

Start the full stack:

```bash
cd /Users/linziwei/Desktop/TESTProgram
docker compose up --build -d
```

View logs:

```bash
docker compose logs -f app
docker compose logs -f mysql
```

Stop the stack:

```bash
docker compose down
```

Stop the stack and remove the MySQL volume:

```bash
docker compose down -v
```

Docker Compose services:

- App container: `consultbridge-app`
- MySQL container: `consultbridge-mysql`

Docker Compose database credentials:

- Root user: `root`
- Root password: `root`
- App database: `consulting_booking`
- App user: `booking_user`
- App password: `booking_pass`

Inside Docker, the app connects to:

- Host: `mysql`
- Port: `3306`

From your host machine, the app is exposed on:

- `http://localhost:8080`

## Environment Variables

The runtime can be customized with:

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

The test runtime can also use:

- `MYSQL_TEST_DATABASE`

## Key APIs

Base path:

- `http://localhost:8080/api`

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/register/customer`
- `POST /api/auth/register/specialist`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/change-password`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/password-reset/confirm`

### Users

- `GET /api/users/me`
- `GET /api/users`
- `PUT /api/users/me`
- `PUT /api/users/{id}`
- `POST /api/users`

### Categories

- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`

### Specialists

- `GET /api/specialists`
- `GET /api/specialists/{id}`
- `GET /api/specialists/manage`
- `GET /api/specialists/me`
- `POST /api/specialists`
- `PUT /api/specialists/{id}`

Search filters supported by `GET /api/specialists`:

- `categoryId`
- `level`
- `keyword`
- `minFee`
- `maxFee`
- `availableAt`

### Time Slots

- `GET /api/slots/specialists/{specialistId}`
- `POST /api/slots/specialists/{specialistId}`

Optional query parameters for `GET /api/slots/specialists/{specialistId}`:

- `status`
- `fromDate`
- `days`

### Bookings

- `POST /api/bookings`
- `POST /api/bookings/{id}/confirm`
- `POST /api/bookings/{id}/reject`
- `POST /api/bookings/{id}/cancel`
- `POST /api/bookings/{id}/reschedule`
- `POST /api/bookings/{id}/complete`
- `GET /api/bookings/me`
- `GET /api/bookings/schedule`

### Reporting

- `GET /api/reports/summary`

## Authentication Header

After sign-in, the API returns a token. Protected endpoints require:

```text
X-Auth-Token: <token>
```

## VS Code

The project includes `.vscode/launch.json`, so the main application can also be started directly from VS Code.

## UI Design Guide

Use `UI_DESIGN_GUIDE.md` before changing or extending frontend pages. It documents the project visual system, component rules, layout patterns, responsive behavior, and copywriting standards.

## Testing

Run tests with:

```bash
cd /Users/linziwei/Desktop/TESTProgram
mvn -Dmaven.repo.local=.m2/repository test
```

Tests use MySQL too. The default test profile in `src/test/resources/application-test.yml` uses:

- Database: `consulting_booking_test`
- Host: `localhost`
- Port: `3306`
- Username: `root`
- Password: `root`

If your local MySQL credentials differ, run:

```bash
cd /Users/linziwei/Desktop/TESTProgram
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD='your_password' mvn -Dmaven.repo.local=.m2/repository test
```

## MySQL Workbench

You can inspect the project database with MySQL Workbench.

Recommended local connection:

- Hostname: `127.0.0.1`
- Port: `3306`
- Username: your actual MySQL username
- Password: your actual MySQL password

If you are running through Docker and exposing the default port, you can use:

- Hostname: `127.0.0.1`
- Port: `3306`
- Username: `root` or `booking_user`
- Password: `root` or `booking_pass`

## Troubleshooting

### `Access denied for user 'root'@'localhost'`

Your MySQL username or password does not match the values used by the application. Start the app with explicit environment variables:

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD='your_password' mvn -Dmaven.repo.local=.m2/repository spring-boot:run
```

### `Communications link failure`

MySQL is not running, or the app is pointing to the wrong host or port.

### `Port 8080 already in use`

Another process is already using the application port. Stop that process or change the server port.

### `Cannot connect to the Docker daemon`

Docker Desktop is not running yet. Start Docker first, then rerun `docker compose up --build`.
