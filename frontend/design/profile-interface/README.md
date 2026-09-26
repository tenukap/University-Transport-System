# FleetTrack backend

Spring Boot REST API for the BusTrans Pro admin dashboard.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```powershell
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Endpoints

- `GET /api/dashboard` - overview metrics, recent users, and feedback
- `GET /api/users` - list users
- `POST /api/users` - create a user
- `PUT /api/users/{id}` - update a user's role and status
- `GET /api/bookings` - list bookings
- `POST /api/bookings` - create a booking
- `GET /api/reports/financial` - generate a JSON report
- `GET /api/reports/financial.csv` - download the report as CSV
- `GET /api/feedback` - list user feedback
- `POST /api/announcements` - publish an announcement

Example:

```powershell
Invoke-RestMethod http://localhost:8080/api/dashboard
```

## Connect to your SQL Server database

1. Run `SQLQuery6.sql` in SQL Server Management Studio. It creates `CampusTransportDB`, tables, seed data, and stored procedures.
2. Set the connection environment variables before starting the API:

```powershell
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=CampusTransportDB;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "your-sql-server-user"
$env:DB_PASSWORD = "your-password"
mvn spring-boot:run
```

The API now reads and writes dashboard data using the SQL schema. Do not commit database credentials. Booking creation expects `tripId`, `userId`, `seatNumber`, and `fare`, matching the `Bookings` table:

```json
{
	"tripId": 1,
	"userId": 3,
	"seatNumber": 12,
	"fare": 150.00
}
```
