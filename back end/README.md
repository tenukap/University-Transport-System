# TransMove Java Backend

Spring Boot REST API for the TransMove dashboard and profile interface.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- SQL Server with `CampusTransportDB` created by `SQLQuery6.sql`

## Run

From this folder:

```powershell
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=CampusTransportDB;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "your-sql-server-user"
$env:DB_PASSWORD = "your-password"
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## Interface endpoints

 `POST /api/auth/login` authenticates an active `Admin` or `FinanceOfficer` and returns a JWT
- `GET /api/users/{id}/bookings` loads booking history
- `DELETE /api/users/{id}` deletes the user and related SQL records in one transaction
- `GET /api/dashboard` loads dashboard metrics
Use a strong random `JWT_SECRET` environment variable in production. The sample `SQLQuery6.sql` password hashes are placeholders and must be replaced with real BCrypt hashes before login can succeed.

The configured admin allowlist includes `doanee315@gmail.com`. That email must still exist as an active row in `Users` with a valid BCrypt `PasswordHash`; the allowlist grants admin authority after successful password verification.
- `GET /api/bookings` loads all bookings
- `POST /api/bookings` creates a booking with `tripId`, `userId`, `seatNumber`, and `fare`
- `GET /api/reports/financial` generates the financial report
- `GET /api/reports/financial.csv` downloads the financial report
- `GET /api/feedback` loads feedback
- `POST /api/announcements` publishes an announcement
- `DELETE /api/bookings/{id}` deletes a booking
- `DELETE /api/announcements/{id}` deletes an announcement

Example profile update:

```json
{
  "fullName": "Marcus Thorne",
  "email": "m.thorne@logistics-corp.com",
  "phone": "+1 (555) 019-2834"
}
```

The SQL schema does not contain columns for company, billing address, transit passes, or payment methods. Those interface fields need additional tables/columns before they can be persisted.

The `PasswordHash` values in the sample SQL are placeholders. Replace them with real BCrypt hashes before testing login. The backend intentionally rejects invalid or non-active credentials rather than accepting plain-text passwords.
