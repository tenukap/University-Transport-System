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
