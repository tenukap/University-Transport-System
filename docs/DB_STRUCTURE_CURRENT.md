# Current Database Structure (V1–V8 as-is)

> Describes the schema **exactly as it stands today** in migrations V1–V8.
> No changes have been applied. "Unused" and "no entity" are noted per table.

---

## A. Relationship Trees

### A1. User / role child tables

```
Users
 ├── student     (user_id)          — STUDENT role profile
 └── driver      (user_id)          — DRIVER role profile
```

ADMIN, FINANCE_OFFICER, TRANSPORT_OFFICER have no child tables.

### A2. Bus / trip / booking chain

```
bus
 └── bustrip                    (bus_id)
      ├── booking                (trip_id)
      │    ├── seat_reservation  (booking_id)
      │    └── Feedback          (BookingId, nullable)
      ├── Trip_Status            (TripId)
      ├── Location_Update        (TripId)
      └── tripcancellation       (TripId)
```

### A3. Finance chain

```
invoice
 └── payment  (invoice_id)
      └── paymentcancellation  (PaymentId — no FK constraint!)
```

### A4. Route / destination (disconnected from bustrip)

```
BusRoute
 └── Destination  (RouteId)
```

### A5. Standalone tables (FK back to Users only)

```
Users
 ├── booking           (user_id)
 ├── Emergency_Report  (user_id)
 ├── Crash_Incident    (user_id)
 ├── SavedReports      (GeneratedByUserId)
 ├── Announcements     (PostedByUserId)
 ├── Feedback          (UserId)
 └── AdminAuditLogs    (AdminUserId, TargetUserId nullable)
```

### A6. Shared lookup table

```
location  ←── bustrip (PickupLocationId, DropLocationId)
          ←── booking  (pickup_loc_id,   dropoff_loc_id)
```

---

## B. Tables (FK-safe insert order)

---

### 1. `Users`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `User.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| UserId | INT IDENTITY | NOT NULL | — | PK |
| FullName | VARCHAR(100) | NOT NULL | — | |
| Email | VARCHAR(150) | NOT NULL | — | UNIQUE |
| PasswordHash | VARCHAR(255) | NOT NULL | — | BCrypt in prod; plaintext in dev seeds |
| Phone | VARCHAR(20) | NULL | — | |
| RoleName | VARCHAR(50) | NOT NULL | — | See allowed values below |
| AccountStatus | VARCHAR(20) | NULL | `'Active'` | See allowed values below |
| CreatedAt | DATETIME2 | NULL | `GETDATE()` | |
| UpdatedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `UserId`
**UNIQUE:** `Email`
**Allowed `RoleName` values (by code/seed):** `'ADMIN'`, `'STUDENT'`, `'DRIVER'`, `'FINANCE_OFFICER'`, `'TRANSPORT_OFFICER'`
**Allowed `AccountStatus` values (by seed):** `'Active'`, `'Suspended'`, `'Deactivated'`
**No CHECK constraints on RoleName or AccountStatus.**

---

### 2. `student`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `Student.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| id | BIGINT IDENTITY | NOT NULL | — | PK (surrogate) |
| user_id | INT | NOT NULL | — | UNIQUE; FK → Users(UserId) |
| student_index | VARCHAR(50) | NOT NULL | — | UNIQUE |
| full_name | VARCHAR(100) | NOT NULL | — | **Duplicate of Users.FullName** |
| phone | VARCHAR(20) | NULL | — | **Duplicate of Users.Phone** |

**PK:** `id`
**UNIQUE:** `user_id`, `student_index`
**FK:** `FK_student_user`: `user_id → Users(UserId)` ON DELETE CASCADE

---

### 3. `location`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `Location.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| LocationId | INT IDENTITY | NOT NULL | — | PK |
| LocationName | VARCHAR(100) | NOT NULL | — | |
| Latitude | DECIMAL(9,6) | NULL | — | |
| Longitude | DECIMAL(9,6) | NULL | — | |

**PK:** `LocationId`

---

### 4. `bus`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `Bus.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| bus_id | BIGINT IDENTITY | NOT NULL | — | PK |
| registration_number | VARCHAR(50) | NOT NULL | — | UNIQUE |
| passenger_capacity | INT | NOT NULL | — | |
| status | VARCHAR(20) | NULL | `'Available'` | `'Available'` by seed |

**PK:** `bus_id`
**UNIQUE:** `registration_number`

---

### 5. `BusRoute`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `BusRoute.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| RouteId | INT IDENTITY | NOT NULL | — | PK |
| RouteName | NVARCHAR(100) | NOT NULL | — | |
| StartPoint | NVARCHAR(100) | NOT NULL | — | |
| EndPoint | NVARCHAR(100) | NOT NULL | — | |

**PK:** `RouteId`
**Note:** Not linked to `bustrip` — disconnected route catalogue.

---

### 6. `Destination`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | **None** — no Java entity |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| DestinationId | INT IDENTITY | NOT NULL | — | PK |
| DestinationName | NVARCHAR(100) | NOT NULL | — | |
| Location | NVARCHAR(255) | NOT NULL | — | Free-text location description |
| RouteId | INT | NOT NULL | — | FK → BusRoute(RouteId) |

**PK:** `DestinationId`
**FK:** `FK_Destination_BusRoute`: `RouteId → BusRoute(RouteId)` (no ON DELETE rule stated)
**Note:** No Java entity. Not used by any service or controller.

---

### 7. `driver`

| Migration | `V7__add_fleet_tables.sql` |
|-----------|--------------------------|
| Entity | `Driver.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| user_id | INT | NOT NULL | — | PK + FK → Users(UserId) |
| license_number | NVARCHAR(100) | NOT NULL | — | UNIQUE |
| dob | DATE | NULL | — | |
| status | NVARCHAR(50) | NULL | `'Available'` | |

**PK:** `user_id`
**UNIQUE:** `license_number`
**FK:** `fk_driver_user`: `user_id → Users(UserId)` ON DELETE CASCADE

---

### 8. `bustrip`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `BusTrip.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| TripId | INT IDENTITY | NOT NULL | — | PK |
| bus_id | BIGINT | NOT NULL | — | FK → bus(bus_id) |
| TripDate | DATE | NOT NULL | — | |
| StartTime | TIME | NOT NULL | — | |
| ETA | TIME | NOT NULL | — | |
| PickupLocationId | INT | NULL | — | FK → location(LocationId) |
| DropLocationId | INT | NULL | — | FK → location(LocationId) |
| TripStatus | VARCHAR(20) | NULL | `'Scheduled'` | See allowed values |
| OperatingCost | DECIMAL(10,2) | NULL | `0.00` | |

**PK:** `TripId`
**FK:**
- `FK_bustrip_bus`: `bus_id → bus(bus_id)` (no ON DELETE rule)
- `FK_bustrip_pickup`: `PickupLocationId → location(LocationId)`
- `FK_bustrip_drop`: `DropLocationId → location(LocationId)`

**Allowed `TripStatus` values (by code):** `'Scheduled'`, `'Completed'`, `'Cancelled'`
**Missing:** No `driver_user_id` FK — no driver is assigned to a trip.

---

### 9. `booking`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `Booking.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| id | BIGINT IDENTITY | NOT NULL | — | PK |
| user_id | INT | NOT NULL | — | FK → Users(UserId) — should be student |
| trip_id | INT | NOT NULL | — | FK → bustrip(TripId) |
| pickup_loc_id | INT | NULL | — | FK → location(LocationId) |
| dropoff_loc_id | INT | NULL | — | FK → location(LocationId) |
| seat_number | INT | NULL | — | |
| fare_amount | DECIMAL(10,2) | NULL | — | |
| status | VARCHAR(20) | NULL | `'PENDING'` | See allowed values |
| created_at | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `id`
**FK:**
- `FK_booking_user`: `user_id → Users(UserId)` ON DELETE CASCADE
- `FK_booking_trip`: `trip_id → bustrip(TripId)` (no ON DELETE rule)
- `FK_booking_pickup`: `pickup_loc_id → location(LocationId)`
- `FK_booking_dropoff`: `dropoff_loc_id → location(LocationId)`

**Allowed `status` values (by code):** `'PENDING'`, `'CONFIRMED'`, `'CANCELLED'`

---

### 10. `seat_reservation`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `SeatReservation.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| id | BIGINT IDENTITY | NOT NULL | — | PK |
| booking_id | BIGINT | NOT NULL | — | FK → booking(id) |
| bus_id | BIGINT | NOT NULL | — | FK → bus(bus_id) |
| seat_number | INT | NOT NULL | — | |
| reserved_at | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `id`
**FK:**
- `FK_seatres_booking`: `booking_id → booking(id)` (no ON DELETE rule)
- `FK_seatres_bus`: `bus_id → bus(bus_id)` (no ON DELETE rule)

---

### 11. `SavedReports`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | **None** — no Java entity |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| ReportId | INT IDENTITY | NOT NULL | — | PK |
| ReportTitle | VARCHAR(150) | NOT NULL | — | |
| ReportType | VARCHAR(50) | NOT NULL | — | `'Operations'`, `'Financial'` (by seed) |
| StartDate | DATE | NOT NULL | — | |
| EndDate | DATE | NOT NULL | — | |
| GeneratedByUserId | INT | NOT NULL | — | FK → Users(UserId) |
| SummaryNotes | NVARCHAR(MAX) | NULL | — | |
| GeneratedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `ReportId`
**FK:** `FK_savedreports_user`: `GeneratedByUserId → Users(UserId)` ON DELETE CASCADE
**Note:** No Java entity. Table exists but no controller or service reads/writes it via JPA.

---

### 12. `Announcements`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | **None** — no Java entity |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| AnnouncementId | INT IDENTITY | NOT NULL | — | PK |
| Title | VARCHAR(200) | NOT NULL | — | |
| Content | NVARCHAR(MAX) | NOT NULL | — | |
| PostedByUserId | INT | NOT NULL | — | FK → Users(UserId) |
| CreatedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `AnnouncementId`
**FK:** `FK_announcements_user`: `PostedByUserId → Users(UserId)` ON DELETE CASCADE
**Note:** No Java entity. Used by seed only.

---

### 13. `Feedback`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | `Feedback.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| FeedbackId | INT IDENTITY | NOT NULL | — | PK |
| UserId | INT | NOT NULL | — | FK → Users(UserId) |
| BookingId | BIGINT | NULL | — | FK → booking(id) |
| Subject | VARCHAR(150) | NOT NULL | — | |
| Message | NVARCHAR(MAX) | NOT NULL | — | Mapped as `comments` in entity |
| Rating | INT | NULL | — | CHECK 1–5 or NULL |
| Status | VARCHAR(20) | NULL | `'Pending'` | `'Pending'`, `'Reviewed'` (by seed) |
| SubmittedAt | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `FeedbackId`
**UNIQUE:** None
**FK:**
- `FK_feedback_user`: `UserId → Users(UserId)` ON DELETE CASCADE
- `FK_feedback_booking`: `BookingId → booking(id)` (no ON DELETE rule, nullable)
**CHECK:** `CK_feedback_rating`: `Rating IS NULL OR (Rating BETWEEN 1 AND 5)`
**Note:** `Feedback.java` does NOT map the `Rating` or `BookingId` columns (they are not fields in the entity — unmapped columns).

---

### 14. `AdminAuditLogs`

| Migration | `V1__schema.sql` |
|-----------|-----------------|
| Entity | **None** — no Java entity |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| LogId | INT IDENTITY | NOT NULL | — | PK |
| AdminUserId | INT | NOT NULL | — | FK → Users(UserId) |
| ActionPerformed | VARCHAR(100) | NOT NULL | — | |
| TargetUserId | INT | NULL | — | **No FK constraint declared** |
| Timestamp | DATETIME2 | NULL | `GETDATE()` | |

**PK:** `LogId`
**FK:** `FK_auditlogs_user`: `AdminUserId → Users(UserId)` ON DELETE CASCADE
**Missing FK:** `TargetUserId` references `Users(UserId)` by convention but has no declared constraint.
**Note:** No Java entity. Used by seed only.

---

### 15. `Emergency_Report`

| Migration | `V2__emergency_rt.sql` |
|-----------|----------------------|
| Entity | `EmergencyReport.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| Report_ID | INT IDENTITY | NOT NULL | — | PK |
| Report_Title | VARCHAR(255) | NOT NULL | — | |
| Emergency_Type | VARCHAR(100) | NOT NULL | — | |
| Description | NVARCHAR(MAX) | NOT NULL | — | |
| Timestamp | DATETIME | NULL | `GETDATE()` | `insertable=false` in entity |
| Resolution_Status | VARCHAR(50) | NULL | `'Pending'` | |
| user_id | INT | NULL | — | FK → Users(UserId) |

**PK:** `Report_ID`
**FK:** `FK_Emergency_User`: `user_id → Users(UserId)` ON DELETE CASCADE
**Note:** Java field is named `studentNo` — misleading; any user can be linked.

---

### 16. `Crash_Incident`

| Migration | `V3__crash_incident.sql` |
|-----------|------------------------|
| Entity | `CrashIncident.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| Incident_ID | INT IDENTITY | NOT NULL | — | PK |
| bus_id | BIGINT | NULL | — | FK → bus(bus_id) |
| user_id | INT | NULL | — | FK → Users(UserId) — semantically driver |
| Location_Coordinates | VARCHAR(255) | NOT NULL | — | |
| Severity_Level | VARCHAR(50) | NOT NULL | — | |
| Description | NVARCHAR(MAX) | NULL | — | |
| Timestamp | DATETIME | NULL | `GETDATE()` | |
| Status | VARCHAR(50) | NULL | `'Reported'` | `'Reported'`, `'Resolved'`, `'Under Investigation'` (by seed) |

**PK:** `Incident_ID`
**FK:**
- `FK_Crash_Bus`: `bus_id → bus(bus_id)` (no ON DELETE rule)
- `FK_Crash_User`: `user_id → Users(UserId)` ON DELETE CASCADE
**Note:** Java field `driverNo` maps `user_id` — semantically a driver, but FK points to Users.

---

### 17. `Trip_Status`

| Migration | `V5__transp_status.sql` |
|-----------|------------------------|
| Entity | `TripStatus.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| Status_Id | INT IDENTITY | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Status_Type | VARCHAR(100) | NULL | — | See allowed values |
| Updated_At | DATETIME | NULL | `GETDATE()` | |

**PK:** `Status_Id`
**FK:** `FK_TripStatus_Trip`: `TripId → bustrip(TripId)` (no ON DELETE rule)
**Allowed `Status_Type` values (by seed):** `'Scheduled'`, `'Departed'`, `'En Route'`, `'Cancelled'`
**Note:** `TripStatus.java` stores `TripId` as a plain `Integer` — no `@ManyToOne` join.

---

### 18. `Location_Update`

| Migration | `V5__transp_status.sql` |
|-----------|------------------------|
| Entity | `LocationUpdate.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| Location_Update_Id | INT IDENTITY | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Latitude | DECIMAL(10,8) | NOT NULL | — | |
| Longitude | DECIMAL(11,8) | NOT NULL | — | |
| Recorded_At | DATETIME | NULL | `GETDATE()` | |

**PK:** `Location_Update_Id`
**FK:** `FK_LocationUpdate_Trip`: `TripId → bustrip(TripId)` (no ON DELETE rule)
**Note:** `LocationUpdate.java` stores `TripId` as a plain `Integer` — no `@ManyToOne` join.

---

### 19. `invoice`

| Migration | `V6__invoice_payment.sql` |
|-----------|--------------------------|
| Entity | `Invoice.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| invoice_id | BIGINT IDENTITY | NOT NULL | — | PK |
| billing_month | INT | NOT NULL | — | CHECK 1–12 |
| billing_year | INT | NOT NULL | — | CHECK >= 2020 |
| issue_date | DATE | NOT NULL | — | |
| due_date | DATE | NOT NULL | — | CHECK >= issue_date |
| total_amount | DECIMAL(10,2) | NOT NULL | — | CHECK >= 0 |

**PK:** `invoice_id`
**CHECK constraints:** `chk_invoice_month` (1–12), `chk_invoice_year` (>= 2020), `chk_invoice_amount` (>= 0), `chk_invoice_dates` (due_date >= issue_date)
**Note:** No FK to any user. `Invoice.getInvoiceStatus()` is a `@Transient` computed property.

---

### 20. `payment`

| Migration | `V6__invoice_payment.sql` + `V8__transport_officer.sql` (extended CHECK) |
|-----------|-------------------------------------------------------------------------|
| Entity | `Payment.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| payment_id | BIGINT IDENTITY | NOT NULL | — | PK |
| invoice_id | BIGINT | NOT NULL | — | FK → invoice(invoice_id) |
| amount | DECIMAL(10,2) | NOT NULL | — | CHECK > 0 |
| payment_date | DATE | NOT NULL | — | |
| payment_status | NVARCHAR(50) | NOT NULL | — | CHECK — see allowed values |

**PK:** `payment_id`
**FK:** `fk_payment_invoice`: `invoice_id → invoice(invoice_id)` ON DELETE NO ACTION ON UPDATE NO ACTION
**CHECK constraints:** `chk_payment_amount` (amount > 0), `chk_payment_status` (V6: PAID|PENDING|FAILED; extended in V8 to also allow CANCELLED)
**INDEX:** `ix_payment_invoice_id` on `invoice_id`
**Allowed `payment_status` values:** `'PAID'`, `'PENDING'`, `'FAILED'`, `'CANCELLED'`

---

### 21. `paymentcancellation`

| Migration | `V8__transport_officer.sql` |
|-----------|---------------------------|
| Entity | `PaymentCancelation.java` (note single-L spelling) |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| CancellationId | INT IDENTITY | NOT NULL | — | PK |
| PaymentId | INT | NOT NULL | — | **No FK constraint declared** |
| Reason | NVARCHAR(255) | NULL | — | |
| CancelledAt | DATETIME | NULL | `GETDATE()` | |

**PK:** `CancellationId`
**Missing FK:** `PaymentId` should reference `payment(payment_id)` but no constraint exists.
**Note:** Java field `paymentId` is `Long`; column is `INT` — minor type mismatch (no runtime issue unless value exceeds INT range).

---

### 22. `tripcancellation`

| Migration | `V8__transport_officer.sql` |
|-----------|---------------------------|
| Entity | `TripCancellation.java` |

| Column | SQL type | Null | Default | Notes |
|--------|----------|------|---------|-------|
| CancellationId | INT IDENTITY | NOT NULL | — | PK |
| TripId | INT | NOT NULL | — | FK → bustrip(TripId) |
| Reason | NVARCHAR(255) | NULL | — | |
| CancelledAt | DATETIME | NULL | `GETDATE()` | |

**PK:** `CancellationId`
**FK:** `FK_tripcancellation_bustrip`: `TripId → bustrip(TripId)` (no ON DELETE rule)
**Note:** V8 comment says "Note: V1 already has a separate 'TripCancelation' (single-L) table" — but examining V1 there is **no** such table. The comment is incorrect; only `tripcancellation` (this table) exists.
