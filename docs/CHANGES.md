# Change Log

## Prompt 12 — Finance review screen and admin finance view

### Why
The finance officer needed a proper interface to review student bank slip submissions (approve/reject), and the admin needed a read-only finance overview. The old manual invoice create/delete flow was obsolete now that invoices are generated automatically.

### What changed

**Part A — finance-officer.html Payments tab**
Rebuilt the Payments table with 7 columns: Student, Invoice, Amount, Date, Status, Reviewed by/on, Actions. Status shows semantic text: "Pending review", "Approved" (slip + PAID), "Paid" (manual + PAID), "Rejected" (slip + FAILED), "Failed" (manual + FAILED). Rejection reason shown as small text under the badge. PENDING slip submissions are sorted to the top. A count badge on the Payments sidebar item shows the number of pending reviews. Filter buttons: All / Pending review / Approved / Rejected. Slip payments get "View slip" (Bearer-token blob URL), "Approve" (confirm dialog → PUT /approve), "Reject" (modal with reason textarea + live char count → PUT /reject). Manual payments keep Edit and Delete unchanged. Success/error banner after each action.

**Part B — finance-officer.html Invoices tab**
Removed "+ New Invoice" button, the new-invoice modal, and all preview/create JS (obsolete — invoices are now auto-created by the backend). Removed Delete action. Updated empty state to "No invoices yet. They are created automatically when students book trips." Edit due date remains.

**Part C — backend: removed unused manual endpoints**
Removed from `FinanceController` and `FinanceService`: `GET /api/finance/students`, `GET /api/finance/invoices/preview`, `POST /api/finance/invoices`, `DELETE /api/finance/invoices/{id}`, and private helpers `requireStudentExists`, `getBillableBookings`. Deleted unused DTOs `FinanceStudentDTO`, `InvoicePreviewDTO`, `CreateInvoiceRequest`. The invoice sync, PUT due-date, and all payment endpoints are unchanged.

**Part D — admin-dashboard.html Finance tab**
New "💰 Finance" sidebar item. Read-only panel with two stacked tables: Invoices (student, period, due, total, paid, balance, status) and Payments (student, invoice, amount, date, status text, View slip). Lazy-loaded on first tab click. No approve/reject — those belong to the finance officer.

### Endpoints removed
| Method | Path | Reason |
|--------|------|--------|
| `GET`  | `/api/finance/students` | Only served the new-invoice dropdown, which is removed |
| `GET`  | `/api/finance/invoices/preview` | Only served the new-invoice preview, which is removed |
| `POST` | `/api/finance/invoices` | Replaced by automatic sync on read |
| `DELETE` | `/api/finance/invoices/{id}` | Manual deletion obsolete with auto-sync |

### Files changed
- `frontend/finance-officer.html`
- `frontend/admin-dashboard.html`
- `src/…/controller/FinanceController.java`
- `src/…/service/FinanceService.java`
- `src/…/dto/FinanceStudentDTO.java` *(deleted)*
- `src/…/dto/InvoicePreviewDTO.java` *(deleted)*
- `src/…/dto/CreateInvoiceRequest.java` *(deleted)*
- `docs/CHANGES.md` *(this file)*

---

## Prompt 11 — Student invoices page

### Why
Students needed a self-service view to see their monthly invoices and pay by uploading a bank slip. The backend endpoints were already in place from Prompt 10; this adds the frontend.

### What changed

**`frontend/invoices.html`** *(new)*
New student page at `invoices.html`, same structure as `bookings.html`.

**`frontend/js/invoices.js`** *(new)*
Loads `GET /api/student/invoices`, renders a table (Period, Due, Total, Paid, Pending review, Balance, Status badge, Pay / History actions). Overdue rows have a red tint. Pay modal: client-side file extension and 5 MB size check before upload, submits via `FormData` + `requestFormData`, shows backend error messages inline. History modal: shows each submission with status (Pending review / Approved / Rejected), rejection reason, and a "View slip" button that fetches the file as a blob and opens an object URL in a new tab (revoked after 60 s so a plain `<a href>` which cannot send the Bearer token is not used). Empty state when no invoices exist yet.

**`frontend/js/api.js`** — added `requestFormData(path, formData)` (POST multipart without `Content-Type` header so the browser sets the multipart boundary), and wrappers `getMyInvoices()` and `submitSlip(invoiceId, formData)`.

**`frontend/js/layout.js`** — added `{ page: 'invoices', … }` to `NAV_ITEMS` and `invoiceIcon()` SVG. This adds the "Invoices" link to the sidebar on every student page that uses the shared layout automatically.

**`frontend/js/profile.js`** — added a "View invoices →" link in the Monthly Travel Charges card header.

**`frontend/css/styles.css`** — added invoice table, badge, modal-field, history-row and button styles.

### Files changed
- `frontend/invoices.html` *(new)*
- `frontend/js/invoices.js` *(new)*
- `frontend/js/api.js`
- `frontend/js/layout.js`
- `frontend/js/profile.js`
- `frontend/css/styles.css`
- `docs/CHANGES.md` *(this file)*

---

## Prompt 10 — Auto invoices and bank slip payments (backend)

### Why
Previously, invoices had to be created manually by the finance officer. This prompt makes them automatic: every time an invoice list is read (finance or student view), the system syncs invoices from the student's actual booking data. Students can now pay by uploading a scanned bank slip; a finance officer then approves or rejects each submission.

### What changed

**New Flyway migration — `V8__payment_slip_and_invoice_unique.sql`**
Additive only; no `flyway:clean` needed. Adds six nullable columns to `payment`: `slip_file_name`, `slip_original_name`, `submitted_by_user_id`, `reviewed_by_user_id`, `reviewed_at`, `review_note`. Also adds a filtered unique index on `invoice(student_user_id, billing_month, billing_year) WHERE student_user_id IS NOT NULL` (manually created invoices with NULL student are excluded).

**Auto-sync (`FinanceService.syncStudentInvoices` / `syncAllStudentInvoices`)**
On every read of the invoice list the service groups non-CANCELLED bookings by billing month, then creates, updates, or deletes invoices accordingly. New invoices get `due_date = last day of billing month + 14 days` (clamped to today when the month is in the past, to satisfy the existing DB check constraint). Finance-officer-edited due dates are never overwritten. An invoice with zero bookings remaining is deleted unless it already has payments, in which case the total is set to zero (treated as a credit).

**New and changed endpoints:**

| Method | Path | Roles | Purpose |
|--------|------|-------|---------|
| `GET`  | `/api/student/invoices` | STUDENT | Own invoices (synced) with embedded payment history |
| `POST` | `/api/student/invoices/{id}/payments` | STUDENT | Upload a bank slip — creates a PENDING payment |
| `GET`  | `/api/slips/{paymentId}` | authenticated | Stream the slip file; student may only fetch their own |
| `PUT`  | `/api/finance/payments/{id}/approve` | FINANCE_OFFICER, ADMIN | Mark PENDING slip PAID (overpayment guard re-runs) |
| `PUT`  | `/api/finance/payments/{id}/reject` | FINANCE_OFFICER, ADMIN | Mark PENDING slip FAILED with a rejection reason |

Existing `PUT /api/finance/payments/{id}` and `DELETE /api/finance/payments/{id}` now return 409 if the payment has a slip (use approve/reject instead).
Existing `GET /api/finance/payments` DTO extended with: `studentName`, `hasSlip`, `submittedByName`, `reviewedByName`, `reviewedAt`, `reviewNote`.

**Ethical considerations**
Slip files contain sensitive bank account details. Mitigations: files are stored outside the web root in a configurable directory (`./uploads/slips` by default), never served as static resources, always streamed with access control (owner, finance officer or admin only), the uploads folder is excluded from version control, and file type (content type AND extension) plus size (≤ 5 MB) are validated before storage.

### Files changed
- `src/…/db/migration/V8__payment_slip_and_invoice_unique.sql` *(new)*
- `src/…/entity/Payment.java` — 6 new fields
- `src/…/dto/PaymentListDTO.java` — 6 new fields
- `src/…/dto/StudentInvoiceDTO.java` *(new)*
- `src/…/dto/StudentPaymentDTO.java` *(new)*
- `src/…/dto/ReviewDecisionRequest.java` *(new)*
- `src/…/repository/BookingRepository.java` — `findByUser_UserIdAndStatusNot`
- `src/…/repository/InvoiceRepository.java` — `findByStudentUserIdOrderByInvoiceIdDesc`
- `src/…/service/SlipStorageService.java` *(new)*
- `src/…/service/FinanceService.java` — sync logic, student methods, approve/reject, inject UserRepository + SlipStorageService
- `src/…/controller/FinanceController.java` — approve/reject endpoints
- `src/…/controller/StudentPortalController.java` *(new, maps `/api/student`)*
- `src/…/controller/SlipController.java` *(new, maps `/api/slips`)*
- `src/…/config/SecurityConfig.java` — `/api/student/**` and `/api/slips/**` rules
- `src/main/resources/application.properties` — `app.slip-dir`, multipart limits
- `.gitignore` — `uploads/`
- `docs/CHANGES.md` *(this file)*

---

## Prompt 8 — Finance Officer: Invoices and Payments tabs

### Why
The Invoices and Payments panels previously linked out to Thymeleaf MVC pages at `/invoices` and `/payments`. Those pages rely on session-cookie auth and do not carry a Bearer token, so they are incompatible with the stateless JWT setup used by every other dashboard. Replaced them with real tabbed UIs inside `finance-officer.html`, backed by new REST endpoints that accept a JWT just like the rest of the app. Invoices are generated from actual student bookings rather than entered manually.

### What changed

**New backend endpoints (FINANCE_OFFICER + ADMIN):**

| Method | Path | Purpose |
|--------|------|---------|
| `GET`  | `/api/finance/students` | List all students (id, full name, student index) for the dropdown |
| `GET`  | `/api/finance/invoices/preview?studentId=&month=&year=` | Preview billable booking count and total for a student/month — uses the same shared method as create so they cannot disagree |
| `GET`  | `/api/finance/invoices` | List all invoices, newest first, with computed status (PAID / OVERDUE / PENDING), amountPaid and balance |
| `POST` | `/api/finance/invoices` | Create invoice from bookings; server sets issueDate=today and totalAmount from bookings; returns 400 for no billable bookings or past due date; 409 for duplicate student+month+year |
| `PUT`  | `/api/finance/invoices/{id}` | Update due date only (must not be before issueDate) |
| `DELETE` | `/api/finance/invoices/{id}` | Delete invoice; 409 if any payment exists |
| `GET`  | `/api/finance/payments` | List all payments, newest first |
| `POST` | `/api/finance/payments` | Record payment (PAID/PENDING/FAILED only; CANCELLED not settable here); 409 if PAID amount would exceed invoice total |
| `PUT`  | `/api/finance/payments/{id}` | Edit payment; same overpayment guard excluding the payment itself |
| `DELETE` | `/api/finance/payments/{id}` | Delete payment; 409 if status is PAID |

**SecurityConfig:** added one rule `.requestMatchers("/api/finance/**").hasAnyRole("FINANCE_OFFICER","ADMIN")` before `.anyRequest()`.

**New backend files:**
- `src/…/dto/FinanceStudentDTO.java`, `InvoicePreviewDTO.java`, `InvoiceListDTO.java`, `PaymentListDTO.java` (response records)
- `src/…/dto/CreateInvoiceRequest.java`, `UpdateInvoiceDueDateRequest.java`, `CreatePaymentRequest.java`, `UpdatePaymentRequest.java` (request POJOs)
- `src/…/service/FinanceService.java`
- `src/…/controller/FinanceController.java`

**Modified backend files:**
- `src/…/repository/InvoiceRepository.java` — added `findAllByOrderByInvoiceIdDesc` and `findByStudentUserIdAndBillingMonthAndBillingYear`
- `src/…/repository/PaymentRepository.java` — added `findAllByOrderByPaymentIdDesc` and `findByInvoice_InvoiceId`
- `src/…/config/SecurityConfig.java` — added `/api/finance/**` rule

**Frontend — `frontend/finance-officer.html`:**
- Added CSS for table-wrap, badges, modals, link-btn, preview-box.
- Replaced Invoices panel (was an external-link card with a "separate sign-in" warning) with a live table: Student, Period, Issued, Due, Total, Paid, Status badge, Edit/Delete actions. New Invoice modal with student dropdown, month/year selects, due date, and live preview that shows billable booking count and total; Save is disabled when count is 0.
- Replaced Payments panel similarly with a live table: Invoice label, Amount, Date, Status badge, Edit/Delete actions. Record Payment modal with invoice dropdown filtered to unpaid invoices, amount defaulting to the selected invoice's balance, payment date, and status select.
- Financial Summary tab is unchanged.
- Old MVC controllers (`InvoiceController`, `PaymentController`) and their Thymeleaf templates are untouched and still exist; they are simply no longer reachable from the dashboard.

---

## Prompt 6 — Admin Buses Tab + Admin Dashboard Audit

### Why
The admin had no way to manage buses from the dashboard; `buses.html` was a separate, standalone page reachable only by URL. The admin dashboard also had several incorrect or misleading numbers and missing data in the Trips, Bookings and Reports tabs.

### What changed

**New backend endpoints (all ADMIN-only):**

| Method | Path | Purpose |
|--------|------|---------|
| `PUT` | `/api/buses/{id}` | Edit registration and/or capacity (capacity guard: cannot go below active booking count on upcoming trips → 409) |
| `PATCH` | `/api/buses/{id}/status` | Set status to Available / Maintenance / Out of Service (upcoming-trip guard → 409) |

Existing `POST /api/buses/add` now validates uniqueness and capacity range (1–100) and returns 409 on duplicate registration instead of a raw DB error. `DELETE /api/buses/delete/{id}` now returns 409 if the bus has any trip history.

**Dashboard counter fixes (FleetTrackService):**
- *Active Bookings*: was `countByStatus("PENDING")` → fixed to `countByStatus("CONFIRMED")`.
- *Upcoming Trips*: was all Scheduled trips regardless of date → fixed to non-cancelled trips with `tripDate >= today`.

**Financial report fix:** `grossRevenue` was summing fares for ALL bookings including CANCELLED → fixed to exclude CANCELLED bookings.

**Bookings DTO:** added `studentName` field so the admin Bookings tab can show names instead of raw user IDs.

**Frontend — `admin-dashboard.html`:**
- New **Buses** sidebar tab between Trips and Bookings: table with Registration, Capacity, Status badge, Edit / Status / Delete actions; Add Bus modal with frontend + backend validation; error banner shows 409 messages.
- **Trips** tab: switched API call from `GET /api/trips` to `GET /api/transport/trips` (admin already has access) to get bus and driver info; added Bus, Driver, Status columns; sorted newest-first.
- **Bookings** tab: student name shown instead of raw user ID (deactivated students still visible); trip label shows pickup → dropoff + date + time.
- **Users** tab: Actions column given `min-width: 160px` and `white-space: nowrap` so the Deactivate button is not clipped at 1280px.
- **Reports** tab: removed non-functional Route and Group filter dropdowns (backend never used them); updated label to "Confirmed Revenue".
- Added `badge--warn` CSS class (amber) for Maintenance bus status.

**Frontend — `buses.html`:** replaced with a meta-refresh redirect to `admin-dashboard.html`.

### Files changed
- `src/…/repository/BusRepository.java`
- `src/…/repository/BusTripRepository.java`
- `src/…/service/BusService.java`
- `src/…/controller/BusController.java`
- `src/…/service/FleetTrackService.java`
- `src/…/dto/BookingResponseDTO.java`
- `src/…/service/BookingService.java`
- `frontend/admin-dashboard.html`
- `frontend/buses.html`
- `docs/CHANGES.md` (this file)

---

## Prompt 9 — Cleanups: booking date picker, routes removed, deactivated token check

### Part A — booking date picker removed
The `<input type="date">` on `book.html` was cosmetic (book.js never sent its value to the server) and misled students into thinking they could pick any date. Replaced with a read-only text line that shows the selected trip's actual date and departure time, populated from the trip data already loaded in the dropdown.

### Part B — Routes feature removed
The BusRoute entity and table have no FK link to trips or bookings, making the Routes tab useless in the transport officer dashboard. Removed the sidebar tab, panel, modal, all JS functions (`loadRoutes`, `openEditRoute`, `deleteRoute`, `saveRoute`), the Overview counter card, and the four route CRUD API endpoints (`POST/GET/PUT/DELETE /api/transport/routes`) from TransportController. Removed the matching service methods from RouteService; `scheduleTrip()` is kept because it is still called by trip creation. The BusRoute entity, BusRouteRepository and DB table are unchanged.

### Part C — deactivated user token check
JwtAuthenticationFilter previously only validated the JWT signature and did not check whether the account was still active. A one-query-per-request check was added: after parsing the JWT, the filter loads the user row and returns 401 `{"error":"This account has been deactivated"}` if `AccountStatus` is not `Active`, stopping the filter chain before any endpoint logic runs.

### Files changed
- `frontend/book.html`
- `frontend/js/book.js`
- `frontend/transport-officer-dashboard.html`
- `src/…/controller/TransportController.java`
- `src/…/service/RouteService.java`
- `src/…/security/JwtAuthenticationFilter.java`
- `docs/CHANGES.md` (this file)

---

## Prompt 14 — Driver trips and endpoint security

Added a driver trips list endpoint and locked the four previously unprotected endpoint groups.

### Part A — GET /api/driver/my-trips
New `DriverPortalController` returns only the logged-in driver's upcoming non-cancelled trips (date ≥ today), ordered by date and start time. Each row includes tripId, tripDate, startTime, eta, pickup name, drop name, bus registration, tripStatus, and latestStatusType (most recent Trip_Status log, nullable). Driver identity always comes from the JWT principal, never from a query parameter. New `DriverTripDTO` carries these fields.

### Part B — Security and ownership
Replaced the single `permitAll()` rule for the four groups with role-specific matchers (specific before general, first match wins):

| Endpoint group | POST | PUT | DELETE | GET |
|---|---|---|---|---|
| /api/trip-statuses/** | DRIVER | authenticated | authenticated | authenticated (DRIVER filtered) |
| /api/location-updates/** | DRIVER | authenticated | authenticated | authenticated (DRIVER filtered) |
| /api/emergency-reports/** | DRIVER | TRANSPORT_OFFICER | TRANSPORT_OFFICER | authenticated (DRIVER filtered) |
| /api/crash-incidents/** | DRIVER | TRANSPORT_OFFICER | TRANSPORT_OFFICER | authenticated (DRIVER filtered) |
| /api/driver/** | DRIVER | — | — | DRIVER |

POST /api/trip-statuses and POST /api/location-updates: validate that the tripId is assigned to the logged-in driver (403 if not), that the trip is not Cancelled, and that statusType is one of: Scheduled, Departed, In Progress, Delayed, At Stop, Completed (400 otherwise). Latitude must be −90..90 and longitude −180..180 (400 otherwise). Logging "Completed" also sets BusTrip.tripStatus = "Completed" so the trip leaves the student booking dropdown. POST /api/emergency-reports: reporter id set from principal; validates title (≤255), type (≤100), description (≤2000). POST /api/crash-incidents: driverUserId set from principal; timestamp = LocalDateTime.now() so Time is never NULL; validates location, severity, description. GET on all four groups: DRIVER sees only their own rows; TRANSPORT_OFFICER and ADMIN see all. PUT/DELETE on emergency-reports and crash-incidents restricted to TRANSPORT_OFFICER (frontend does not call these from driver.html or any student page; emergency.html is a legacy prototype not linked from any portal).

### Student decision — emergency.html
`emergency.html` is not linked from any student-facing page and uses raw fetch calls without auth headers (legacy prototype). POST /api/emergency-reports is therefore DRIVER only. STUDENT role can still GET their own reports in case historical data exists.

### Part C — driver.html
Replaced the typed Trip ID inputs on Trip Status and Location Updates forms with dropdowns populated from GET /api/driver/my-trips (option text: "Pickup → Drop | DD Mon HH:MM | REG"). Empty state disables the submit buttons and shows "No trips assigned to you yet." "Use My GPS" continues to work. Client-side lat/lon range validation matches the server-side rules. All four tables have loading, empty, and error states; crash incident Time column now shows a real value.

### Files changed
- `src/…/config/SecurityConfig.java`
- `src/…/controller/DriverPortalController.java` (new)
- `src/…/controller/TripStatusController.java`
- `src/…/controller/LocationUpdateController.java`
- `src/…/controller/EmergencyReportController.java`
- `src/…/controller/CrashIncidentController.java`
- `src/…/service/TripStatusService.java`
- `src/…/service/LocationUpdateService.java`
- `src/…/service/EmergencyReportService.java`
- `src/…/service/CrashIncidentService.java`
- `src/…/repository/BusTripRepository.java`
- `src/…/repository/TripStatusRepository.java`
- `src/…/repository/LocationUpdateRepository.java`
- `src/…/repository/EmergencyReportRepository.java`
- `src/…/repository/CrashIncidentRepository.java`
- `src/…/dto/DriverTripDTO.java` (new)
- `frontend/driver.html`
- `docs/CHANGES.md` (this file)
