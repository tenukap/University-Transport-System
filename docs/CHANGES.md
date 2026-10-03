# Change Log

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
