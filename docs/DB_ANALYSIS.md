# Database Design Analysis

> Phase 1 — Analysis only. No changes have been made.

---

## 1. Role Inventory

| Role | Role-specific columns | Where they live today |
|------|----------------------|----------------------|
| ADMIN | None | Users only |
| STUDENT | `student_index`, `full_name`*, `phone`* | `student` table (`id` surrogate PK) |
| DRIVER | `license_number`, `dob`, `status` | `driver` table (`user_id` PK) |
| FINANCE_OFFICER | None | Users only |
| TRANSPORT_OFFICER | None | Users only |

*`full_name` and `phone` are role-specific in form only — they duplicate columns that already exist in `Users` (see §2).

**Verdict on which roles need a child table:**

| Role | Child table needed? | Reason |
|------|--------------------|-|
| ADMIN | No | No role-specific data beyond Users |
| STUDENT | Yes | `student_index` and (after refactor) `semester` are student-only |
| DRIVER | Yes (already exists) | `license_number`, `dob`, `status` are driver-only |
| FINANCE_OFFICER | No | No role-specific data |
| TRANSPORT_OFFICER | No | No role-specific data |

---

## 2. Redundancy

### 2a. Duplicated columns

| Column in child table | Exact duplicate in Users | Notes |
|-----------------------|--------------------------|-------|
| `student.full_name` | `Users.FullName` | Populated with the same value in every seed insert. Edits via `StudentService.updateStudent()` update only `student.full_name`, leaving `Users.FullName` stale. |
| `student.phone` | `Users.Phone` | Same as above — `StudentService` updates only `student.phone`. |

### 2b. Surrogate ID that duplicates user_id

| Column | Issue |
|--------|-------|
| `student.id` (BIGINT IDENTITY) | `student.user_id` is already declared `UNIQUE` in V1. The surrogate `id` adds a second identity with no value. Every caller (`StudentRepository`, `StudentService`, `StudentController`) navigates via `user_id`, but then returns `student.id` as the DTO's `id` field, which propagates to the frontend. This creates a confusing dual-identity: the API exposes `student.id`, but auth tokens carry `Users.UserId`. |

---

## 3. FK Review — every FK that points to Users(UserId)

| Child table | Child column | Role that acts | Current target | Recommended target | Reason | Code impact |
|---|---|---|---|---|---|---|
| `student` | `user_id` | STUDENT | `Users(UserId)` | `Users(UserId)` ✓ | Correct. PK should be user_id (drop surrogate id). | Medium — entity, DTOs, service |
| `driver` | `user_id` | DRIVER | `Users(UserId)` | `Users(UserId)` ✓ | Correct. Already the PK with no surrogate. | None |
| `booking` | `user_id` | STUDENT | `Users(UserId)` | `student(user_id)` | Only students book. Pointing directly to Users allows any role to book without validation. Changing to `student(user_id)` enforces that at DB level. | Medium — `BookingService` loads `User` now; would need to load `Student` (or keep loading `User` but add a role check) |
| `Emergency_Report` | `user_id` | Any user (seed uses TRANSPORT_OFFICER and STUDENT) | `Users(UserId)` | `Users(UserId)` ✓ | Both students and transport officers submit reports. Keep as-is. Java field `studentNo` is misleadingly named. | Low — rename Java field only |
| `Crash_Incident` | `user_id` | DRIVER | `Users(UserId)` | `driver(user_id)` | Java field is named `driverNo`. A crash is always reported by/linked to a driver. Changing FK target enforces this. | Low for schema; medium for service if validation added |
| `SavedReports` | `GeneratedByUserId` | ADMIN or FINANCE_OFFICER | `Users(UserId)` | `Users(UserId)` ✓ | Multiple roles can generate reports. No child table to point to. | None |
| `Announcements` | `PostedByUserId` | ADMIN | `Users(UserId)` | `Users(UserId)` ✓ | Admins post announcements. No child table. | None |
| `Feedback` | `UserId` | STUDENT | `Users(UserId)` | `student(user_id)` | Only students give feedback about trips. Changing target enforces this. | Low — `Feedback` entity already loads `User user` lazily |
| `AdminAuditLogs` | `AdminUserId` | ADMIN | `Users(UserId)` | `Users(UserId)` ✓ | Correct. | None |
| `AdminAuditLogs` | `TargetUserId` | Any user | `Users(UserId)` (implied; no FK constraint declared) | `Users(UserId)` — and add constraint | TargetUserId has no FK constraint in V1. Should add one. | Low — add constraint only |

---

## 4. Missing Links

### 4a. Driver-to-Trip assignment (CRITICAL)
`bustrip` has no column linking a trip to a specific driver. The `driver` table exists and drivers have accounts, but there is no `driver_id` / `assigned_user_id` FK on `bustrip`. Consequently:
- Drivers can log in but cannot see "their" trips.
- `Crash_Incident.user_id` links a crash to a user but the trip involved in the crash is not recorded.
- The driver portal page (`driver.html`) exists but has no trip-assignment data to display.

**Proposed fix:** Add `driver_user_id INT NULL REFERENCES driver(user_id)` to `bustrip`.

### 4b. Invoice-to-User link (missing)
`invoice` has no FK to `Users`. There is no way to know which student (or which month's usage) an invoice covers, or who the responsible officer is. The finance module is isolated from the booking/user model.

**Proposed fix (open question):** Decide whether an invoice belongs to a student (monthly travel bill) or is a system-level operating invoice. See §7 Q5.

### 4c. paymentcancellation has no FK constraint
`paymentcancellation.PaymentId` is declared as `INT NOT NULL` in V8 but has **no CONSTRAINT FK line**. It references `payment.payment_id` by convention only. If a payment is deleted, orphan rows will exist silently.

**Proposed fix:** Add `CONSTRAINT FK_paymentcancel_payment FOREIGN KEY (PaymentId) REFERENCES payment(payment_id)` in V8.

### 4d. BusRoute / Destination not linked to bustrip
`BusRoute` and `Destination` tables exist and have seed data, but `bustrip` does not reference them. Trips are scheduled with direct `location` FKs, not with route references. The two subsystems are entirely disconnected.

---

## 5. Proposed Schema

### 5a. Users (unchanged)
```sql
CREATE TABLE Users (
    UserId        INT IDENTITY PRIMARY KEY,
    FullName      VARCHAR(100) NOT NULL,
    Email         VARCHAR(150) NOT NULL UNIQUE,
    PasswordHash  VARCHAR(255) NOT NULL,
    Phone         VARCHAR(20) NULL,
    RoleName      VARCHAR(50) NOT NULL,  -- 'ADMIN'|'STUDENT'|'DRIVER'|'FINANCE_OFFICER'|'TRANSPORT_OFFICER'
    AccountStatus VARCHAR(20) DEFAULT 'Active',  -- 'Active'|'Suspended'|'Deactivated'
    CreatedAt     DATETIME2 DEFAULT GETDATE(),
    UpdatedAt     DATETIME2 DEFAULT GETDATE()
);
```

### 5b. student (refactored)
```sql
CREATE TABLE student (
    user_id       INT PRIMARY KEY,  -- was surrogate id; user_id was UNIQUE already
    student_index VARCHAR(50) NULL,  -- changed from NOT NULL UNIQUE to nullable
    semester      TINYINT NULL,
    CONSTRAINT FK_student_user FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE,
    CONSTRAINT CK_student_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 8)
);
-- Filtered unique index: two students cannot share a non-null index, but NULLs are allowed
CREATE UNIQUE INDEX UQ_student_index ON student(student_index)
    WHERE student_index IS NOT NULL;
```

**Columns removed:** `id` (surrogate), `full_name` (→ Users.FullName), `phone` (→ Users.Phone)

### 5c. driver (unchanged — already correct)
```sql
CREATE TABLE driver (
    user_id        INT PRIMARY KEY,
    license_number NVARCHAR(100) NOT NULL UNIQUE,
    dob            DATE NULL,
    status         NVARCHAR(50) DEFAULT 'Available',
    CONSTRAINT fk_driver_user FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE
);
```

### 5d. bustrip (add driver assignment)
```sql
ALTER TABLE bustrip
    ADD driver_user_id INT NULL,
    CONSTRAINT FK_bustrip_driver FOREIGN KEY (driver_user_id)
        REFERENCES driver(user_id);
```

### 5e. Tables where FK target should change

#### booking.user_id → student(user_id)
```sql
-- Drop current FK
ALTER TABLE booking DROP CONSTRAINT FK_booking_user;
-- Re-add pointing to student
ALTER TABLE booking ADD CONSTRAINT FK_booking_user
    FOREIGN KEY (user_id) REFERENCES student(user_id) ON DELETE CASCADE;
```

#### Crash_Incident.user_id → driver(user_id)
```sql
ALTER TABLE Crash_Incident DROP CONSTRAINT FK_Crash_User;
ALTER TABLE Crash_Incident ADD CONSTRAINT FK_Crash_Driver
    FOREIGN KEY (user_id) REFERENCES driver(user_id) ON DELETE CASCADE;
```

#### Feedback.UserId → student(user_id)
```sql
ALTER TABLE Feedback DROP CONSTRAINT FK_feedback_user;
ALTER TABLE Feedback ADD CONSTRAINT FK_feedback_user
    FOREIGN KEY (UserId) REFERENCES student(user_id) ON DELETE CASCADE;
```

#### paymentcancellation.PaymentId — add missing FK
```sql
ALTER TABLE paymentcancellation ADD CONSTRAINT FK_paymentcancel_payment
    FOREIGN KEY (PaymentId) REFERENCES payment(payment_id);
```

---

## 6. Change List

All migration files are edited **in-place** (V1–V8) because the project uses `flyway:clean` before each run.

### 6a. Migration files

| File | Change | Risk | JSON to frontend changes? |
|------|--------|------|--------------------------|
| `V1__schema.sql` | `student`: drop `id`, drop `full_name`, drop `phone`; change PK to `user_id`; make `student_index` NULL; add `semester TINYINT NULL CHECK`; add filtered unique index. Add FK constraint on `AdminAuditLogs.TargetUserId`. | Medium | Yes — `StudentResponseDTO.id` becomes `user_id` |
| `V1__schema.sql` | `booking`: change FK `FK_booking_user` to reference `student(user_id)` instead of `Users(UserId)` | Medium | No — `userId` in DTO stays the same value |
| `V1__schema.sql` | `Feedback`: change FK to reference `student(user_id)` | Low | No |
| `V3__crash_incident.sql` | Change FK `FK_Crash_User` to reference `driver(user_id)` | Low | No |
| `V7__add_fleet_tables.sql` | `bustrip`: add `driver_user_id INT NULL FK->driver(user_id)` | Low (additive) | Yes — `TripResponseDTO` gains `driverUserId` |
| `V8__transport_officer.sql` | Add FK constraint on `paymentcancellation.PaymentId` | Low | No |

### 6b. Java entity files

| File | Change | Risk |
|------|--------|------|
| `Student.java` | Remove `id`, `fullName`, `phone` fields. Change `@Id` to `userId` (annotate with `@ManyToOne @MapsId` or plain `@Column`). Add `semester` field. | High — many downstream references |
| `CrashIncident.java` | Rename field `driverNo` → `userId` (or keep name, add comment). No functional change if FK stays on Users and the column name doesn't change. | Low |
| `EmergencyReport.java` | Rename field `studentNo` → `userId`. No functional change. | Low |
| `BusTrip.java` | Add `@ManyToOne @JoinColumn(name="driver_user_id") private Driver driver;` | Low |

### 6c. Repository files

| File | Change | Risk |
|------|--------|------|
| `StudentRepository.java` | `findById(Long)` becomes `findById(Integer)` (since PK changes from surrogate BIGINT to user_id INT). `findByUser_UserId` can be replaced by `findById`. | Medium |
| `BookingRepository.java` | `findByUser_UserId(Long)` — if booking.user_id now points to student, the join path changes. May need `findByUserId(Long)` using plain column (no longer a navigation from User entity). | Medium |

### 6d. Service files

| File | Change | Risk |
|------|--------|------|
| `StudentService.java` | `updateStudent()` currently sets `student.fullName` and `student.phone`. After refactor, these fields don't exist on Student — must update `Users.FullName` and `Users.Phone` via `UserRepository` instead. | High |
| `BookingService.java` | `createBooking()` loads `User user = userRepository.findById(userId)` and sets it on Booking. After FK retarget, Booking needs a `Student` reference (or a plain `user_id` long). Also: current code does not check that the user has STUDENT role — add that check. | Medium |

### 6e. DTO files

| File | Change | Risk | JSON change? |
|------|--------|------|-------------|
| `StudentResponseDTO.java` | `id` field should become `userId` (or keep as `id` but source it from `user_id`). Remove `fullName` and `phone` fields from the Student-specific DTO (they come from Users now). Or keep them projected from the User join — simpler for the frontend. | Medium | Yes — field name `id` stays but value might differ if currently surrogate |
| `StudentUpdateDTO.java` | `fullName` and `phone` can remain but the service must route them to `Users` instead of `student`. | Low | No |
| `TripResponseDTO.java` | Add `driverUserId` and optionally `driverName` (joined from driver+Users). | Low | Yes — additive |

### 6f. Frontend files

| File | Change | Risk | Notes |
|------|--------|------|-------|
| `frontend/js/profile.js` | `student.fullName` and `student.phone` — if DTOs continue to project these from Users, no change. If DTOs stop providing them, fetch from `/users/{id}` instead. | Low (if DTOs kept) | |
| `frontend/js/api.js` | `updateStudent(id, data)` — if `id` changes from surrogate to user_id, the `id` passed in is already `getUserId()` → `user_id`, so this may already be correct. Verify. | Low | |
| `frontend/js/bookings.js` | Displays `b.pickupLocId` (ID only, not name). After refactor no change unless DTO changes. | None | |
| `frontend/js/book.js` | Calls `createBooking({ tripId, pickupLocId, dropoffLocId })`. No userId in body — user is identified by JWT. No change needed. | None | |

### 6g. Seed / script files

| File | Change | Risk |
|------|--------|------|
| `V4__seed_data.sql` | No student rows inserted here (admin only). No change. | None |
| `scripts/dev-seed-test-data.sql` | `student` inserts currently supply `full_name` and `phone` columns. Remove those two columns from the INSERT. | Low |

---

## 7. Open Questions

1. **`student_index` nullability**: The proposed schema makes it nullable so a user can register as STUDENT before receiving their index number. Is that correct, or should `student_index` remain NOT NULL and registration always require it?

2. **`student.semester`**: What does `semester` represent — the current enrolled semester (1–8) at registration time? Will it be kept up to date each term, or is it a one-time registration field?

3. **`booking.user_id` FK target**: Changing from `Users` to `student` enforces at DB level that only students can book. This is the right design goal, but it means `BookingService` must load `Student` (or at minimum verify the user has STUDENT role before booking). Confirm this is acceptable.

4. **Driver-Trip assignment**: Should `bustrip.driver_user_id` be `NOT NULL` (every trip must have a driver before scheduling) or `NULL` (assigned later)? How should the driver portal display "my trips"?

5. **Invoice owner**: Is `invoice` a per-student monthly travel bill, a fleet operating cost invoice, or a supplier invoice? The answer determines whether a `user_id` FK (pointing to student or finance officer) should be added to `invoice`.

6. **`Crash_Incident.user_id` FK retarget**: Changing the FK to reference `driver(user_id)` enforces that crash reporters must be registered drivers. Currently the seed script inserts crash incidents with `@uPep` and `@uNimal` who are both drivers, so data is consistent. Confirm this constraint is always desired (no admin-reported crashes?).

7. **`BusRoute` / `Destination` and `bustrip` disconnect**: These two tables are populated but never joined to trips. Should `bustrip` gain a `route_id FK -> BusRoute(RouteId)` to replace or supplement the `PickupLocationId`/`DropLocationId` pair? Or are they a separate "route catalogue" not linked to individual scheduled trips?
