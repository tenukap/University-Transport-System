// My Bookings page (ports MyBookings.jsx).

const TABS = ['All', 'Pending', 'Confirmed', 'Cancelled'];

let bookings = [];
let activeTab = 'All';
let selected = null;

const tabsEl = document.getElementById('tabs');
const listEl = document.getElementById('bookings-list');
const modalRoot = document.getElementById('modal-root');

function busIconSvg(size = 20, color = '#FFFFFF') {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6v6M16 6v6M2 12h19.6M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v8c0 .4.1.8.2 1.2C2.5 16.3 3 18 3 18h3"/><circle cx="7" cy="18" r="2"/><path d="M9 18h5"/><circle cx="16" cy="18" r="2"/></svg>`;
}

function badge(status) {
  const key = (status || '').toUpperCase();
  const cls = { PENDING: 'badge--pending', CONFIRMED: 'badge--confirmed', CANCELLED: 'badge--cancelled' }[key] || 'badge--pending';
  return `<span class="badge ${cls}">${key || 'UNKNOWN'}</span>`;
}

function formatDate(dt) {
  if (!dt) return '—';
  const d = new Date(dt);
  if (isNaN(d)) return dt;
  return d.toLocaleString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function canCancel(status) {
  return ['PENDING', 'CONFIRMED'].includes((status || '').toUpperCase());
}

function renderTabs() {
  tabsEl.innerHTML = TABS.map((tab) =>
    `<button class="tab${tab === activeTab ? ' is-active' : ''}" data-tab="${tab}">${tab}</button>`
  ).join('');
  tabsEl.querySelectorAll('.tab').forEach((btn) => {
    btn.addEventListener('click', () => { activeTab = btn.dataset.tab; renderTabs(); renderList(); });
  });
}

function renderList() {
  const filtered = bookings.filter((b) =>
    activeTab === 'All' ? true : (b.status || '').toUpperCase() === activeTab.toUpperCase()
  );

  if (filtered.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state__icon">${busIconSvg(48, '#8E9A98')}</div>
        <p>No bookings found</p>
      </div>`;
    return;
  }

  listEl.innerHTML = filtered.map((b) => `
    <div class="booking-card">
      <div class="booking-card__inner">
        <div class="booking-icon">${busIconSvg()}</div>
        <div class="booking-body">
          <div class="booking-row">
            <span class="booking-title">Trip #${b.tripId}</span>
            ${badge(b.status)}
          </div>
          <div class="booking-meta">Pickup Location ID: ${b.pickupLocId} → SLIIT</div>
          <div class="booking-meta--sm">Date booked: ${formatDate(b.createdAt)}</div>
          ${b.seatNumber != null ? `<div class="booking-seat">Seat #${b.seatNumber}</div>` : ''}
          <div class="booking-actions">
            <button class="btn btn--outline" data-view="${b.id}">View Details</button>
          </div>
        </div>
      </div>
    </div>`).join('');

  listEl.querySelectorAll('[data-view]').forEach((btn) => {
    btn.addEventListener('click', () => {
      selected = bookings.find((b) => String(b.id) === btn.dataset.view);
      renderModal();
    });
  });
}

function detailRow(label, value) {
  return `<div class="detail-row"><span class="detail-row__label">${label}</span><span class="detail-row__value">${value}</span></div>`;
}

function renderModal() {
  if (!selected) {
    modalRoot.innerHTML = '';
    return;
  }

  modalRoot.innerHTML = `
    <div class="modal-overlay" id="modal-overlay">
      <div class="modal" id="modal-box">
        <button class="modal__close" id="modal-close" aria-label="Close">✕</button>
        <h2 class="modal__title">Booking #${selected.id}</h2>
        <div class="modal__rows">
          ${detailRow('Trip ID', `#${selected.tripId}`)}
          <div class="detail-row"><span class="detail-row__label">Status</span>${badge(selected.status)}</div>
          ${detailRow('Pickup Location ID', selected.pickupLocId)}
          ${detailRow('Dropoff Location ID', selected.dropoffLocId)}
          ${detailRow('Date Booked', formatDate(selected.createdAt))}
          ${detailRow('Seat Number', selected.seatNumber != null ? `#${selected.seatNumber}` : 'Not assigned')}
        </div>
        ${canCancel(selected.status)
          ? `<button class="btn btn--danger btn--block" id="cancel-booking-btn">Cancel Booking</button>`
          : ''}
      </div>
    </div>`;

  const close = () => { selected = null; renderModal(); };
  document.getElementById('modal-overlay').addEventListener('click', close);
  document.getElementById('modal-box').addEventListener('click', (e) => e.stopPropagation());
  document.getElementById('modal-close').addEventListener('click', close);

  const cancelBtn = document.getElementById('cancel-booking-btn');
  if (cancelBtn) cancelBtn.addEventListener('click', handleCancel);
}

async function handleCancel() {
  if (!selected) return;
  const cancelBtn = document.getElementById('cancel-booking-btn');
  cancelBtn.disabled = true;
  cancelBtn.textContent = 'Cancelling...';
  try {
    await cancelBooking(selected.id);
    selected = null;
    renderModal();
    await loadBookings();
  } catch (err) {
    alert(err.message || 'Cancel failed');
    cancelBtn.disabled = false;
    cancelBtn.textContent = 'Cancel Booking';
  }
}

async function loadBookings() {
  listEl.innerHTML = '<p class="muted-text">Loading...</p>';
  try {
    const data = await getBookingsByUser(getUserId());
    bookings = Array.isArray(data) ? data : [];
    renderList();
  } catch (err) {
    listEl.innerHTML = `<p class="error-text">${err.message || 'Failed to load bookings'}</p>`;
  }
}

renderTabs();
loadBookings();
