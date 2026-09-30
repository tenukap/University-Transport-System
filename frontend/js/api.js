// API layer - native fetch wrappers.
// Base URL comes from config.js; every request carries the JWT via getAuthHeaders().

// Shared HTML-escape helper (used by every file that builds innerHTML).
function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// Core request helper: sets JSON + Authorization headers, checks status, parses body.
// Throws an Error whose message is the backend's response text so callers can surface it.
// 401 → session expired; redirect to login.
// 403 → ownership or role error; throw so the caller can display the message.
// 204 → returns null (no body to parse).
async function request(path, { method = 'GET', body } = {}) {
  const options = {
    method,
    headers: getAuthHeaders(), // { Content-Type, Authorization: Bearer <token> }
  };
  if (body !== undefined) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, options);

  // Session expired → back to login.
  if (res.status === 401) {
    localStorage.clear();
    window.location.replace('login.html');
    throw new Error('Your session has expired. Please log in again.');
  }

  const text = await res.text();

  if (!res.ok) {
    // Surface the backend message (JSON { message/error } or plain text).
    let msg = text;
    try {
      const j = text ? JSON.parse(text) : null;
      if (j && (j.message || j.error)) msg = j.message || j.error;
    } catch (_) { /* plain text — use as-is */ }
    throw new Error(msg || `Request failed (${res.status})`);
  }

  // 204 No Content or genuinely empty body.
  return text ? JSON.parse(text) : null;
}

// GET /trips  (admin use — all trips regardless of status/date)
const getTrips = () => request('/trips');

// GET /trips/available  (student booking page — Scheduled, future trips only)
const getAvailableTrips = () => request('/trips/available');

// GET /trips/{tripId}/seats  -> { capacity, booked, available }
const getTripSeats = (tripId) => request(`/trips/${tripId}/seats`);

// GET /locations
const getLocations = () => request('/locations');

// GET /locations/{id}
const getLocationById = (id) => request(`/locations/${id}`);

// POST /bookings  body: { tripId, pickupLocId, dropoffLocId }  (server assigns seat + fare)
const createBooking = (data) => request('/bookings', { method: 'POST', body: data });

// GET /bookings/user/{userId}
const getBookingsByUser = (userId) => request(`/bookings/user/${userId}`);

// GET /bookings/{id}
const getBookingById = (id) => request(`/bookings/${id}`);

// PUT /bookings/{id}/cancel  (soft cancel — seat released)
const cancelBooking = (id) => request(`/bookings/${id}/cancel`, { method: 'PUT' });

// DELETE /bookings/{id}  (hard delete — returns 204)
const deleteBooking = (id) => request(`/bookings/${id}`, { method: 'DELETE' });

// GET /bookings/charges?month=M&year=Y  (authenticated student only)
const getMyCharges = (month, year) => {
  const params = new URLSearchParams();
  if (month != null) params.set('month', month);
  if (year  != null) params.set('year',  year);
  const qs = params.toString();
  return request(`/bookings/charges${qs ? '?' + qs : ''}`);
};

// GET /students/{id}
const getStudentById = (id) => request(`/students/${id}`);

// GET /students/user/{userId}
const getStudentByUserId = (userId) => request(`/students/user/${userId}`);

// PUT /students/{id}  body: { semester?, studentIndex? }
const updateStudent = (id, data) => request(`/students/${id}`, { method: 'PUT', body: data });

// PUT /users/{id}  body: { fullName, phone }  (student self-service)
const updateUser = (id, data) => request(`/users/${id}`, { method: 'PUT', body: data });
