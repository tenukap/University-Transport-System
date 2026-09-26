// My Bookings page (ports MyBookings.jsx).
// Shows real trip details, marks past trips "Completed", and lets students
// leave a star rating + comment on a booking once its trip date has passed.

const TABS = ['All', 'Pending', 'Confirmed', 'Completed', 'Cancelled'];

let bookings = [];
let feedbackMap = {}; // bookingId (string) -> feedback response
let activeTab = 'All';
let selected = null;
let feedbackFor = null; // booking currently being reviewed
let feedbackRating = 0;

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

// Trip date arrives as an ISO date string ("2026-09-22"); start time as "HH:mm:ss".
function formatTripDate(b) {
  if (!b.tripDate) return 'Date to be confirmed';
  const d = new Date(`${b.tripDate}T00:00:00`);
  if (isNaN(d)) return b.tripDate;
  const datePart = d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  const timePart = b.startTime ? b.startTime.slice(0, 5) : null;
  return timePart ? `${datePart}, ${timePart}` : datePart;
}

function routeLabel(b) {
  return `${b.pickupName || 'Pickup'} → ${b.dropoffName || 'SLIIT'}`;
}

function tripTitle(b) {
  return `Trip to ${b.dropoffName || 'SLIIT'}`;
}

// A booking is "completed" once its trip date is strictly before today
// (and it wasn't cancelled) — mirrors the backend feedback rule.
function isPast(b) {
  if (!b.tripDate || (b.status || '').toUpperCase() === 'CANCELLED') return false;
  const tripDay = new Date(`${b.tripDate}T00:00:00`);
  if (isNaN(tripDay)) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return tripDay < today;
}

function canCancel(status) {
  return ['PENDING', 'CONFIRMED'].includes((status || '').toUpperCase());
}

function starsDisplay(rating) {
  let s = '';
  for (let i = 1; i <= 5; i++) s += `<span class="star${i <= rating ? ' is-filled' : ''}">★</span>`;
  return `<span class="stars">${s}</span>`;
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
  const filtered = bookings.filter((b) => {
    if (activeTab === 'All') return true;
    if (activeTab === 'Completed') return isPast(b);
    return (b.status || '').toUpperCase() === activeTab.toUpperCase();
  });

  if (filtered.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state__icon">${busIconSvg(48, '#8E9A98')}</div>
        <p>No bookings found</p>
      </div>`;
    return;
  }

  listEl.innerHTML = filtered.map((b) => {
    const past = isPast(b);
    const fb = feedbackMap[String(b.id)];

    let feedbackAction = '';
    if (past && fb) {
      feedbackAction = `<div class="booking-rating">Your rating: ${starsDisplay(fb.rating)}</div>`;
    } else if (past) {
      feedbackAction = `<button class="btn btn--primary" data-feedback="${b.id}">Leave Feedback</button>`;
    }

    return `
    <div class="booking-card">
      <div class="booking-card__inner">
        <div class="booking-icon">${busIconSvg()}</div>
        <div class="booking-body">
          <div class="booking-row">
            <span class="booking-title">${tripTitle(b)}</span>
            <span class="booking-badges">
              ${badge(b.status)}
              ${past ? '<span class="badge badge--completed">Completed</span>' : ''}
            </span>
          </div>
          <div class="booking-meta">${routeLabel(b)}</div>
          <div class="booking-meta--sm">${formatTripDate(b)}</div>
          ${b.seatNumber != null ? `<div class="booking-seat">Seat #${b.seatNumber}</div>` : ''}
          <div class="booking-actions">
            <button class="btn btn--outline" data-view="${b.id}">View Details</button>
            ${feedbackAction}
          </div>
        </div>
      </div>
    </div>`;
  }).join('');

  listEl.querySelectorAll('[data-view]').forEach((btn) => {
    btn.addEventListener('click', () => {
      selected = bookings.find((b) => String(b.id) === btn.dataset.view);
      renderModal();
    });
  });
  listEl.querySelectorAll('[data-feedback]').forEach((btn) => {
    btn.addEventListener('click', () => openFeedback(btn.dataset.feedback));
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

  const past = isPast(selected);
  const fb = feedbackMap[String(selected.id)];

  let feedbackSection = '';
  if (past && fb) {
    feedbackSection = `
      <div class="feedback-summary">
        <div class="detail-row"><span class="detail-row__label">Your rating</span>${starsDisplay(fb.rating)}</div>
        ${fb.comment && fb.comment !== '(no comment)' ? `<p class="feedback-comment">“${fb.comment}”</p>` : ''}
      </div>`;
  } else if (past) {
    feedbackSection = `<button class="btn btn--primary btn--block" id="modal-feedback-btn">Leave Feedback</button>`;
  }

  modalRoot.innerHTML = `
    <div class="modal-overlay" id="modal-overlay">
      <div class="modal" id="modal-box">
        <button class="modal__close" id="modal-close" aria-label="Close">✕</button>
        <h2 class="modal__title">${tripTitle(selected)}</h2>
        <div class="modal__rows">
          ${detailRow('Booking', `#${selected.id}`)}
          <div class="detail-row"><span class="detail-row__label">Status</span>
            <span class="booking-badges">${badge(selected.status)}${past ? '<span class="badge badge--completed">Completed</span>' : ''}</span>
          </div>
          ${detailRow('Route', routeLabel(selected))}
          ${detailRow('Trip Date', formatTripDate(selected))}
          ${detailRow('Date Booked', formatDate(selected.createdAt))}
          ${detailRow('Seat Number', selected.seatNumber != null ? `#${selected.seatNumber}` : 'Not assigned')}
        </div>
        ${feedbackSection}
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

  const fbBtn = document.getElementById('modal-feedback-btn');
  if (fbBtn) fbBtn.addEventListener('click', () => { const b = selected; selected = null; renderModal(); openFeedback(b.id); });
}

function openFeedback(bookingId) {
  feedbackFor = bookings.find((b) => String(b.id) === String(bookingId));
  feedbackRating = 0;
  renderFeedbackModal();
}

function renderFeedbackModal() {
  if (!feedbackFor) {
    modalRoot.innerHTML = '';
    return;
  }
  const b = feedbackFor;

  modalRoot.innerHTML = `
    <div class="modal-overlay" id="fb-overlay">
      <div class="modal" id="fb-box">
        <button class="modal__close" id="fb-close" aria-label="Close">✕</button>
        <h2 class="modal__title">Rate your trip</h2>
        <p class="booking-meta">${tripTitle(b)} — ${formatTripDate(b)}</p>
        <div class="star-input" id="fb-stars">
          ${[1, 2, 3, 4, 5].map((i) => `<button type="button" class="star-btn" data-rate="${i}">★</button>`).join('')}
        </div>
        <textarea id="fb-comment" class="feedback-textarea" rows="4" placeholder="Tell us about your trip (optional)"></textarea>
        <p class="error-text" id="fb-error" style="display:none;"></p>
        <button class="btn btn--primary btn--block" id="fb-submit">Submit Feedback</button>
      </div>
    </div>`;

  const close = () => { feedbackFor = null; feedbackRating = 0; renderFeedbackModal(); };
  document.getElementById('fb-overlay').addEventListener('click', close);
  document.getElementById('fb-box').addEventListener('click', (e) => e.stopPropagation());
  document.getElementById('fb-close').addEventListener('click', close);

  document.querySelectorAll('#fb-stars .star-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      feedbackRating = Number(btn.dataset.rate);
      document.querySelectorAll('#fb-stars .star-btn').forEach((b2) => {
        b2.classList.toggle('is-filled', Number(b2.dataset.rate) <= feedbackRating);
      });
    });
  });

  document.getElementById('fb-submit').addEventListener('click', handleSubmitFeedback);
}

async function handleSubmitFeedback() {
  if (!feedbackFor) return;
  const err = document.getElementById('fb-error');
  err.style.display = 'none';
  if (!feedbackRating) {
    err.textContent = 'Please select a star rating.';
    err.style.display = 'block';
    return;
  }
  const submit = document.getElementById('fb-submit');
  submit.disabled = true;
  submit.textContent = 'Submitting…';
  try {
    await submitFeedback({
      bookingId: feedbackFor.id,
      rating: feedbackRating,
      comment: document.getElementById('fb-comment').value.trim(),
    });
    feedbackFor = null;
    feedbackRating = 0;
    renderFeedbackModal();
    await loadBookings();
  } catch (e) {
    err.textContent = e.message || 'Failed to submit feedback';
    err.style.display = 'block';
    submit.disabled = false;
    submit.textContent = 'Submit Feedback';
  }
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
    const userId = getUserId();
    const [data, fb] = await Promise.all([
      getBookingsByUser(userId),
      getFeedbackByUser(userId).catch(() => []),
    ]);
    bookings = Array.isArray(data) ? data : [];
    feedbackMap = {};
    (Array.isArray(fb) ? fb : []).forEach((f) => {
      if (f.bookingId != null) feedbackMap[String(f.bookingId)] = f;
    });
    renderList();
  } catch (err) {
    listEl.innerHTML = `<p class="error-text">${err.message || 'Failed to load bookings'}</p>`;
  }
}

renderTabs();
loadBookings();
