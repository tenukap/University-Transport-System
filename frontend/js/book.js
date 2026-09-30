// Book a Trip page.

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
const dateInput  = document.getElementById('travel-date');
const bookBtn    = document.getElementById('book-btn');
const errorEl    = document.getElementById('book-error');
const successEl  = document.getElementById('book-success');
const seatHint   = document.getElementById('seat-hint');

dateInput.value = today();

// Fetch seat availability for the currently selected trip and update the hint.
async function updateSeatHint() {
  const tripId = tripSelect.value;
  if (!tripId || !trips.length) {
    seatHint.textContent = 'Available seats: —';
    bookBtn.disabled = false;
    return;
  }
  seatHint.textContent = 'Checking seats…';
  try {
    const seats = await getTripSeats(tripId);
    if (seats.available === 0) {
      seatHint.textContent = 'Fully booked';
      bookBtn.disabled = true;
    } else {
      seatHint.textContent = `Available seats: ${seats.available} of ${seats.capacity}`;
      bookBtn.disabled = false;
    }
  } catch (_) {
    seatHint.textContent = 'Available seats: unknown';
    bookBtn.disabled = false;
  }
}

async function loadTrips() {
  try {
    trips = await getAvailableTrips();
    if (!trips.length) {
      tripSelect.innerHTML = '<option>No trips available</option>';
      seatHint.textContent = 'No trips available';
      bookBtn.disabled = true;
      return;
    }
    tripSelect.innerHTML = trips
      .map((t) => {
        const pickup  = escapeHtml(t.pickupLocationName  || '—');
        const dropoff = escapeHtml(t.dropLocationName    || '—');
        const time    = escapeHtml(formatTime(t.startTime));
        return `<option value="${escapeHtml(t.tripId)}">${pickup} → ${dropoff} • ${time}</option>`;
      })
      .join('');
    await updateSeatHint();
  } catch (err) {
    errorEl.textContent = err.message || 'Failed to load trips';
    tripSelect.innerHTML = '<option>Failed to load</option>';
    seatHint.textContent = '';
  }
}

async function handleBook() {
  const selectedTrip = trips.find((t) => String(t.tripId) === String(tripSelect.value));
  if (!selectedTrip) {
    errorEl.textContent = 'Please select a trip route.';
    return;
  }

  bookBtn.disabled = true;
  bookBtn.textContent = 'Booking...';
  errorEl.textContent = '';
  successEl.classList.add('hidden');

  try {
    const booking = await createBooking({
      tripId:       selectedTrip.tripId,
      pickupLocId:  selectedTrip.pickupLocationId,
      dropoffLocId: selectedTrip.dropLocationId,
      // seatNumber and fareAmount are set server-side; do not send them.
    });
    const seat = booking && booking.seatNumber != null ? `Seat ${booking.seatNumber}.` : '';
    const fare = booking && booking.fareAmount  != null
      ? ` Fare: LKR ${Number(booking.fareAmount).toFixed(2)}.` : '';
    successEl.textContent = `Booking confirmed! ${seat}${fare} Redirecting to My Bookings…`;
    successEl.classList.remove('hidden');
    await updateSeatHint();
    setTimeout(() => { window.location.href = 'bookings.html'; }, 2000);
  } catch (err) {
    errorEl.textContent = err.message || 'Booking failed';
    await updateSeatHint();
    bookBtn.disabled = false;
    bookBtn.textContent = 'Book Now';
  }
}

tripSelect.addEventListener('change', updateSeatHint);
bookBtn.addEventListener('click', handleBook);
loadTrips();
