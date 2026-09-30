# DB Analysis — uni_transport_system
_Based on: migrations V1–V7 (current after schema rewrite, 2026-09-30)_

> **Phase 1 — Analysis only. No files have been changed.**

---

## 1. ROLE INVENTORY

| Role | Role-specific columns | Lives in | Has Java entity? |
|---|---|---|---|
| ADMIN | `employee_id`, `department`, `access_level` | `admin` (V1) | Yes — `Admin.java` (new, untracked) |
| STUDENT | `student_index`, `semester` | `student` (V1) | Yes — `Student.java` |
| DRIVER | `license_number`, `dob`, `status` | `driver` (V1) | Yes — `Driver.java` |
| FINANCE_OFFICER | `employee_id`, `approval_limit`, `hire_date` | `finance_officer` (V1) | **No** |
| TRANSPORT_OFFICER | `employee_id`, `depot`, `hire_date` | `transport_officer` (V1) | **No** |

**Verdict on alignment with design goal:** The V1–V7 rewrite already implements the parent/child pattern correctly. Shared columns (`FullName`, `Email`, `PasswordHash`, `Phone`, `RoleName`, `AccountStatus`, `CreatedAt`, `UpdatedAt`) live exclusively in `Users`. No role table duplicates shared columns. All role tables use `user_id` as their sole PK, which is also an FK to `Users(UserId)`.

---

## 2. REDUNDANCY

### 2a. Shared-column duplication
**None.** The old `student.full_name` and `student.phone` columns are gone in V1–V7.

### 2b. Surrogate-id duplication
**None.** Every role table uses `user_id INT NOT NULL PK FK → Users(UserId)`. No separate auto-increment `id` column exists in any role table.

### 2c. Structural issues that remain (not duplicates, but mismatches)

| # | Issue | Where | Impact |
|---|---|---|---|
| 1 | `student.student_index` is `NOT NULL` with a plain UNIQUE constraint | `student` table | `UserService.createUser` must insert a `PENDING-{userId}` placeholder string to satisfy the NOT NULL constraint. This pollutes the unique index with non-real data. Design goal requires NULL + filtered unique index. |
| 2 | `student.semester` has no CHECK constraint | `student` table | Any TINYINT value (0, 9, 255) is accepted. Design goal requires `CHECK (semester BETWEEN 1 AND 8)`. |
| 3 | `Booking.java` navigates to `User`, not `Student` | `Booking.java` entity | DB FK is `booking.user_id → student(user_id)`. Entity has `@ManyToOne private User user`. Works numerically because `student.user_id == Users.UserId`, but any non-student UserId inserted will fail the DB FK at runtime with a cryptic constraint violation. |
| 4 | `BusTrip.java` stores `driver_user_id` as plain `Integer` | `BusTrip.java` entity | No `@ManyToOne` navigation. Hibernate does not enforce the FK at ORM level; only the DB constraint catches violations. Consistent with `Invoice.java` and `CrashIncident.java` (same pattern), but inconsistent with other FK columns that use `@ManyToOne`. |
| 5 | `Feedback.java` field is named `comments` but column is `Message` | `Feedback.java` entity | Cosmetic confusion; `@Column(name="Message")` makes it work at runtime. |
| 6 | `EmergencyReport.java` field is named `studentNo` for `user_id` | `EmergencyReport.java` entity | Any user can file an emergency report; calling the field `studentNo` is misleading and will confuse maintainers. |

---

## 3. FK REVIEW

_Every FK that points directly to `Users(UserId)` or to a role child table._

| child.column | role acting there | current DB target | recommended target | reason | code impact |
|---|---|---|---|---|---|
| `student.user_id` | STUDENT | `Users(UserId)` CASCADE | ✓ correct | PK/FK; child deleted when user deleted | None |
| `driver.user_id` | DRIVER | `Users(UserId)` CASCADE | ✓ correct | PK/FK; correct | None |
| `admin.user_id` | ADMIN | `Users(UserId)` (no CASCADE) | ✓ correct | `Admin.java` deletes child explicitly before parent | None |
| `finance_officer.user_id` | FINANCE_OFFICER | `Users(UserId)` (no CASCADE) | ✓ correct | No entity; manual delete required | Add entity or add CASCADE |
| `transport_officer.user_id` | TRANSPORT_OFFICER | `Users(UserId)` (no CASCADE) | ✓ correct | No entity; manual delete required | Add entity or add CASCADE |
| `booking.user_id` | STUDENT (only students book) | `student(user_id)` | ✓ correct in DB | Only students book; FK is semantically right | **`Booking.java` must change**: `@ManyToOne private Student student` instead of `private User user`; `BookingService` must load `Student` rather than `User`; `BookingResponseDTO.userId` value stays the same |
| `invoice.student_user_id` | STUDENT | `student(user_id)` | ✓ correct | Invoice belongs to a student | None (stored as plain Integer, no navigation) |
| `bustrip.driver_user_id` | DRIVER | `driver(user_id)` | ✓ correct | Trip is driven by a driver | None (stored as plain Integer) |
| `Crash_Incident.driver_user_id` | DRIVER | `driver(user_id)` | ✓ correct | Crash involves a driver | None (stored as plain Integer) |
| `Emergency_Report.user_id` | ANY USER | `Users(UserId)` CASCADE | ✓ keep as Users | Any authenticated user can file a report | Rename Java field `studentNo` → `userId` (cosmetic; JSON key changes from `studentNo` to `userId`) |
| `Feedback.UserId` | ANY USER | `Users(UserId)` CASCADE | ✓ correct | Any authenticated user can give feedback | None |
| `SavedReports.GeneratedByUserId` | ADMIN | `Users(UserId)` CASCADE | Could be `admin(user_id)` to enforce role | Only admins generate saved reports | Table has no Java entity; low priority |
| `Announcements.PostedByUserId` | ADMIN | `Users(UserId)` CASCADE | Keep as Users or change to `admin(user_id)` | Acceptable either way | Table has no Java entity; low priority |
| `AdminAuditLogs.AdminUserId` | ADMIN | `Users(UserId)` CASCADE | Should be `admin(user_id)` | Log entries should be constrained to admin actors | Table has no Java entity; low priority |
| `AdminAuditLogs.TargetUserId` | ANY USER | **No FK constraint declared** | Add `REFERENCES Users(UserId)` | Missing constraint — orphans possible | Add constraint only |

---

## 4. MISSING LINKS

| # | Gap | Severity | Detail |
|---|---|---|---|
| 1 | `bustrip` has no FK to `BusRoute` | Medium | The transport officer manages routes and trips as separate objects. There is no `route_id` on `bustrip`, so you cannot query "which trips run on Route X?" nor display a route name on the trip listing. Add `bustrip.route_id INT NULL FK → BusRoute(RouteId)` if this relationship is needed. |
| 2 | `Destination` table is completely orphaned | High | Defined in V2 with FK to `BusRoute`, but has no Java entity, repository, service, or UI. If named stops within a route are not needed, drop the table. |
| 3 | `student_index` has no registration path | High | `UserService.createUser` sets `student_index = 'PENDING-{userId}'` because `StudentUpdateDTO` only exposes `semester`. There is no API endpoint that accepts and saves the real student index at registration time. The profile page also cannot update it. |
| 4 | Profile page sends `fullName`/`phone` to the wrong endpoint | High | `frontend/js/profile.js` calls `updateStudent(student.id, { fullName, phone })`. `StudentUpdateDTO` only exposes `semester`. The backend silently ignores `fullName` and `phone`. Users cannot update their name or phone through the profile page. Fix: route name/phone updates to `PUT /api/users/{id}`. |
| 5 | `seat_reservation` vs `booking.seat_number` — no sync | Medium | Both tables record a seat number independently. There is no constraint or service logic ensuring they match. Consider whether one is redundant. |
| 6 | `invoice.student_user_id` is nullable — intent unclear | Open question | If every invoice must belong to a student (monthly travel bill), the column should be NOT NULL. If generic/fleet invoices exist, document it. |
| 7 | `finance_officer` and `transport_officer` tables have no Java entities | Low | These roles can authenticate (via `Users`) but their role-specific profile data cannot be read or written through JPA. |

---

## 5. PROPOSED SCHEMA (role tables only)

_Only tables that need DDL changes from the current V1. All others are shown as "unchanged"._

### Users — unchanged
```sql
CREATE TABLE Users (
    UserId        INT IDENTITY NOT NULL,
    FullName      VARCHAR(100) NOT NULL,
    Email         VARCHAR(150) NOT NULL,
    PasswordHash  VARCHAR(255) NOT NULL,
    Phone         VARCHAR(20) NULL,
    RoleName      VARCHAR(50) NOT NULL,
    AccountStatus VARCHAR(20) NULL CONSTRAINT DF_Users_AccountStatus DEFAULT 'Active',
    CreatedAt     DATETIME2 NULL CONSTRAINT DF_Users_CreatedAt DEFAULT GETDATE(),
    UpdatedAt     DATETIME2 NULL CONSTRAINT DF_Users_UpdatedAt DEFAULT GETDATE(),
    CONSTRAINT PK_Users PRIMARY KEY (UserId),
    CONSTRAINT UQ_Users_Email UNIQUE (Email)
);
```

### student — TWO changes required

```sql
CREATE TABLE student (
    user_id       INT NOT NULL,
    student_index VARCHAR(50) NULL,          -- CHANGE: was NOT NULL; drop plain UNIQUE below
    semester      TINYINT NULL
        CONSTRAINT CK_student_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 8),  -- ADD
    CONSTRAINT PK_student PRIMARY KEY (user_id),
    -- No UNIQUE constraint on student_index here; use the filtered index below
    CONSTRAINT FK_student_user FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE
);

-- CHANGE: replaces UQ_student_index (which rejected all NULLs)
CREATE UNIQUE NONCLUSTERED INDEX UQ_student_index_filtered
    ON student(student_index) WHERE student_index IS NOT NULL;
```

**Delta from current:**
1. `student_index VARCHAR(50) NOT NULL` → `VARCHAR(50) NULL`
2. `CONSTRAINT UQ_student_index UNIQUE (student_index)` → dropped; replaced by filtered unique index
3. `semester TINYINT NULL` → add `CHECK (semester IS NULL OR semester BETWEEN 1 AND 8)`

**Effect on other tables/FKs:** none — `student.user_id` PK does not change.

### driver — unchanged
```sql
CREATE TABLE driver (
    user_id        INT NOT NULL,
    license_number NVARCHAR(100) NOT NULL,
    dob            DATE NULL,
    status         NVARCHAR(50) NULL CONSTRAINT DF_driver_status DEFAULT 'Available',
    CONSTRAINT PK_driver PRIMARY KEY (user_id),
    CONSTRAINT UQ_driver_license UNIQUE (license_number),
    CONSTRAINT FK_driver_user FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE
);
```

### admin — unchanged
```sql
CREATE TABLE admin (
    user_id      INT NOT NULL,
    employee_id  VARCHAR(20) NOT NULL,
    department   VARCHAR(100) NULL,
    access_level VARCHAR(20) NOT NULL CONSTRAINT DF_admin_access_level DEFAULT 'STANDARD',
    CONSTRAINT PK_admin PRIMARY KEY (user_id),
    CONSTRAINT UQ_admin_employee_id UNIQUE (employee_id),
    CONSTRAINT CK_admin_access_level CHECK (access_level IN ('STANDARD','SUPER')),
    CONSTRAINT FK_admin_user FOREIGN KEY (user_id) REFERENCES Users(UserId)
    -- no ON DELETE CASCADE intentional: Admin.java deletes child row explicitly
);
```

### finance_officer — unchanged
```sql
CREATE TABLE finance_officer (
    user_id        INT NOT NULL,
    employee_id    VARCHAR(20) NOT NULL,
    approval_limit DECIMAL(10,2) NULL,
    hire_date      DATE NULL,
    CONSTRAINT PK_finance_officer PRIMARY KEY (user_id),
    CONSTRAINT UQ_finance_officer_employee_id UNIQUE (employee_id),
    CONSTRAINT CK_finance_officer_limit CHECK (approval_limit IS NULL OR approval_limit >= 0),
    CONSTRAINT FK_finance_officer_user FOREIGN KEY (user_id) REFERENCES Users(UserId)
);
```

### transport_officer — unchanged
```sql
CREATE TABLE transport_officer (
    user_id     INT NOT NULL,
    employee_id VARCHAR(20) NOT NULL,
    depot       VARCHAR(100) NULL,
    hire_date   DATE NULL,
    CONSTRAINT PK_transport_officer PRIMARY KEY (user_id),
    CONSTRAINT UQ_transport_officer_employee_id UNIQUE (employee_id),
    CONSTRAINT FK_transport_officer_user FOREIGN KEY (user_id) REFERENCES Users(UserId)
);
```

---

## 6. CHANGE LIST

_Applies only if the proposed schema changes from §5 are approved. Files are edited in-place (V1–V7) because the project uses `flyway:clean` before each run._

### Migration files

| File | Change | Risk | Frontend JSON changes? |
|---|---|---|---|
| `V1__users_and_roles.sql` | `student_index NULL`, drop plain `UQ_student_index`, add filtered unique index, add `semester` CHECK | Low (flyway-clean) | No |

### Java entity files

| File | Change | Risk |
|---|---|---|
| `Student.java` | No column constraint to remove, but update any Hibernate `nullable=false` if ever added | Low |
| `Booking.java` | Change `@ManyToOne private User user` → `@ManyToOne private Student student`; update `@JoinColumn` | Medium |
| `EmergencyReport.java` | Rename field `studentNo` → `userId` (cosmetic) | Low — JSON key `studentNo` changes to `userId` |

### Repository files

| File | Change | Risk |
|---|---|---|
| `BookingRepository.java` | `findByUser_UserId(Long)` → `findByStudent_UserId(Long)` (or `findByUserId(Long)` if stored as a plain column) | Low |

### Service files

| File | Change | Risk |
|---|---|---|
| `UserService.java` | Remove `PENDING-{id}` placeholder in `createUser`; `student_index` can now be null at registration | Medium — registration flow changes |
| `BookingService.java` | Load `Student` via `StudentRepository` instead of `User` via `UserRepository` for the booking creator; update entity assignment | Medium |

### DTO files

| File | Change | Risk | JSON change? |
|---|---|---|---|
| `StudentUpdateDTO.java` | Add `String studentIndex` so the student can set their real index post-registration | Low | No |
| `BookingResponseDTO.java` | `userId` field value is unchanged (still the student's UserId); no field name change needed | Low | No |

### Frontend files

| File | Change | Risk |
|---|---|---|
| `frontend/js/profile.js` | `handleSave` must call `PUT /api/users/{userId}` for `fullName`/`phone` and `PUT /api/students/{id}` only for `semester`/`studentIndex` | Medium — currently broken |
| `frontend/js/api.js` | Add `updateUser(id, data)` wrapper for `PUT /users/{id}` | Low |
| `frontend/js/app.js` (both copies) | If `EmergencyReport.studentNo` renamed to `userId`, update `report.studentNo` references | Low |
| `src/main/resources/static/js/app.js` | Same as above | Low |

### Files that do NOT change
- `V2`–`V7` migrations (no change propagates from the student table delta)
- `Driver.java`, `Admin.java`, `Invoice.java`, `CrashIncident.java`, `Payment.java`, `PaymentCancelation.java`, `TripCancellation.java`, `BusTrip.java`
- All DTOs except `StudentUpdateDTO`
- All services except `UserService`, `BookingService`
- All repositories except `BookingRepository`
- `StudentService.java` — no change (already reads `fullName`/`phone` from `user.getFullName()`/`user.getPhone()`)

---

## 7. OPEN QUESTIONS

1. **`student_index` at registration**: If `student_index` becomes nullable, what triggers the student to fill it in? Is it entered at signup (needs to be added to the registration form and `UserRequest`), or is it set later by an admin? This determines whether `StudentUpdateDTO.studentIndex` or a dedicated admin endpoint is the right path.

2. **`semester` meaning**: Does `semester` represent the student's current enrolled semester at time of registration (1–8), or is it updated each term? If the latter, who updates it and when?

3. **`invoice.student_user_id` nullable intent**: Does the system allow invoices not linked to any student (e.g., operational cost invoices)? If no, make the column NOT NULL. If yes, document the distinction.

4. **Drop `Destination` table?**: It has no entity, no service, no UI. If bus stops within a route are modelled solely through the `location` table, `Destination` is dead schema. Drop it to keep migrations clean.

5. **Link `bustrip` to `BusRoute`?**: The transport officer dashboard manages routes and trips independently. Should a scheduled trip carry a `route_id` FK? Needed if you want to answer "which trips run on Route X?" or display a route name on bookings.

6. **`finance_officer` and `transport_officer` entities**: Is it intentional that these roles have no Java entities (authentication only), or should entities be added for future profile editing or role-specific dashboards?

7. **`SavedReports`, `Announcements`, `AdminAuditLogs` (V6)**: These three tables have no Java entities or services. Are they planned features, or dead schema? If dead, drop them.

8. **`admin.user_id` FK has no ON DELETE CASCADE** (unlike `student` and `driver`). `Admin.java` deletes the child row explicitly before the parent. Should the same pattern apply to `finance_officer` and `transport_officer`? Currently neither has an entity, so deletion is unmanaged.

9. **`seat_reservation` vs `booking.seat_number`**: Is `seat_reservation` used anywhere? If not, remove it. If it should stay, add a CHECK or service-level assertion to keep `seat_reservation.seat_number` in sync with `booking.seat_number`.

10. **`EmergencyReport.studentNo` rename**: Renaming to `userId` changes the JSON key name from `studentNo` to `userId`. The frontend in `app.js` reads `report.reportId`, `report.reportTitle`, `report.emergencyType`, `report.description`, `report.resolutionStatus` but does NOT display `studentNo` / `userId` in the current UI — so the rename is safe unless other consumers read that field.
