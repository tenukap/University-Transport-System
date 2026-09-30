# Database Structure — uni_transport_system (Final)
_Schema: V1–V7 as implemented after 2026-09-30 changes_

---

## A. Relationship Trees

### A1. User / role child tables

```
Users
 ├── student          (user_id PK/FK  ON DELETE CASCADE)
 ├── driver           (user_id PK/FK  ON DELETE CASCADE)
 ├── admin            (user_id PK/FK  ON DELETE CASCADE)
 ├── finance_officer  (user_id PK/FK  ON DELETE CASCADE)
 └── transport_officer(user_id PK/FK  ON DELETE CASCADE)
```

### A2. Bus / trip / booking chain

```
bus
 └── bustrip                     (bus_id FK, driver_user_id FK→driver)
      ├── booking                 (trip_id FK, user_id FK→student)
      │    ├── seat_reservation   (booking_id FK, bus_id FK)
      │    └── Feedback           (BookingId FK nullable)
      ├── Trip_Status             (TripId FK)
      ├── Location_Update         (TripId FK)
      └── tripcancellation        (TripId FK)
```

### A3. Finance chain

```
student
 └── invoice               (student_user_id FK nullable)
      └── payment           (invoice_id FK)
           └── paymentcancellation  (PaymentId FK)
```

### A4. Route / Destination (disconnected from bustrip)

```
BusRoute
 └── Destination   (RouteId FK)   — no Java entity; unused by code
```

### A5. Safety / feedback

```
Users
 ├── Emergency_Report  (user_id FK)
 ├── Feedback          (UserId FK)
 ├── SavedReports      (GeneratedByUserId FK)  — no entity
 ├── Announcements     (PostedByUserId FK)     — no entity
 └── AdminAuditLogs    (AdminUserId FK)        — no entity; TargetUserId intentionally has no FK

driver
 └── Crash_Incident    (driver_user_id FK)
```

### A6. Shared location lookup

```
location  ←── bustrip  (PickupLocationId, DropLocationId)
          ←── booking   (pickup_loc_id, dropoff_loc_id)
```

---

## B. Tables (FK-safe insert order)

---

### 1. `Users`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | `User.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| UserId | INT IDENTITY | NOT NULL | — | PK |
| FullName | VARCHAR(100) | NOT NULL | — | |
| Email | VARCHAR(150) | NOT NULL | — | UNIQUE |
| PasswordHash | VARCHAR(255) | NOT NULL | — | Plaintext in dev (BCrypt in AuthService) |
| Phone | VARCHAR(20) | NULL | — | |
| RoleName | VARCHAR(50) | NOT NULL | — | No DB CHECK; enforced by code |
| AccountStatus | VARCHAR(20) | NULL | `'Active'` | No DB CHECK; enforced by UserService |
| CreatedAt | DATETIME2 | NULL | `GETDATE()` | |
| UpdatedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `UserId`
**UNIQUE:** `UQ_Users_Email` on `Email`
**`RoleName` values used by code:** `'ADMIN'`, `'STUDENT'`, `'DRIVER'`, `'FINANCE_OFFICER'`, `'TRANSPORT_OFFICER'`
**`AccountStatus` values enforced by UserService:** `'Active'`, `'Suspended'`, `'Deactivated'`

---

### 2. `student`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | `Student.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| student_index | VARCHAR(50) | **NULL** | — | Filtered unique index (non-null values only) |
| semester | TINYINT | NULL | — | CHECK: NULL or 1–8 |

**PK:** `user_id`
**Unique index:** `UQ_student_index_filtered` on `student_index WHERE student_index IS NOT NULL`
**FK:** `FK_student_user`: `user_id → Users(UserId)` ON DELETE CASCADE
**CHECK:** `CK_student_semester`: `semester IS NULL OR semester BETWEEN 1 AND 8`

---

### 3. `driver`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | `Driver.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| license_number | NVARCHAR(100) | NOT NULL | — | UNIQUE |
| dob | DATE | NULL | — | |
| status | NVARCHAR(50) | NULL | `'Available'` | |

**PK:** `user_id`
**UNIQUE:** `UQ_driver_license` on `license_number`
**FK:** `FK_driver_user`: `user_id → Users(UserId)` ON DELETE CASCADE
**`status` values used by code:** `'Available'`, `'On Duty'`, `'Off Duty'`

---

### 4. `admin`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | `Admin.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| employee_id | VARCHAR(20) | NOT NULL | — | UNIQUE |
| department | VARCHAR(100) | NULL | — | |
| access_level | VARCHAR(20) | NOT NULL | `'STANDARD'` | CHECK: `'STANDARD'` or `'SUPER'` |

**PK:** `user_id`
**UNIQUE:** `UQ_admin_employee_id` on `employee_id`
**FK:** `FK_admin_user`: `user_id → Users(UserId)` ON DELETE CASCADE
**CHECK:** `CK_admin_access_level`: `access_level IN ('STANDARD','SUPER')`

---

### 5. `finance_officer`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | None — no Java entity |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| employee_id | VARCHAR(20) | NOT NULL | — | UNIQUE |
| approval_limit | DECIMAL(10,2) | NULL | — | CHECK: ≥ 0 or NULL |
| hire_date | DATE | NULL | — | |

**PK:** `user_id`
**UNIQUE:** `UQ_finance_officer_employee_id` on `employee_id`
**FK:** `FK_finance_officer_user`: `user_id → Users(UserId)` ON DELETE CASCADE
**CHECK:** `CK_finance_officer_limit`: `approval_limit IS NULL OR approval_limit >= 0`

---

### 6. `transport_officer`

| Defined in | `V1__users_and_roles.sql` |
|---|---|
| Entity | None — no Java entity |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| employee_id | VARCHAR(20) | NOT NULL | — | UNIQUE |
| depot | VARCHAR(100) | NULL | — | |
| hire_date | DATE | NULL | — | |

**PK:** `user_id`
**UNIQUE:** `UQ_transport_officer_employee_id` on `employee_id`
**FK:** `FK_transport_officer_user`: `user_id → Users(UserId)` ON DELETE CASCADE

---

### 7. `location`

| Defined in | `V2__fleet_and_locations.sql` |
|---|---|
| Entity | `Location.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| LocationId | INT IDENTITY | NOT NULL | — | PK |
| LocationName | VARCHAR(100) | NOT NULL | — | |
| Latitude | DECIMAL(9,6) | NULL | — | |
| Longitude | DECIMAL(9,6) | NULL | — | |

**PK:** `LocationId`

---

### 8. `bus`

| Defined in | `V2__fleet_and_locations.sql` |
|---|---|
| Entity | `Bus.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| bus_id | BIGINT IDENTITY | NOT NULL | — | PK |
| registration_number | VARCHAR(50) | NOT NULL | — | UNIQUE |
| passenger_capacity | INT | NOT NULL | — | |
| status | VARCHAR(20) | NULL | `'Available'` | No DB CHECK |

**PK:** `bus_id`
**UNIQUE:** `UQ_bus_registration` on `registration_number`
**`status` values used by code:** `'Available'`, `'In Service'`, `'Under Maintenance'`

---

### 9. `BusRoute`

| Defined in | `V2__fleet_and_locations.sql` |
|---|---|
| Entity | `BusRoute.java` — `@Table(name = "busroute")` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| RouteId | INT IDENTITY(1,1) | NOT NULL | — | PK |
| RouteName | NVARCHAR(100) | NOT NULL | — | |
| StartPoint | NVARCHAR(100) | NOT NULL | — | |
| EndPoint | NVARCHAR(100) | NOT NULL | — | |

**PK:** `RouteId`
**Note:** No FK from `bustrip` to `BusRoute`. Routes are managed via `/api/transport/routes` but not linked to individual trips.

---

### 10. `Destination`

| Defined in | `V2__fleet_and_locations.sql` |
|---|---|
| Entity | None — no Java entity; **UNUSED** |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| DestinationId | INT IDENTITY(1,1) | NOT NULL | — | PK |
| DestinationName | NVARCHAR(100) | NOT NULL | — | |
| Location | NVARCHAR(255) | NOT NULL | — | Free-text description |
| RouteId | INT | NOT NULL | — | FK → BusRoute(RouteId) |

**PK:** `DestinationId`
**FK:** `FK_Destination_BusRoute`: `RouteId → BusRoute(RouteId)` (no ON DELETE rule)
**Index:** `IX_Destination_RouteId` on `RouteId`

---

### 11. `bustrip`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `BusTrip.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| TripId | INT IDENTITY | NOT NULL | — | PK |
| bus_id | BIGINT | NOT NULL | — | FK → bus(bus_id) |
| driver_user_id | INT | NULL | — | FK → driver(user_id) — plain Integer in entity |
| TripDate | DATE | NOT NULL | — | |
| StartTime | TIME | NOT NULL | — | |
| ETA | TIME | NOT NULL | — | |
| PickupLocationId | INT | NULL | — | FK → location(LocationId) |
| DropLocationId | INT | NULL | — | FK → location(LocationId) |
| TripStatus | VARCHAR(20) | NULL | `'Scheduled'` | No DB CHECK |
| OperatingCost | DECIMAL(10,2) | NULL | `0.00` | |

**PK:** `TripId`
**FK:**
- `FK_bustrip_bus`: `bus_id → bus(bus_id)` (no ON DELETE)
- `FK_bustrip_driver`: `driver_user_id → driver(user_id)` (no ON DELETE)
- `FK_bustrip_pickup`: `PickupLocationId → location(LocationId)`
- `FK_bustrip_drop`: `DropLocationId → location(LocationId)`

**Indexes:** `IX_bustrip_bus_id`, `IX_bustrip_driver_user_id`
**`TripStatus` values used by code:** `'Scheduled'`, `'Completed'`, `'Cancelled'`

---

### 12. `booking`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `Booking.java` (ORM navigates user_id as `User user` — see note) |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| id | BIGINT IDENTITY | NOT NULL | — | PK |
| user_id | INT | NOT NULL | — | FK → student(user_id) |
| trip_id | INT | NOT NULL | — | FK → bustrip(TripId) |
| pickup_loc_id | INT | NULL | — | FK → location(LocationId) |
| dropoff_loc_id | INT | NULL | — | FK → location(LocationId) |
| seat_number | INT | NULL | — | |
| fare_amount | DECIMAL(10,2) | NULL | — | |
| status | VARCHAR(20) | NULL | `'PENDING'` | No DB CHECK |
| created_at | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `id`
**FK:**
- `FK_booking_student`: `user_id → student(user_id)` (no ON DELETE)
- `FK_booking_trip`: `trip_id → bustrip(TripId)` (no ON DELETE)
- `FK_booking_pickup`: `pickup_loc_id → location(LocationId)`
- `FK_booking_dropoff`: `dropoff_loc_id → location(LocationId)`

**Indexes:** `IX_booking_user_id`, `IX_booking_trip_id`
**Filtered unique index:** `UQ_booking_trip_seat_active` on `(trip_id, seat_number) WHERE status <> 'CANCELLED' AND seat_number IS NOT NULL`
**`status` values used by code:** `'PENDING'`, `'CONFIRMED'`, `'CANCELLED'`
**Note:** `Booking.java` maps `user_id` as `@ManyToOne private User user`. The DB FK points to `student`, but student.user_id equals Users.UserId numerically so the ORM load works. `BookingService.createBooking` verifies a `student` row exists before inserting.

---

### 13. `seat_reservation`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `SeatReservation.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| id | BIGINT IDENTITY | NOT NULL | — | PK |
| booking_id | BIGINT | NOT NULL | — | FK → booking(id) |
| bus_id | BIGINT | NOT NULL | — | FK → bus(bus_id) |
| seat_number | INT | NOT NULL | — | |
| reserved_at | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `id`
**FK:**
- `FK_seatres_booking`: `booking_id → booking(id)` (no ON DELETE)
- `FK_seatres_bus`: `bus_id → bus(bus_id)` (no ON DELETE)

**Index:** `IX_seatres_booking_id`

---

### 14. `Trip_Status`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `TripStatus.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| Status_Id | INT IDENTITY(1,1) | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Status_Type | VARCHAR(100) | NOT NULL | — | No DB CHECK |
| Updated_At | DATETIME | NULL | `GETDATE()` | |

**PK:** `Status_Id`
**FK:** `FK_TripStatus_Trip`: `TripId → bustrip(TripId)` (no ON DELETE)
**Index:** `IX_TripStatus_TripId`
**`Status_Type` values used by UI:** `'Scheduled'`, `'Departed'`, `'En Route'`, `'Cancelled'`

---

### 15. `Location_Update`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `LocationUpdate.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| Location_Update_Id | INT IDENTITY(1,1) | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Latitude | DECIMAL(10,8) | NOT NULL | — | |
| Longitude | DECIMAL(11,8) | NOT NULL | — | |
| Recorded_At | DATETIME | NULL | `GETDATE()` | |

**PK:** `Location_Update_Id`
**FK:** `FK_LocationUpdate_Trip`: `TripId → bustrip(TripId)` (no ON DELETE)
**Index:** `IX_LocUpdate_TripId`

---

### 16. `tripcancellation`

| Defined in | `V3__trips_and_bookings.sql` |
|---|---|
| Entity | `TripCancellation.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| CancellationId | INT IDENTITY(1,1) | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Reason | NVARCHAR(255) | NULL | — | |
| CancelledAt | DATETIME | NULL | `GETDATE()` | |

**PK:** `CancellationId`
**FK:** `FK_tripcancellation_bustrip`: `TripId → bustrip(TripId)` (no ON DELETE)
**Index:** `IX_tripcancellation_TripId`

---

### 17. `invoice`

| Defined in | `V4__finance.sql` |
|---|---|
| Entity | `Invoice.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| invoice_id | BIGINT IDENTITY(1,1) | NOT NULL | — | PK |
| billing_month | INT | NOT NULL | — | CHECK 1–12 |
| billing_year | INT | NOT NULL | — | CHECK ≥ 2020 |
| issue_date | DATE | NOT NULL | — | |
| due_date | DATE | NOT NULL | — | CHECK ≥ issue_date |
| total_amount | DECIMAL(10,2) | NOT NULL | — | CHECK ≥ 0 |
| student_user_id | INT | NULL | — | FK → student(user_id) |

**PK:** `invoice_id`
**FK:** `FK_invoice_student`: `student_user_id → student(user_id)` (no ON DELETE)
**Index:** `IX_invoice_student_user_id`
**CHECK:** `chk_invoice_month` (1–12), `chk_invoice_year` (≥ 2020), `chk_invoice_amount` (≥ 0), `chk_invoice_dates` (due_date ≥ issue_date)
**Note:** `Invoice.getInvoiceStatus()` is `@Transient` (computed at runtime; not stored).

---

### 18. `payment`

| Defined in | `V4__finance.sql` |
|---|---|
| Entity | `Payment.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| payment_id | BIGINT IDENTITY(1,1) | NOT NULL | — | PK |
| invoice_id | BIGINT | NOT NULL | — | FK → invoice(invoice_id) |
| amount | DECIMAL(10,2) | NOT NULL | — | CHECK > 0 |
| payment_date | DATE | NOT NULL | — | |
| payment_status | NVARCHAR(50) | NOT NULL | — | CHECK — see allowed values |

**PK:** `payment_id`
**FK:** `fk_payment_invoice`: `invoice_id → invoice(invoice_id)` ON DELETE NO ACTION
**Index:** `ix_payment_invoice_id`
**CHECK:** `chk_payment_amount` (> 0), `chk_payment_status` (`payment_status IN (N'PAID', N'PENDING', N'FAILED', N'CANCELLED')`)
**`payment_status` values:** `'PAID'`, `'PENDING'`, `'FAILED'`, `'CANCELLED'`

---

### 19. `paymentcancellation`

| Defined in | `V4__finance.sql` |
|---|---|
| Entity | `PaymentCancelation.java` (single-L in class name) |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| CancellationId | INT IDENTITY(1,1) | NOT NULL | — | PK |
| PaymentId | BIGINT | NOT NULL | — | FK → payment(payment_id) |
| Reason | NVARCHAR(255) | NULL | — | |
| CancelledAt | DATETIME | NULL | `GETDATE()` | |

**PK:** `CancellationId`
**FK:** `FK_paycancel_payment`: `PaymentId → payment(payment_id)` (no ON DELETE)
**Index:** `IX_paycancel_PaymentId`

---

### 20. `Emergency_Report`

| Defined in | `V5__safety_and_feedback.sql` |
|---|---|
| Entity | `EmergencyReport.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| Report_ID | INT IDENTITY(1,1) | NOT NULL | — | PK |
| Report_Title | VARCHAR(255) | NOT NULL | — | |
| Emergency_Type | VARCHAR(100) | NOT NULL | — | |
| Description | NVARCHAR(MAX) | NOT NULL | — | |
| Timestamp | DATETIME | NULL | `GETDATE()` | `insertable=false` in entity |
| Resolution_Status | VARCHAR(50) | NULL | `'Pending'` | |
| user_id | INT | NULL | — | FK → Users(UserId) |

**PK:** `Report_ID`
**FK:** `FK_Emergency_User`: `user_id → Users(UserId)` ON DELETE CASCADE
**Index:** `IX_EmgReport_user_id`
**Note:** Java field named `studentNo` (maps `user_id`). Any authenticated user can file a report. The misleading field name is intentionally left unchanged to avoid JSON shape change.
**`Resolution_Status` values used by code:** `'Pending'`, `'Resolved'`

---

### 21. `Crash_Incident`

| Defined in | `V5__safety_and_feedback.sql` |
|---|---|
| Entity | `CrashIncident.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| Incident_ID | INT IDENTITY(1,1) | NOT NULL | — | PK |
| bus_id | BIGINT | NULL | — | FK → bus(bus_id) |
| driver_user_id | INT | NULL | — | FK → driver(user_id) — plain Integer in entity |
| Location_Coordinates | VARCHAR(255) | NOT NULL | — | |
| Severity_Level | VARCHAR(50) | NOT NULL | — | |
| Description | NVARCHAR(MAX) | NULL | — | |
| Timestamp | DATETIME | NULL | `GETDATE()` | |
| Status | VARCHAR(50) | NULL | `'Reported'` | |

**PK:** `Incident_ID`
**FK:**
- `FK_Crash_Bus`: `bus_id → bus(bus_id)` (no ON DELETE)
- `FK_Crash_Driver`: `driver_user_id → driver(user_id)` (no ON DELETE)

**Indexes:** `IX_CrashInc_bus_id`, `IX_CrashInc_driver_user_id`
**`Status` values used by code:** `'Reported'`, `'Under Investigation'`, `'Resolved'`

---

### 22. `Feedback`

| Defined in | `V5__safety_and_feedback.sql` |
|---|---|
| Entity | `Feedback.java` |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| FeedbackId | INT IDENTITY | NOT NULL | — | PK |
| UserId | INT | NOT NULL | — | FK → Users(UserId) |
| BookingId | BIGINT | NULL | — | FK → booking(id) — plain Long in entity |
| Subject | VARCHAR(150) | NOT NULL | — | |
| Message | NVARCHAR(MAX) | NOT NULL | — | Entity field named `comments` |
| Rating | INT | NULL | — | CHECK: NULL or 1–5 |
| Status | VARCHAR(20) | NULL | `'Pending'` | |
| SubmittedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `FeedbackId`
**FK:**
- `FK_Feedback_User`: `UserId → Users(UserId)` ON DELETE CASCADE
- `FK_Feedback_Booking`: `BookingId → booking(id)` (no ON DELETE)

**Indexes:** `IX_Feedback_UserId`, `IX_Feedback_BookingId`
**CHECK:** `CK_Feedback_Rating`: `Rating IS NULL OR (Rating BETWEEN 1 AND 5)`
**`Status` values used by code:** `'Pending'`, `'Reviewed'`

---

### 23. `SavedReports`

| Defined in | `V6__admin.sql` |
|---|---|
| Entity | None — no Java entity; **UNUSED** |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| ReportId | INT IDENTITY | NOT NULL | — | PK |
| ReportTitle | VARCHAR(150) | NOT NULL | — | |
| ReportType | VARCHAR(50) | NOT NULL | — | |
| StartDate | DATE | NOT NULL | — | |
| EndDate | DATE | NOT NULL | — | |
| GeneratedByUserId | INT | NOT NULL | — | FK → Users(UserId) |
| SummaryNotes | NVARCHAR(MAX) | NULL | — | |
| GeneratedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `ReportId`
**FK:** `FK_SavedReports_User`: `GeneratedByUserId → Users(UserId)` ON DELETE CASCADE

---

### 24. `Announcements`

| Defined in | `V6__admin.sql` |
|---|---|
| Entity | None — no Java entity; **UNUSED** |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| AnnouncementId | INT IDENTITY | NOT NULL | — | PK |
| Title | VARCHAR(200) | NOT NULL | — | |
| Content | NVARCHAR(MAX) | NOT NULL | — | |
| PostedByUserId | INT | NOT NULL | — | FK → Users(UserId) |
| CreatedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `AnnouncementId`
**FK:** `FK_Announcements_User`: `PostedByUserId → Users(UserId)` ON DELETE CASCADE

---

### 25. `AdminAuditLogs`

| Defined in | `V6__admin.sql` |
|---|---|
| Entity | None — no Java entity; **UNUSED** |

| Column | SQL type | NULL | Default | Notes |
|---|---|---|---|---|
| LogId | INT IDENTITY | NOT NULL | — | PK |
| AdminUserId | INT | NOT NULL | — | FK → Users(UserId) |
| ActionPerformed | VARCHAR(100) | NOT NULL | — | |
| TargetUserId | INT | NULL | — | **No FK** — intentional; audit logs must outlive deleted users |
| Timestamp | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `LogId`
**FK:** `FK_AdminAuditLogs_User`: `AdminUserId → Users(UserId)` ON DELETE CASCADE
**Note:** `TargetUserId` intentionally has no FK constraint (see V6 comment). Audit history must be preserved when referenced users are deleted.
