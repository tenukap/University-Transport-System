// API layer - native fetch wrappers (ports src/api/api.js from the old React app).
// Base URL and STUDENT_ID come from config.js.

// Core request helper: sets JSON headers, checks status, parses body.
// Throws an Error whose message is the backend's response text so callers
// can surface it (mirrors the old axios err.response.data handling).
async function request(path, { method = 'GET', body } = {}) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (body !== undefined) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, options);
  const text = await res.text();

  if (!res.ok) {
    throw new Error(text || `Request failed (${res.status})`);
  }

  // Some endpoints may return an empty body.
  return text ? JSON.parse(text) : null;
}

// GET /trips
const getTrips = () => request('/trips');

// GET /locations
const getLocations = () => request('/locations');

// GET /locations/{id}
const getLocationById = (id) => request(`/locations/${id}`);

// POST /bookings  body: { studentId, tripId, pickupLocId, dropoffLocId }
const createBooking = (data) => request('/bookings', { method: 'POST', body: data });

// GET /bookings/student/{studentId}
const getBookingsByStudent = (studentId) => request(`/bookings/student/${studentId}`);

// GET /bookings/{id}
const getBookingById = (id) => request(`/bookings/${id}`);

// PUT /bookings/{id}/cancel
const cancelBooking = (id) => request(`/bookings/${id}/cancel`, { method: 'PUT' });

// GET /students/{id}
const getStudentById = (id) => request(`/students/${id}`);

// PUT /students/{id}  body: { fullName, phone }
const updateStudent = (id, data) => request(`/students/${id}`, { method: 'PUT', body: data });
