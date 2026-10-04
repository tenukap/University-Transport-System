// Student "Track My Bus" page.
// Fetches /api/student/tracking (JWT identity only) and renders booked-trip cards.
// Clicking a card with a known location shows it on a small inline Leaflet map.

L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:       'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:     'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

let trkData       = [];
let trkServerTime = null;
let trkMap        = null;
let trkMapMarker  = null;
let trkSelected   = null;
let trkTimer      = null;

const listEl   = document.getElementById('trk-list');
const mapWrap  = document.getElementById('trk-map-wrap');

async function loadTracking() {
  try {
    const resp = await request('/student/tracking');
    trkServerTime = resp.serverTime;
    trkData = Array.isArray(resp.data) ? resp.data : [];
    render();
  } catch (err) {
    listEl.innerHTML = `<p class="error-text">${escapeHtml(err.message || 'Failed to load tracking data')}</p>`;
  }
}

function render() {
  if (!trkData.length) {
    listEl.innerHTML = '<div class="empty-state"><p>You have no upcoming booked trips.</p></div>';
    mapWrap.style.display = 'none';
    return;
  }

  listEl.innerHTML = trkData.map((t, i) => {
    const statusAge = age(t.latestStatusAt);
    const locAge    = age(t.latestLocationAt);
    const staleS    = stale(t.latestStatusAt);
    const staleL    = stale(t.latestLocationAt);

    const statusBlock = t.latestStatusType
      ? `<span class="badge ${statusCls(t.latestStatusType)}">${escapeHtml(t.latestStatusType)}</span>
         <span class="${staleS ? 'trk-stale' : ''}" style="font-size:11px;">${statusAge || ''}</span>`
      : '<span class="trk-none">No updates yet</span>';

    const locBlock = t.latestLat
      ? `<span class="${staleL ? 'trk-stale' : ''}" style="font-size:12px;">${escapeHtml(t.latestLat)}, ${escapeHtml(t.latestLng)}
           (${locAge || ''})</span>
         <a href="${osmUrl(t.latestLat, t.latestLng)}" target="_blank" rel="noopener"
            style="color:#35627A;font-weight:600;font-size:12px;margin-left:6px;">Map</a>`
      : '<span class="trk-none">No location yet</span>';

    const isSelected = trkSelected === i;
    return `<div class="trk-card${isSelected ? ' is-selected' : ''}" data-idx="${i}">
      <div class="trk-card__header">
        <span class="trk-card__route">${escapeHtml(t.pickupName || '?')} → ${escapeHtml(t.dropName || '?')}</span>
        <span class="badge ${tripStatusCls(t.tripStatus)}">${escapeHtml(t.tripStatus || '—')}</span>
      </div>
      <div class="trk-card__meta">
        ${escapeHtml(t.tripDate || '—')} · Departs ${escapeHtml(t.startTime ? t.startTime.substring(0,5) : '—')}
        ${t.eta ? '· ETA ' + escapeHtml(t.eta.substring(0,5)) : ''}
      </div>
      <div class="trk-card__row">
        <div class="trk-card__item"><span class="trk-card__label">Bus</span><span>${escapeHtml(t.busRegistration || '—')}</span></div>
        <div class="trk-card__item"><span class="trk-card__label">My Seat</span><span>${t.mySeat != null ? escapeHtml(String(t.mySeat)) : '—'}</span></div>
        <div class="trk-card__item"><span class="trk-card__label">Latest Status</span><span>${statusBlock}</span></div>
        <div class="trk-card__item" style="flex:1;min-width:180px;"><span class="trk-card__label">Last Location</span><span>${locBlock}</span></div>
      </div>
    </div>`;
  }).join('');

  listEl.querySelectorAll('.trk-card').forEach(card => {
    card.addEventListener('click', () => selectTrip(parseInt(card.dataset.idx, 10)));
  });

  // Re-select the previously selected card if still valid
  if (trkSelected !== null && trkSelected < trkData.length) showMap(trkSelected);
}

function selectTrip(idx) {
  trkSelected = idx;
  render(); // re-render to update is-selected highlight
}

function showMap(idx) {
  const t = trkData[idx];
  if (!t || !t.latestLat || !t.latestLng) { mapWrap.style.display = 'none'; return; }

  const lat = parseFloat(t.latestLat);
  const lng = parseFloat(t.latestLng);
  if (isNaN(lat) || isNaN(lng)) { mapWrap.style.display = 'none'; return; }

  mapWrap.style.display = '';

  if (!trkMap) {
    trkMap = L.map('trk-map-wrap').setView([lat, lng], 15);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(trkMap);
  } else {
    trkMap.setView([lat, lng], 15);
  }

  if (trkMapMarker) trkMap.removeLayer(trkMapMarker);
  trkMapMarker = L.marker([lat, lng])
    .addTo(trkMap)
    .bindPopup(`<strong>${escapeHtml(t.busRegistration || 'Bus')}</strong><br/>
                ${escapeHtml(t.pickupName || '?')} → ${escapeHtml(t.dropName || '?')}<br/>
                Last seen: ${escapeHtml(age(t.latestLocationAt) || 'just now')}`)
    .openPopup();

  // Leaflet needs a size refresh when the container was hidden
  setTimeout(() => trkMap.invalidateSize(), 100);
}

// ---- helpers ----

function age(dateStr) {
  if (!dateStr || !trkServerTime) return null;
  const d = new Date(trkServerTime) - new Date(dateStr);
  if (isNaN(d) || d < 0) return 'just now';
  const m = Math.floor(d / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return m + ' min ago';
  const h = Math.floor(m / 60);
  return h < 24 ? h + ' h ago' : Math.floor(h / 24) + ' d ago';
}

// True when the timestamp is more than 30 minutes old relative to serverTime.
function stale(dateStr) {
  return dateStr && trkServerTime && (new Date(trkServerTime) - new Date(dateStr)) > 1800000;
}

function statusCls(s) {
  if (!s) return 'badge--pending';
  if (s === 'Completed') return 'badge--confirmed';
  if (['Cancelled','Breakdown'].includes(s)) return 'badge--cancelled';
  return 'badge--pending';
}

function tripStatusCls(s) {
  if (s === 'Completed') return 'badge--confirmed';
  if (s === 'Cancelled') return 'badge--cancelled';
  return 'badge--pending';
}

function osmUrl(lat, lng) {
  const la = encodeURIComponent(lat), lo = encodeURIComponent(lng);
  return `https://www.openstreetmap.org/?mlat=${la}&mlon=${lo}#map=16/${la}/${lo}`;
}

// Auto-refresh every 30 s; timer is managed here (no tab-leave hook needed on a standalone page)
function startRefresh() {
  if (trkTimer) clearInterval(trkTimer);
  trkTimer = setInterval(loadTracking, 30000);
}

// Pause refresh when the user leaves the tab (Page Visibility API)
document.addEventListener('visibilitychange', () => {
  if (document.hidden) { clearInterval(trkTimer); trkTimer = null; }
  else startRefresh();
});

loadTracking();
startRefresh();
