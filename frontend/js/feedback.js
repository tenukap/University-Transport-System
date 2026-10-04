// Student feedback page: give feedback on departed trips, view own feedback history.
// Depends on: config.js, auth.js, api.js (request helper + escapeHtml)

const esc = escapeHtml;

// ---- state ----
let selectedRating = 0;
let eligibleTrips = [];

// ---- init ----
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('give-feedback-btn').addEventListener('click', openModal);
  document.getElementById('fb-modal-cancel').addEventListener('click', closeModal);
  document.getElementById('fb-submit-btn').addEventListener('click', submitFeedback);
  document.getElementById('fb-comment').addEventListener('input', updateCharCount);
  setupStars();

  // close on overlay click
  document.getElementById('feedback-modal').addEventListener('click', e => {
    if (e.target === document.getElementById('feedback-modal')) closeModal();
  });

  loadMyFeedback();
});

// ---- star selector ----
function setupStars() {
  document.querySelectorAll('.star-btn').forEach(btn => {
    btn.addEventListener('click', () => setRating(parseInt(btn.dataset.value, 10)));
    btn.addEventListener('mouseenter', () => highlightStars(parseInt(btn.dataset.value, 10)));
    btn.addEventListener('mouseleave', () => highlightStars(selectedRating));
  });
}

function setRating(val) {
  selectedRating = val;
  document.getElementById('fb-rating').value = val;
  highlightStars(val);
}

function highlightStars(val) {
  document.querySelectorAll('.star-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.value, 10) <= val);
  });
}

function updateCharCount() {
  const len = document.getElementById('fb-comment').value.length;
  document.getElementById('fb-char-count').textContent = len;
}

// ---- modal ----
async function openModal() {
  resetModal();
  document.getElementById('feedback-modal').classList.add('show');

  const select = document.getElementById('fb-trip-select');
  const noTrips = document.getElementById('fb-no-trips');
  select.innerHTML = '<option value="">Loading…</option>';
  select.style.display = 'block';
  noTrips.style.display = 'none';

  try {
    eligibleTrips = await request('/feedback/eligible');
    if (!eligibleTrips.length) {
      select.style.display = 'none';
      noTrips.style.display = 'block';
      document.getElementById('fb-submit-btn').disabled = true;
    } else {
      select.innerHTML = eligibleTrips.map(t =>
        `<option value="${esc(String(t.bookingId))}">${esc(t.tripLabel)}${t.busRegistration ? ' — ' + esc(t.busRegistration) : ''}</option>`
      ).join('');
      document.getElementById('fb-submit-btn').disabled = false;
    }
  } catch (err) {
    showModalError(err.message || 'Failed to load eligible trips.');
    select.style.display = 'none';
    document.getElementById('fb-submit-btn').disabled = true;
  }
}

function closeModal() {
  document.getElementById('feedback-modal').classList.remove('show');
  resetModal();
}

function resetModal() {
  selectedRating = 0;
  highlightStars(0);
  document.getElementById('fb-rating').value = '';
  document.getElementById('fb-comment').value = '';
  document.getElementById('fb-char-count').textContent = '0';
  hideModalError();
  document.getElementById('fb-submit-btn').disabled = false;
}

function showModalError(msg) {
  const el = document.getElementById('fb-modal-error');
  el.textContent = msg;
  el.classList.add('show');
}

function hideModalError() {
  const el = document.getElementById('fb-modal-error');
  el.textContent = '';
  el.classList.remove('show');
}

// ---- submit ----
async function submitFeedback() {
  hideModalError();

  const bookingId = parseInt(document.getElementById('fb-trip-select').value, 10);
  if (!bookingId) { showModalError('Please select a trip.'); return; }

  const rating = selectedRating;
  if (!rating) { showModalError('Please select a rating (1–5 stars).'); return; }

  const comments = document.getElementById('fb-comment').value.trim();
  if (!comments) { showModalError('Please enter a comment.'); return; }
  if (comments.length > 1000) { showModalError('Comment cannot exceed 1000 characters.'); return; }

  const btn = document.getElementById('fb-submit-btn');
  btn.disabled = true;
  btn.textContent = 'Submitting…';

  try {
    await request('/feedback', { method: 'POST', body: { bookingId, rating, comments } });
    closeModal();
    loadMyFeedback();
  } catch (err) {
    showModalError(err.message || 'Failed to submit feedback.');
    btn.disabled = false;
    btn.textContent = 'Submit';
  }
}

// ---- my feedback list ----
async function loadMyFeedback() {
  const container = document.getElementById('feedback-list');
  container.innerHTML = '<div style="padding:24px;color:#8E9A98;text-align:center;">Loading…</div>';

  try {
    const list = await request('/feedback/mine');
    if (!list.length) {
      container.innerHTML = '<div style="padding:24px;color:#8E9A98;text-align:center;">You have not given any feedback yet.</div>';
      return;
    }
    container.innerHTML = list.map(fb => renderCard(fb)).join('');
  } catch (err) {
    container.innerHTML = `<div style="padding:24px;color:#B4463C;text-align:center;">${esc(err.message || 'Failed to load feedback.')}</div>`;
  }
}

function renderCard(fb) {
  const stars = fb.rating ? '★'.repeat(fb.rating) + '☆'.repeat(5 - fb.rating) : '—';
  const statusBadge = fb.status === 'Reviewed'
    ? '<span class="badge-reviewed">Reviewed</span>'
    : '<span class="badge-pending">Pending</span>';
  const date = fb.submittedAt ? new Date(fb.submittedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '—';

  let replyBlock = '';
  if (fb.status === 'Reviewed' && fb.adminResponse) {
    replyBlock = `<div class="feedback-card__reply">
      <div class="feedback-card__reply-label">Admin Reply</div>
      <div>${esc(fb.adminResponse)}</div>
    </div>`;
  }

  return `<div class="feedback-card">
    <div class="feedback-card__header">
      <div class="feedback-card__trip">${esc(fb.tripLabel || '—')}</div>
      <div class="feedback-card__stars">${esc(stars)}</div>
    </div>
    <div class="feedback-card__comment">${esc(fb.comments || '')}</div>
    <div class="feedback-card__meta">
      ${statusBadge}
      <span>${esc(date)}</span>
    </div>
    ${replyBlock}
  </div>`;
}
