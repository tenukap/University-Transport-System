// Book a Trip page (ports BookTrip.jsx).

function formatTime(t) {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${period}`;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

let trips = [];

const tripSelect = document.getElementById('trip-select');
const dateInput = document.getElementById('travel-date');
const bookBtn = document.getElementById('book-btn');
const errorEl = document.getElementById('book-error');

dateInput.value = today();

async function loadTrips() {
  try {
    trips = await getTrips();
    if (!trips.length) {
      tripSelect.innerHTML = '<option>No trips available</option>';
      return;
    }
    tripSelect.innerHTML = trips
      .map((t) => `<option value="${t.tripId}">${t.pickupLocationName} → ${t.dropLocationName} • ${formatTime(t.startTime)}</option>`)
      .join('');
  } catch {
    errorEl.textContent = 'Failed to load trips';
    tripSelect.innerHTML = '<option>Failed to load</option>';
  }
}

async function handleBook() {
  const selectedTrip = trips.find((t) => String(t.tripId) === String(tripSelect.value));
  if (!selectedTrip) {
    alert('Please select a trip route.');
    return;
  }

  bookBtn.disabled = true;
  bookBtn.textContent = 'Booking...';
  try {
    await createBooking({
      studentId: getStudentIdFromToken(),
      tripId: selectedTrip.tripId,
      pickupLocId: selectedTrip.pickupLocationId,
      dropoffLocId: selectedTrip.dropLocationId,
    });
    alert('Booking confirmed! Seat assigned.');
  } catch (err) {
    alert(err.message || 'Booking failed');
  } finally {
    bookBtn.disabled = false;
    bookBtn.textContent = 'Book Now';
  }
}

bookBtn.addEventListener('click', handleBook);
loadTrips();
