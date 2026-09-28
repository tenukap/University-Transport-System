// Live Trip Map page (ports MapView.jsx) using plain Leaflet from CDN.

const SLIIT = [6.9147, 79.9729];

// Point Leaflet's default marker icons at the CDN images.
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom dropoff marker (SLIIT) in secondary rose colour.
const dropoffIcon = L.divIcon({
  className: '',
  html: '<div style="width:14px;height:14px;background:#B46258;border-radius:50%;border:2px solid white;box-shadow:0 2px 4px rgba(0,0,0,0.3)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

let trips = [];
let locations = [];
let map = null;
let layerGroup = null;

const selectEl = document.getElementById('trip-select');
const infoEl = document.getElementById('trip-info');

function formatTime(t) {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${period}`;
}

function getCoords(locationId) {
  const loc = locations.find((l) => l.locationId === locationId);
  return loc ? [parseFloat(loc.latitude), parseFloat(loc.longitude)] : null;
}

function getLocationName(locationId) {
  const loc = locations.find((l) => l.locationId === locationId);
  return loc ? loc.locationName : 'Unknown';
}

function initMap() {
  map = L.map('map').setView(SLIIT, 13);
  // OpenStreetMap tiles — fine when served over HTTP (not file://).
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors',
  }).addTo(map);
  layerGroup = L.layerGroup().addTo(map);
}

function drawMarkers() {
  layerGroup.clearLayers();
  const selected = trips.find((t) => t.tripId === parseInt(selectEl.value, 10));

  if (selected) {
    const pickupCoords = getCoords(selected.pickupLocationId);
    const dropoffCoords = getCoords(selected.dropLocationId);
    if (pickupCoords && dropoffCoords) {
      L.marker(pickupCoords)
        .bindPopup(`<strong>${getLocationName(selected.pickupLocationId)}</strong><br/>Pickup Stop`)
        .addTo(layerGroup);
      L.marker(dropoffCoords, { icon: dropoffIcon })
        .bindPopup(`${getLocationName(selected.dropLocationId)} - Dropoff`)
        .addTo(layerGroup);
      L.polyline([pickupCoords, dropoffCoords], { color: '#35627A', weight: 4, dashArray: '8 4' })
        .addTo(layerGroup);
    }
  } else {
    // Default state: show all pickup location markers.
    trips.forEach((trip) => {
      const coords = getCoords(trip.pickupLocationId);
      if (!coords) return;
      L.marker(coords)
        .bindPopup(`<strong>${getLocationName(trip.pickupLocationId)}</strong><br/>Pickup Stop • ${formatTime(trip.startTime)}`)
        .addTo(layerGroup);
    });
  }

  renderInfo(selected);
}

function renderInfo(selected) {
  if (!selected) {
    infoEl.className = 'trip-info trip-info--empty';
    infoEl.textContent = 'Select a trip above to see the route';
    return;
  }

  infoEl.className = 'trip-info';
  const etaPart = selected.eta
    ? ` &nbsp;•&nbsp; ETA: <strong>${formatTime(selected.eta)}</strong>`
    : '';
  infoEl.innerHTML = `
    <div class="trip-info__route">${getLocationName(selected.pickupLocationId)} → ${getLocationName(selected.dropLocationId)}</div>
    <div class="trip-info__meta">Departure: <strong>${formatTime(selected.startTime)}</strong>${etaPart}</div>
    <div class="trip-info__seats">9 seats left</div>
    <button class="btn btn--primary" id="book-this-trip">Book This Trip</button>`;
  document.getElementById('book-this-trip').addEventListener('click', () => {
    window.location.href = 'book.html';
  });
}

function populateSelect() {
  selectEl.innerHTML =
    '<option value="">All trips</option>' +
    trips.map((trip) =>
      `<option value="${trip.tripId}">${getLocationName(trip.pickupLocationId)} → SLIIT • ${trip.startTime}</option>`
    ).join('');
}

async function loadData() {
  try {
    [trips, locations] = await Promise.all([getTrips(), getLocations()]);
    document.getElementById('map-loading').classList.add('hidden');
    document.getElementById('map-wrapper').classList.remove('hidden');
    populateSelect();
    initMap();
    drawMarkers();
    selectEl.addEventListener('change', drawMarkers);
  } catch (err) {
    console.error('Failed to load map data', err);
    document.getElementById('map-loading').textContent = 'Failed to load map data';
  }
}

loadData();
