// API layer - native fetch wrappers (ports src/api/api.js from the old React app).
// Base URL comes from config.js; every request carries the JWT via getAuthHeaders().

// Core request helper: sets JSON + Authorization headers, checks status, parses body.
// Throws an Error whose message is the backend's response text so callers can surface it.
async function request(path, { method = 'GET', body } = {}) {
  const options = {
    method,
    headers: getAuthHeaders(), // { Content-Type, Authorization: Bearer <token> }
  };
  if (body !== undefined) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, options);

  // Not signed in / not allowed: clear state and return to login.
  if (res.status === 401 || res.status === 403) {
    localStorage.clear();
    window.location.replace('login.html');
    throw new Error('Your session is not authorized');
  }

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

// GET /bookings/user/{userId}
const getBookingsByUser = (userId) => request(`/bookings/user/${userId}`);

// GET /bookings/{id}
const getBookingById = (id) => request(`/bookings/${id}`);

// PUT /bookings/{id}/cancel
const cancelBooking = (id) => request(`/bookings/${id}/cancel`, { method: 'PUT' });

// GET /students/{id}
const getStudentById = (id) => request(`/students/${id}`);

// GET /students/user/{userId}
const getStudentByUserId = (userId) => request(`/students/user/${userId}`);

// PUT /students/{id}  body: { fullName, phone }
const updateStudent = (id, data) => request(`/students/${id}`, { method: 'PUT', body: data });
