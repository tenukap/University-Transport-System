import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// --- Mock data (kept commented in case we need to revert) ---
// const mockLocations = [
//   { locationId: 1, locationName: 'SLIIT', lat: 6.9147, lng: 79.9729 },
//   { locationId: 2, locationName: 'Kaduwela', lat: 6.9350, lng: 79.9800 },
//   { locationId: 3, locationName: 'Malabe', lat: 6.9097, lng: 79.9635 },
//   { locationId: 4, locationName: 'Colombo Fort', lat: 6.9355, lng: 79.8487 },
//   { locationId: 5, locationName: 'Nugegoda', lat: 6.8728, lng: 79.8993 },
//   { locationId: 6, locationName: 'Maharagama', lat: 6.8478, lng: 79.9256 },
//   { locationId: 7, locationName: 'Kottawa', lat: 6.8408, lng: 79.9719 },
//   { locationId: 8, locationName: 'Pannipitiya', lat: 6.8611, lng: 79.9436 },
//   { locationId: 9, locationName: 'Battaramulla', lat: 6.9071, lng: 79.9196 },
//   { locationId: 10, locationName: 'Rajagiriya', lat: 6.9108, lng: 79.8878 },
// ];

// const mockTrips = [
//   { tripId: 1, tripDate: '2026-09-20', startTime: '07:00', eta: '08:15', pickupLocationId: 2, dropLocationId: 1, pickupLocationName: 'Kaduwela', dropLocationName: 'SLIIT' },
//   { tripId: 2, tripDate: '2026-09-20', startTime: '08:00', eta: '09:00', pickupLocationId: 3, dropLocationId: 1, pickupLocationName: 'Malabe', dropLocationName: 'SLIIT' },
//   { tripId: 3, tripDate: '2026-09-21', startTime: '07:30', eta: '08:45', pickupLocationId: 4, dropLocationId: 1, pickupLocationName: 'Colombo Fort', dropLocationName: 'SLIIT' },
//   { tripId: 4, tripDate: '2026-09-20', startTime: '07:00', eta: '08:00', pickupLocationId: 5, dropLocationId: 1, pickupLocationName: 'Nugegoda', dropLocationName: 'SLIIT' },
//   { tripId: 5, tripDate: '2026-09-20', startTime: '07:00', eta: '08:10', pickupLocationId: 6, dropLocationId: 1, pickupLocationName: 'Maharagama', dropLocationName: 'SLIIT' },
//   { tripId: 6, tripDate: '2026-09-20', startTime: '07:00', eta: '08:05', pickupLocationId: 7, dropLocationId: 1, pickupLocationName: 'Kottawa', dropLocationName: 'SLIIT' },
//   { tripId: 7, tripDate: '2026-09-20', startTime: '07:00', eta: '07:55', pickupLocationId: 8, dropLocationId: 1, pickupLocationName: 'Pannipitiya', dropLocationName: 'SLIIT' },
//   { tripId: 8, tripDate: '2026-09-20', startTime: '07:00', eta: '07:45', pickupLocationId: 9, dropLocationId: 1, pickupLocationName: 'Battaramulla', dropLocationName: 'SLIIT' },
//   { tripId: 9, tripDate: '2026-09-20', startTime: '07:00', eta: '07:40', pickupLocationId: 10, dropLocationId: 1, pickupLocationName: 'Rajagiriya', dropLocationName: 'SLIIT' },
// ];

// GET /trips - real API call (returns full axios response; use .data at the call site)
export const getTrips = () => api.get('/trips');

// GET /locations - real API call
export const getLocations = () => api.get('/locations');

// GET /locations/{id}
export const getLocationById = (id) => api.get(`/locations/${id}`);

// POST /bookings  body: { studentId, tripId, pickupLocId, dropoffLocId }
export const createBooking = async (data) => {
  const res = await api.post('/bookings', data);
  return res.data;
};

// GET /bookings/student/{studentId}
export const getBookingsByStudent = async (studentId) => {
  const res = await api.get(`/bookings/student/${studentId}`);
  return res.data;
};

// GET /bookings/{id}
export const getBookingById = async (id) => {
  const res = await api.get(`/bookings/${id}`);
  return res.data;
};

// PUT /bookings/{id}/cancel
export const cancelBooking = async (id) => {
  const res = await api.put(`/bookings/${id}/cancel`);
  return res.data;
};

// GET /students/{id}
export const getStudentById = async (id) => {
  const res = await api.get(`/students/${id}`);
  return res.data;
};

// PUT /students/{id}  body: { fullName, phone }
export const updateStudent = async (id, data) => {
  const res = await api.put(`/students/${id}`, data);
  return res.data;
};

export default api;
