# Transport Officer — Data Entry Order

**Project:** SLIIT SE2030 University Transport Management System  
**Generated:** 2026-10-03  
**Scope:** Read-only code analysis (no app was started; no DB was touched)

---

## A. Ordered Steps Table

> Steps 0a–0c are performed by the **ADMIN** role and are hard prerequisites.  
> Steps 1–3 are performed by the **TRANSPORT OFFICER**.  
> Steps 4–6 are what other roles do after the transport officer is done.

| Step | Entity | Screen / Page in `frontend/` | API Endpoint | Required Fields | Depends On | Notes |
|------|--------|------------------------------|--------------|-----------------|------------|-------|
| 0a | `Users` (role=DRIVER) | `admin-dashboard.html` → Users tab | `POST /api/users` | fullName, email, password, roleName=`DRIVER` | — | ADMIN only; no driver profile auto-created (see Gap G1) |
| 0b | `driver` profile | **NO FRONTEND** (gap) | `POST /api/drivers/add` | userId (must match step 0a), license_number | Step 0a | ADMIN only; needed for `driver_user_id` FK on `bustrip` (see Gap G1) |
| 0c | `bus` | `buses.html` → "Deploy New Shuttle" form | `POST /api/buses/add` | registrationNumber, passengerCapacity | — | ADMIN only; transport officer references `bus_id` by number |
| 1 | `location` (pickup) | `transport-officer-dashboard.html` → Locations tab → "+ New Location" | `POST /api/transport/locations` | locationName | — | lat/lng are optional but required for GPS auto-ping on trip creation |
| 2 | `location` (drop) | same screen as Step 1 | `POST /api/transport/locations` | locationName | — | Must be a separate row from Step 1 |
| 3 | `BusRoute` (optional) | `transport-officer-dashboard.html` → Routes tab → "+ New Route" | `POST /api/transport/routes` | routeName, startPoint, endPoint | — | **Routes are NOT linked to trips via FK**; this step is cosmetic metadata only (see Gap G5) |
| 4 | `bustrip` | `transport-officer-dashboard.html` → Trips tab → "+ Schedule Trip" | `POST /api/transport/trips` | bus.busId, tripDate, startTime, eta, pickupLocation.locationId, dropLocation.locationId | Steps 0c, 1, 2 | `TripStatus` auto-set to `"Scheduled"`; no driver assignment UI (see Gap G3) |
| 5 | `booking` (student side) | `book.html` (student portal) | `POST /api/bookings` | tripId, pickupLocId, dropoffLocId | Step 4 trip must be `Scheduled` and not departed | seatNumber and fareAmount assigned server-side |
| 6 | `invoice` + `payment` (finance side) | `finance-officer.html` → Invoices / Payments tabs → Thymeleaf links | `POST /invoices/save`, `POST /payments/save` | See Section B | Step 5 bookings needed for financial summary | Thymeleaf MVC, separate auth session (see Gap G4) |

---

## B. Validation Rules (per step)

### Step 0a — Create DRIVER user (`POST /api/users`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Frontend (JS) | "Min 6 characters" placeholder on password field — **no actual JS enforcement** | `admin-dashboard.html:213` |
| Backend service | Email uniqueness: `userRepository.findByEmail(request.email()).isPresent()` → throws `"Email already in use"` | `UserService.java:53` |
| Backend service | `roleName` uppercased; no role whitelist validation | `UserService.java:64` |
| Backend service | Password stored as **plaintext** (TODO comment in code) | `UserService.java:68` |
| Schema | `Email VARCHAR(150) NOT NULL UNIQUE`, `PasswordHash VARCHAR(255) NOT NULL`, `RoleName VARCHAR(50) NOT NULL` | `V1__users_and_roles.sql:12–23` |

### Step 0b — Create driver profile (`POST /api/drivers/add`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Backend service | None — `driverRepository.save(driver)` directly | `DriverService.java:15` |
| Entity | `license_number` mapped `nullable=false, unique=true` | `Driver.java:12` |
| Schema | `user_id INT NOT NULL` FK → `Users(UserId)`, `license_number NVARCHAR(100) NOT NULL UNIQUE`, `status` defaults to `'Available'` | `V1__users_and_roles.sql:56–67` |

### Step 0c — Create bus (`POST /api/buses/add`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Frontend (JS) | Both `registrationNumber` and `capacity` inputs have `required` attribute; submit blocked by HTML validation | `buses.html:83–91` |
| Backend service | None — `busRepository.save(bus)` directly | `BusService.java:15` |
| Schema | `registration_number VARCHAR(50) NOT NULL UNIQUE`, `passenger_capacity INT NOT NULL`, `status` defaults to `'Available'` | `V2__fleet_and_locations.sql:25–31` |

### Step 1/2 — Create location (`POST /api/transport/locations`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Frontend (JS) | `if (!name)` → "Location name is required." | `transport-officer-dashboard.html:628` |
| Backend controller | No validation — `locationRepo.save(location)` directly | `TransportController.java:153` |
| Schema | `LocationName VARCHAR(100) NOT NULL`; `Latitude DECIMAL(9,6) NULL`, `Longitude DECIMAL(9,6) NULL` | `V2__fleet_and_locations.sql:11–17` |

> **Schema vs code disagreement:** The schema marks `LocationName` NOT NULL, but there is no backend service-level check. Passing `null` as `locationName` will cause a JDBC constraint violation at the DB layer, not a clean 400 response.

### Step 3 — Create route (`POST /api/transport/routes`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Frontend (JS) | `if (!name \|\| !start \|\| !end)` → "All fields are required." | `transport-officer-dashboard.html:579` |
| Backend service | Duplicate name check: `routeRepo.findByRouteName(route.getRouteName()).isPresent()` → throws `"Duplicate Route Found!"` | `RouteService.java:25` |
| Schema | `RouteName NVARCHAR(100) NOT NULL`, `StartPoint NVARCHAR(100) NOT NULL`, `EndPoint NVARCHAR(100) NOT NULL` | `V2__fleet_and_locations.sql:39–46` |

### Step 4 — Schedule trip (`POST /api/transport/trips`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Frontend (JS) | `if (!busId \|\| !date \|\| !start \|\| !eta \|\| !pickId \|\| !dropId)` → "All fields are required." | `transport-officer-dashboard.html:601` |
| Backend service | Sets `trip.setTripStatus("Scheduled")` unconditionally | `RouteService.java:49` |
| Backend service | Auto-creates a `Location_Update` ping using pickup lat/lng (falls back to `6.9271, 79.8612`) | `RouteService.java:51–64` |
| Backend — no check | Bus existence validated by FK only; if bus_id does not exist → DB constraint error, not a clean 400 | `V3__trips_and_bookings.sql:23` |
| Schema | `bus_id BIGINT NOT NULL`, `TripDate DATE NOT NULL`, `StartTime TIME NOT NULL`, `ETA TIME NOT NULL`; `driver_user_id INT NULL` (nullable — trip can be saved without a driver) | `V3__trips_and_bookings.sql:11–32` |

### Step 5 — Student booking (`POST /api/bookings`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Backend service | Caller must have a row in `student` table | `BookingService.java:55` |
| Backend service | Trip `TripStatus` must equal `"Scheduled"` (case-insensitive) | `BookingService.java:67` |
| Backend service | Trip must not have departed (tripDate + startTime vs `LocalDate.now()`) | `BookingService.java:73–80` |
| Backend service | Student cannot book the same trip twice | `BookingService.java:85–89` |
| Backend service | `bus` must not be null | `BookingService.java:91–94` |
| Backend service | A free seat must be available (capacity minus active bookings) | `BookingService.java:103–111` |
| Backend service | Fare is **server-side only** from `booking.fare` property (default `150.00 LKR`) | `BookingService.java:50`, `application.properties` |
| DB index | `UQ_booking_trip_seat_active` prevents two active bookings with same seat on same trip | `V3__trips_and_bookings.sql:87–90` |

### Step 6 — Invoice validation (`POST /invoices/save`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Service | `billingMonth` must be 1–12 | `InvoiceService.java:47` |
| Service | `billingYear` ≥ 2020 | `InvoiceService.java:51` |
| Service | `issueDate` not null | `InvoiceService.java:66` |
| Service | `dueDate` not null; `dueDate >= issueDate` | `InvoiceService.java:71–85` |
| Service | `totalAmount` not null, ≥ 0, ≤ 99999999.99, max 2 decimal places | `InvoiceService.java:88–103` |
| Schema | `chk_invoice_month`, `chk_invoice_year`, `chk_invoice_amount`, `chk_invoice_dates` CHECK constraints | `V4__finance.sql:19–26` |

### Step 6b — Payment validation (`POST /payments/save`)

| Layer | Rule | File:Line |
|-------|------|-----------|
| Service | Valid invoice_id required; invoice must exist | `PaymentService.java:60`, `PaymentService.java:87` |
| Service | `amount > 0`, ≤ 99999999.99, max 2 decimal places | `PaymentService.java:64–75` |
| Service | `paymentDate` not in future | `PaymentService.java:77` |
| Service | `paymentStatus` must be `PAID`, `PENDING`, or `FAILED` (not `CANCELLED` — that is set only via cancel API) | `PaymentService.java:80–88` |
| Schema | `chk_payment_amount CHECK (amount > 0)`, `chk_payment_status CHECK (...)` | `V4__finance.sql:46–53` |

---

## C. Sample Data (enter in this order)

All values are realistic Sri Lankan / SLIIT-area data that pass every validation found in the code.

### Step 0a — Create Transport Officer user (via admin-dashboard.html)

| Field | Value |
|-------|-------|
| Full Name | `Lasantha Wickramasinghe` |
| Email | `lasantha.to@sliit.lk` |
| Role | `TRANSPORT_OFFICER` |
| Password | `sliit2026` |

### Step 0a (second call) — Create Driver user

| Field | Value |
|-------|-------|
| Full Name | `Nuwan Bandara` |
| Email | `nuwan.driver@sliit.lk` |
| Role | `DRIVER` |
| Password | `driver2026` |

### Step 0b — Create driver profile (call via REST client, e.g. Postman, because no UI exists)

```json
POST /api/drivers/add
Authorization: Bearer <admin-token>
{
  "userId": <UserId returned in step 0a for Nuwan>,
  "licenseNumber": "B1234567",
  "dob": "1988-03-15",
  "status": "Available"
}
```

### Step 0c — Create bus (via buses.html as ADMIN)

| Field | Value |
|-------|-------|
| Registration Number | `NC-5678` |
| Passenger Capacity | `40` |

Note the returned `bus_id` (e.g. `1`) — you will type it manually when scheduling the trip.

### Step 1 — Create Pickup Location

| Field | Value |
|-------|-------|
| Location Name | `SLIIT Main Entrance` |
| Latitude | `6.914722` |
| Longitude | `79.972222` |

### Step 2 — Create Drop Location

| Field | Value |
|-------|-------|
| Location Name | `Malabe Junction` |
| Latitude | `6.904722` |
| Longitude | `79.959722` |

(Optional third location for variety)

| Location Name | `Katubedda Gate` |
|---|---|
| Latitude | `6.860000` |
| Longitude | `79.892222` |

### Step 3 — Create Route (optional, cosmetic)

| Field | Value |
|-------|-------|
| Route Name | `SLIIT Main — Malabe Junction` |
| Start Point | `SLIIT Main Entrance` |
| End Point | `Malabe Junction` |

### Step 4 — Schedule Trip

| Field | Value |
|-------|-------|
| Bus ID | `1` (the ID returned in Step 0c) |
| Trip Date | `2026-10-10` |
| Start Time | `07:30` |
| ETA | `08:15` |
| Pickup Location | select `SLIIT Main Entrance` (from Step 1) |
| Drop Location | select `Malabe Junction` (from Step 2) |

After saving, note the returned `TripId` (e.g. `1`). The driver will need this number.

### Step 5 — Student booking (for testing — logged in as a STUDENT user)

The student sees the trip in the `book.html` dropdown as `"SLIIT Main Entrance → Malabe Junction • 07:30 AM"`.

| Field | Value (auto-set) |
|-------|-----------------|
| Trip | selected from dropdown |
| Seat | assigned server-side (first available: 1) |
| Fare | `LKR 150.00` (from `booking.fare` property) |

### Step 6 — Invoice (Finance Officer, via /invoices)

| Field | Value |
|-------|-------|
| Billing Month | `10` |
| Billing Year | `2026` |
| Issue Date | `2026-10-01` |
| Due Date | `2026-10-31` |
| Total Amount | `150.00` |

### Step 6b — Payment (Finance Officer, via /payments)

| Field | Value |
|-------|-------|
| Invoice | select invoice from Step 6 |
| Amount | `150.00` |
| Payment Date | `2026-10-10` |
| Status | `PAID` |

---

## D. What Each Role Needs to See Anything

### DRIVER (`driver.html`)

The driver's endpoints (`/api/emergency-reports`, `/api/crash-incidents`, `/api/trip-statuses`, `/api/location-updates`) are **`permitAll()`** — no JWT is required at the HTTP level.

| What driver needs | Provided by | Notes |
|---|---|---|
| A `Users` row with `RoleName=DRIVER` | Admin (Step 0a) | Required to log in and be redirected to `driver.html` |
| A `driver` row in the `driver` table | Admin (Step 0b) | **No admin UI exists for this**; without it the driver cannot be assigned to a trip via FK |
| Trip ID to log status updates | Transport Officer (Step 4) | Driver types the trip ID manually in `driver.html`; there is no trip list shown to the driver |
| Trip to already exist in `bustrip` | Transport Officer (Step 4) | `POST /api/trip-statuses` has FK → `bustrip(TripId)` — posting to a non-existent trip ID fails with a constraint error |

**Visibility blocker:** The driver has no way to discover which trip they are assigned to — the driver portal shows no trip list and the trip scheduling modal has no driver field. The transport officer must communicate the trip ID out-of-band (e.g. verbally or via message).

### STUDENT (`book.html`, `bookings.html`)

| What student needs | Provided by | Notes |
|---|---|---|
| A `Users` row with `RoleName=STUDENT` | Admin | Auto-creates a `student` row via `UserService` |
| At least one trip with `TripStatus='Scheduled'` AND future date+time | Transport Officer (Step 4) | `TripService.getAvailableTrips()` filters to Scheduled + not departed |
| A `bus` assigned to the trip (`bus_id` not null) | Admin (Step 0c) + Transport Officer references it | `BookingService` throws if `bus == null` |
| A free seat (`capacity > active bookings`) | Step 0c capacity field | Capacity is set at bus creation; cannot be changed via UI |

**Visibility blocker:** If the trip's `TripStatus` is anything other than `"Scheduled"` (e.g. `"Cancelled"`, `"Completed"`, `"In Progress"`), the trip does **not** appear in `GET /api/trips/available`. A trip completed by the transport officer before the student books will be invisible.

### FINANCE OFFICER (`finance-officer.html`, `/invoices`, `/payments`)

| What finance officer needs | Provided by | Notes |
|---|---|---|
| `Users` row with `RoleName=FINANCE_OFFICER` | Admin | No profile table; just the `Users` row is enough for login |
| Bookings to exist for Financial Summary | Students (Step 5) | `GET /api/reports/financial` queries `booking` table |
| An invoice to exist before creating a payment | Finance Officer (Step 6) | `payment.invoice_id FK → invoice.invoice_id` |

**Critical visibility blocker — Thymeleaf auth gap (see Gap G4):** Clicking "Open Invoice Management →" navigates the browser to `/invoices`. This is a plain HTML GET request without an `Authorization: Bearer` header (JWT is stored in `localStorage`, not sent automatically on `<a href>` navigation). The `JwtAuthenticationFilter` finds no token; the `HttpStatusEntryPoint` returns **HTTP 401**. The finance officer sees an error page, not an invoice form. The invoice/payment Thymeleaf system is **unreachable through normal browser navigation** as configured.

---

## E. Gaps and Suspected Bugs

### G1 — No driver profile creation UI (CRITICAL for demo)

**What:** `UserService.createUser()` auto-creates a `student` row for STUDENT-role users (`UserService.java:72–79`) but does **nothing** for DRIVER-role users. The `driver` table row (with `license_number NOT NULL`) must be created separately via `POST /api/drivers/add`. No admin frontend page exists for this call; the only page that links to `driver.html` (`buses.html:59`) goes to the **driver's own portal** (`checkAuth('DRIVER')`), not to a driver management form.

**Effect:** A DRIVER user can log in, but the `driver` table has no row for them. Any trip that tries to reference `driver_user_id` via FK will fail. This is partially hidden because the trip modal has no driver field anyway (see G3).

**Severity:** HIGH for demo — driver portal works (permitAll endpoints), but any database-level driver assignment is impossible without a Postman call.

**How to confirm manually:** Create a user with role=DRIVER via admin-dashboard. Then check the database: `SELECT * FROM driver WHERE user_id = <new_user_id>` — should be empty.

---

### G2 — Transport officer cannot browse available buses (HIGH for demo)

**What:** `BusController` is `@PreAuthorize("hasRole('ADMIN')")` (`BusController.java:13`). The trip scheduling modal uses a plain `<input type="number" id="tm-busid">` (`transport-officer-dashboard.html:203`). The transport officer must type a bus ID they do not know.

**Effect:** On a fresh database the transport officer has no way to know what bus IDs exist without being told out-of-band or logging in as admin.

**Severity:** HIGH for demo — the officer will get a DB FK error if they type a wrong ID.

**How to confirm:** Log in as transport officer, open Trips tab, click "+ Schedule Trip" — the Bus ID field is a blank number input with no dropdown.

---

### G3 — No UI to assign driver to a trip (HIGH for demo)

**What:** `BusTrip.driverUserId` (`BusTrip.java:53`) is stored as a nullable `INT` column. The trip scheduling modal (`transport-officer-dashboard.html:199–218`) has no field for it. `RouteService.scheduleTrip()` does not set it. The `PUT /api/transport/trips/{id}` handler (`TransportController.java:71–83`) only copies date/time/location/status/cost — not `driverUserId`.

**Effect:** All trips are created and updated with `driver_user_id = NULL`. Drivers cannot be assigned to trips through any UI path.

**Severity:** HIGH for demo — the driver portal tab "Trip Status" asks the driver to type a trip ID, but there is no mechanism linking a trip to a driver.

**How to confirm:** Schedule a trip, then `SELECT driver_user_id FROM bustrip WHERE TripId = <id>` — will be NULL.

---

### G4 — Thymeleaf invoice/payment pages unreachable from the browser (CRITICAL for demo)

**What:** `InvoiceController` (`InvoiceController.java:24`) and `PaymentController` (`PaymentController.java:25`) are Spring MVC `@Controller` classes serving Thymeleaf templates at `/invoices` and `/payments`. These paths are not in the security permit list and fall under `.anyRequest().authenticated()` (`SecurityConfig.java:50`). The authentication entry point is `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` (`SecurityConfig.java:32`), and the session policy is `STATELESS` (`SecurityConfig.java:9`). There is no `.formLogin()` configured.

When a finance officer clicks `<a href="/invoices">`, the browser sends a plain GET without an `Authorization` header (JWT is in `localStorage`, not sent on `<a>` navigation). The server returns `401`. No Thymeleaf page is ever rendered.

**Effect:** Finance officer **cannot access invoice or payment management** through the browser at all.

**Severity:** CRITICAL for demo — the entire finance workflow for invoices/payments is broken.

**How to confirm:** Log in as finance officer, click "Open Invoice Management →", observe the browser response is HTTP 401.

---

### G5 — BusRoute is not linked to BusTrip (MEDIUM)

**What:** The `BusRoute` table and `Destination` table exist in V2. `BusTrip` has no `route_id` column or FK. `TransportController` exposes full route CRUD, but routes have no effect on trip creation, booking, or any downstream query.

**Effect:** Routes entered in Step 3 are effectively decorative. The student never sees route names — `TripResponseDTO` (`TripResponseDTO.java`) shows only pickup/drop location names.

**Severity:** MEDIUM — wastes demo time if you enter routes believing they are needed.

**How to confirm:** Create a route, schedule a trip — the trip has no reference to the route anywhere in the response.

---

### G6 — Plaintext passwords throughout (LOW — dev only, not a runtime crash)

**What:** `V7__seed.sql:9` stores `'admin1234'` as a plain string. `UserService.java:68` stores the submitted password without encoding (`// TODO(security): plaintext for dev only`). `PortalController.java:53` compares with `password.equals(user.getPasswordHash())`.

**Effect:** Passwords visible in the database. BCrypt encoder is wired in `SecurityConfig.java:21` but never used by the portal login path.

**Severity:** LOW for demo (it works), HIGH for any real use.

**How to confirm:** Query `SELECT PasswordHash FROM Users` — passwords are readable.

---

### G7 — No bus status update endpoint (LOW)

**What:** `Bus` entity has a `status` column (`Bus.java:28`, default `'Available'`). `BusController` has no `PUT` endpoint. Once a bus is deployed, its status can never be changed to `'Maintenance'` or `'Out of Service'` via the API.

**Effect:** `BookingService` does not check bus status — it only checks whether a bus is assigned at all. A bus in maintenance would still accept bookings.

**Severity:** LOW for demo.

---

### G8 — Trip cancel sends reason as query param, not body (LOW)

**What:** `TransportController.cancelTrip()` accepts `@RequestParam String reason` (`TransportController.java:85`). The frontend encodes it: `apiFetch(\`/transport/trips/${id}/cancel?reason=${encodeURIComponent(reason)}\`, { method: 'PUT' })` (`transport-officer-dashboard.html:659`). Reasons with special characters (e.g. `&`) could be truncated by some proxies.

**Severity:** LOW for demo.

---

### G9 — `buses.html` sidebar "Transit Operators" link goes to driver portal (MEDIUM)

**What:** `buses.html:59` `<a href="driver.html">👤 Transit Operators</a>`. `driver.html:235` calls `checkAuth('DRIVER')` and redirects non-drivers back to login. An admin following the sidebar link from buses.html will be bounced to `login.html`.

**Severity:** MEDIUM — causes confusion. The label implies admin driver management, not the driver's own portal.

---

### G10 — Finance summary endpoint accessible to Finance Officer but invoice/payment pages are not (MEDIUM)

**What:** `SecurityConfig.java:43` allows `FINANCE_OFFICER` to call `/api/reports/financial`. The `finance-officer.html` Financial Summary tab works fine via JWT. But the Invoices and Payments tabs (which link to Thymeleaf) do not work (see G4). The Finance Officer has a half-functional dashboard.

---

## F. Manual Test Checklist (per step)

### Step 0a–0c (Admin prerequisite)

- [ ] Try creating a user with a duplicate email — expect "Email already in use" error
- [ ] Try creating two buses with the same registration number — expect DB unique constraint error (no clean error message)
- [ ] Create a DRIVER user, then verify via Postman that `GET /api/drivers/all` returns an empty list (no driver profile auto-created)
- [ ] Delete a bus that is referenced by a trip — expect FK constraint error from DB

### Step 1–2 (Locations)

- [ ] Save a location with a blank name — frontend should block with "Location name is required."
- [ ] Save a location with no lat/lng — should succeed (nullable fields)
- [ ] Save two locations with the same name — should succeed (no uniqueness constraint on LocationName)
- [ ] Delete a location that is the pickup of an existing trip — expect FK violation from DB (no frontend handling — user sees raw error)

### Step 3 (Routes)

- [ ] Save a route with blank start or end — frontend blocks with "All fields are required."
- [ ] Save two routes with the same name — expect "Duplicate Route Found!" error shown in modal
- [ ] Delete a route that has Destination rows — expect FK violation

### Step 4 (Schedule Trip)

- [ ] Enter a bus ID that does not exist — expect a DB FK error (no clean message in the modal, just HTTP 500 or a raw exception string)
- [ ] Enter a pickup and drop location that are the same — should succeed (no check for this)
- [ ] Schedule a trip for today's date in the past time — it will save, but will NOT appear in `GET /api/trips/available` because `TripService.getAvailableTrips()` filters out departed trips
- [ ] Complete a trip, then try to cancel it — backend returns the trip as-is; complete/cancel UI hides the buttons for Completed/Cancelled status
- [ ] Complete a trip that has active bookings — status changes to "Completed" without cancelling bookings; bookings remain CONFIRMED

### Step 5 (Student Booking)

- [ ] Book twice on the same trip as the same student — expect "You already have a booking for this trip"
- [ ] Book on a trip with status Cancelled or Completed — expect "Trip is not available for booking"
- [ ] Book on a fully-booked trip (all 40 seats taken) — expect HTTP 409 "This trip is fully booked"
- [ ] Cancel a CONFIRMED booking — should succeed and free the seat
- [ ] Delete a booking for a trip that has already departed — expect HTTP 409

### Step 6 (Finance — Invoice)

- [ ] Set `billingMonth=0` or `billingMonth=13` — expect "Billing month must be between 1 and 12"
- [ ] Set `dueDate` before `issueDate` — expect "Due date cannot be before issue date"
- [ ] Set `totalAmount=-1` — expect "Total amount cannot be negative"
- [ ] Try to delete an invoice that has a payment — expect "InvoiceDeletionBlockedException"
- [ ] Try to access `/invoices` directly in the browser as finance officer — expect HTTP 401 (see Gap G4)

---

*All file:line references above were verified by direct reading of source files during this analysis. No application was started and no database was queried.*
