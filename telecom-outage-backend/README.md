# Telecom Network Outage Blast-Radius Mapper – Backend

Generated Spring Boot starter for the PostgreSQL schema you already created (13 tables).
**Java 21 · Spring Boot 3.5.6 · Maven · PostgreSQL · Spring Security · JWT**

## Before running: configure these values
Open `src/main/resources/application.properties`:
- `spring.datasource.url`: replace `telecom_outage_db` with your actual PostgreSQL database name.
- `spring.datasource.username`: replace `postgres` if you use another PostgreSQL user.
- `spring.datasource.password`: replace `CHANGE_ME` with your PostgreSQL password.
- `app.jwt.secret`: replace with a random secret of at least 32 bytes. Keep it private.

You can instead set environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`. `.env.example` is a reference file; Spring Boot does not automatically load it.

Example URL: `jdbc:postgresql://localhost:5432/YOUR_DATABASE_NAME`

## Important schema safety
`spring.jpa.hibernate.ddl-auto=validate` means Hibernate checks entity mappings against your existing schema; it will not create, drop, or update tables. No schema/data SQL is included. The nullable `audit_logs.ip_address` column is intentionally not mapped in this starter entity; it can be added later with a PostgreSQL INET-specific Hibernate type.

## Run in IntelliJ / VS Code terminal
Requirements: JDK 21 and Maven 3.9+.
```bash
mvn clean package
mvn spring-boot:run
```
Health check: `http://localhost:8080/actuator/health`

## Existing sample administrator account
Your sample row `admin@telecom.com` has password_hash `TEMP_NOT_FOR_LOGIN`, which is not a BCrypt hash. Login will not work until you replace it with a BCrypt hash. Do not treat the placeholder as a password. Generate a BCrypt hash using a local PasswordEncoder utility or a temporary Spring Boot command-line runner, then update the row. Never store plaintext passwords.

## API endpoints
- `POST /api/auth/login` — public; JSON `{"email":"admin@telecom.com","password":"your-real-password"}`
- `GET /api/components`, `GET /api/components/{id}` — authenticated
- `POST /api/components` — ADMIN or NETWORK_ENGINEER
- `PATCH /api/components/{id}/status` — ADMIN or NETWORK_ENGINEER
- `GET /api/network/dependencies` — authenticated
- `GET /api/services`, `GET /api/alarms`, `GET /api/alarms/status/{status}` — authenticated
- `GET /api/incidents`, `GET /api/incidents/{id}` — authenticated
- `POST /api/incidents` — ADMIN, NOC_MANAGER, NOC_ANALYST
- `PATCH /api/incidents/{id}/status` — ADMIN, NOC_MANAGER
- `POST /api/incidents/{id}/blast-radius` — ADMIN, NOC_MANAGER, NOC_ANALYST

After login, send `Authorization: Bearer <token>`.

## Blast radius logic
BFS follows active directed dependencies from source/upstream to dependent/downstream. It persists impacted components and services into the two incident impact tables. This baseline does not yet implement Python-service integration, redundant-path analysis, historical calculation runs, unique-customer deduplication, or full restoration workflows.

## Security and production TODOs
- The included API currently has JWT login and BCrypt verification. User provisioning is intentionally not exposed publicly; create users through a controlled admin-only workflow before enabling production use.
- Add response DTOs and pagination; entity serialization is a starter implementation.
- Expand audit logging and validation of enum/check-constraint values.
- Change CORS allowed origins in `SecurityConfig.java` to the React app's actual origin.
- Use HTTPS, secret management, and least-privilege IAM on AWS. Do not commit credentials.
- Python engine, React frontend, Snowflake, Power BI, Docker, AWS VPC/IAM/EC2/S3 and GitHub Actions remain subsequent integration/deployment phases; they are not provisioned by this backend ZIP.


## Extended backend endpoints

### Restoration workflow
- `GET /api/restorations`
- `GET /api/restorations/{id}`
- `GET /api/restorations/incident/{incidentId}`
- `POST /api/restorations` (ADMIN, NOC_MANAGER, NETWORK_ENGINEER)
- `PATCH /api/restorations/{id}/status` (ADMIN, NOC_MANAGER, NETWORK_ENGINEER)

Example POST body:
```json
{
  "incidentId": 1,
  "componentId": 1,
  "actionDescription": "Replaced faulty router power module",
  "previousStatus": "DOWN",
  "newStatus": "UP",
  "restorationStatus": "COMPLETED"
}
```
Creating a restoration entry also updates the selected component's status in the same transaction.

### Administrative table CRUD
All endpoints below require the `ADMIN` role and a JWT bearer token:
- `GET /api/admin/data/{table}`
- `GET /api/admin/data/{table}/{id}`
- `POST /api/admin/data/{table}`
- `PUT /api/admin/data/{table}/{id}`
- `DELETE /api/admin/data/{table}/{id}`

Allowed tables: `roles`, `users`, `components`, `component_dependencies`, `services`,
`service_components`, `alarms`, `incidents`, `incident_affected_components`,
`incident_affected_services`, `restoration_history`, `audit_logs`.
Use database column names in JSON and relationship foreign-key columns such as `component_id`.
For user creation/update, send `password` (plain text over HTTPS); the API stores a BCrypt hash and never returns `password_hash`.
The composite-key table `incident_alarms` has dedicated endpoints:
- `GET /api/admin/incident-alarms`
- `POST /api/admin/incident-alarms` with `{"incidentId":1,"alarmId":1}`
- `DELETE /api/admin/incident-alarms/{incidentId}/{alarmId}`

The administrative generic CRUD endpoints are intentionally restricted to ADMIN. They operate against the existing schema and do not create or alter tables.
