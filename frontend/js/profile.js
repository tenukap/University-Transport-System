// Profile page.

// ============================================================
// Shared helpers
// ============================================================

function getInitials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

function escapeAttr(s) {
  return String(s == null ? '' : s).replace(/"/g, '&quot;');
}

function fmtMoney(amount) {
  if (amount == null) return '—';
  return `LKR ${Number(amount).toFixed(2)}`;
}

function monthName(m) {
  return new Date(2000, m - 1, 1).toLocaleString('default', { month: 'long' });
}

// ============================================================
// Profile card
// ============================================================

let student = null;
let editing = false;

const root = document.getElementById('profile-root');

function render() {
  if (!student) return;

  const fullName = student.fullName || '';
  const phone    = student.phone    || '';

  root.innerHTML = `
    <div class="card">
      <div class="profile-top">
        <div class="profile-avatar">${escapeHtml(getInitials(student.fullName))}</div>
        <h2 class="profile-name">${escapeHtml(student.fullName || '')}</h2>
        <span class="profile-pass">Student Transport Pass &bull; Active</span>
      </div>

      <div id="profile-success" class="notice--success hidden"></div>

      <div class="fields">
        ${field('Full Name',     fullName,                         editing, 'fullName')}
        ${field('Student Index', student.studentIndex || 'Not set', false)}
        ${field('Email Address', student.email,                    false)}
        ${field('Phone Number',  phone,                            editing, 'phone')}
      </div>

      <div class="actions">
        ${editing
          ? `<button class="btn btn--ghost" id="cancel-btn">Cancel</button>
             <button class="btn btn--save"  id="save-btn">Save</button>`
          : `<button class="btn btn--outline" id="edit-btn">Edit Profile</button>`}
      </div>
    </div>`;

  wireActions();
}

function field(label, value, editable, name) {
  const safe = value == null ? '' : value;
  const control = editable
    ? `<input class="field-input" data-field="${escapeAttr(name)}" value="${escapeAttr(safe)}" />`
    : `<div class="field__value">${escapeHtml(safe) || '—'}</div>`;
  return `<div class="field"><label class="field__label">${escapeHtml(label)}</label>${control}</div>`;
}

function wireActions() {
  const editBtn   = document.getElementById('edit-btn');
  const cancelBtn = document.getElementById('cancel-btn');
  const saveBtn   = document.getElementById('save-btn');

  if (editBtn)   editBtn.addEventListener('click',   () => { editing = true;  render(); });
  if (cancelBtn) cancelBtn.addEventListener('click', () => { editing = false; render(); });
  if (saveBtn)   saveBtn.addEventListener('click', handleSave);
}

async function handleSave() {
  const saveBtn  = document.getElementById('save-btn');
  const fullName = document.querySelector('[data-field="fullName"]').value;
  const phone    = document.querySelector('[data-field="phone"]').value;

  saveBtn.disabled  = true;
  saveBtn.textContent = 'Saving...';
  try {
    await updateUser(student.id, { fullName, phone });
    student = await getStudentByUserId(getUserId());
    editing = false;
    render();
    const success = document.getElementById('profile-success');
    success.textContent = 'Profile updated successfully';
    success.classList.remove('hidden');
  } catch (err) {
    alert(err.message || 'Update failed');
    saveBtn.disabled    = false;
    saveBtn.textContent = 'Save';
  }
}

async function loadStudent() {
  root.innerHTML = '<p class="muted-text center">Loading...</p>';
  try {
    student = await getStudentByUserId(getUserId());
    render();
  } catch (err) {
    root.innerHTML = `<p class="error-text center">${escapeHtml(err.message || 'Failed to load profile')}</p>`;
  }
}

// ============================================================
// Monthly Charges card  (independent of profile edit/save)
// ============================================================

const chargesRoot = document.getElementById('charges-root');

function buildChargesShell() {
  const now   = new Date();
  const curM  = now.getMonth() + 1;   // 1-based
  const curY  = now.getFullYear();

  const monthOpts = Array.from({ length: 12 }, (_, i) => {
    const m = i + 1;
    return `<option value="${m}" ${m === curM ? 'selected' : ''}>${escapeHtml(monthName(m))}</option>`;
  }).join('');

  const startYear = 2020;
  const yearOpts  = Array.from({ length: curY - startYear + 1 }, (_, i) => {
    const y = startYear + i;
    return `<option value="${y}" ${y === curY ? 'selected' : ''}>${y}</option>`;
  }).join('');

  chargesRoot.innerHTML = `
    <div class="card">
      <div style="margin-bottom:16px;display:flex;align-items:center;justify-content:space-between;">
        <h3 style="font:700 17px/1 'Montserrat',sans-serif;color:var(--text-primary);margin:0;">
          Monthly Travel Charges
        </h3>
        <a href="invoices.html" style="font-size:13px;color:var(--primary);font-weight:600;text-decoration:none;">View invoices →</a>
      </div>
      <div class="charges-selectors">
        <div>
          <div class="field__label" style="margin-bottom:4px;">Month</div>
          <select id="charges-month" class="select" style="width:auto;padding:8px 12px;">${monthOpts}</select>
        </div>
        <div>
          <div class="field__label" style="margin-bottom:4px;">Year</div>
          <select id="charges-year" class="select" style="width:auto;padding:8px 12px;">${yearOpts}</select>
        </div>
      </div>
      <div id="charges-body" style="margin-top:16px;">
        <p class="muted-text">Loading charges…</p>
      </div>
    </div>`;

  document.getElementById('charges-month').addEventListener('change', loadCharges);
  document.getElementById('charges-year').addEventListener('change',  loadCharges);
}

async function loadCharges() {
  const monthSel = document.getElementById('charges-month');
  const yearSel  = document.getElementById('charges-year');
  const body     = document.getElementById('charges-body');
  if (!monthSel || !yearSel || !body) return;

  const month = Number(monthSel.value);
  const year  = Number(yearSel.value);

  body.innerHTML = '<p class="muted-text">Loading…</p>';

  try {
    const data = await getMyCharges(month, year);
    renderChargesBody(body, data, month, year);
  } catch (err) {
    body.innerHTML = `<p class="error-text">${escapeHtml(err.message || 'Failed to load charges')}</p>`;
  }
}

function renderChargesBody(body, data, selMonth, selYear) {
  if (!data || data.count === 0) {
    body.innerHTML = `
      <p class="muted-text" style="margin:0 0 16px;">No bookings this month.</p>
      ${renderHistory(data ? data.months : [], selMonth, selYear)}`;
    return;
  }

  const rows = (data.items || []).map((item) => `
    <tr>
      <td style="padding:9px 4px;">${escapeHtml(item.tripDate || '—')}</td>
      <td style="padding:9px 4px;">${escapeHtml(item.pickup || '—')} → ${escapeHtml(item.dropoff || '—')}</td>
      <td style="padding:9px 4px;">${item.seatNumber != null ? escapeHtml(String(item.seatNumber)) : '—'}</td>
      <td style="padding:9px 4px;text-align:right;">${escapeHtml(fmtMoney(item.fareAmount))}</td>
    </tr>`).join('');

  body.innerHTML = `
    <table class="charges-table">
      <thead>
        <tr>
          <th>Date</th><th>Route</th><th>Seat</th><th style="text-align:right;">Fare</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
    <p class="charges-total">Total: ${escapeHtml(fmtMoney(data.total))}</p>
    ${renderHistory(data.months || [], selMonth, selYear)}`;
}

function renderHistory(months, selMonth, selYear) {
  if (!months || !months.length) return '';
  const items = months.map((m) => {
    const isCurrent = m.month === selMonth && m.year === selYear;
    return `<div class="charges-history__item${isCurrent ? ' is-current' : ''}">
      <span>${escapeHtml(monthName(m.month))} ${escapeHtml(String(m.year))}</span>
      <span>${escapeHtml(fmtMoney(m.total))}</span>
    </div>`;
  }).join('');
  return `<div class="charges-history">
    <div class="charges-history__title">Last 6 Months</div>
    ${items}
  </div>`;
}

// ============================================================
// Boot
// ============================================================

loadStudent();
buildChargesShell();
loadCharges();
