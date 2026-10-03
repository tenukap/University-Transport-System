// My Invoices page.
// Depends on: config.js (API_BASE, getToken), auth.js (getUserId), api.js (getMyInvoices, submitSlip, escapeHtml).

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
const MAX_SLIP_BYTES = 5 * 1024 * 1024; // 5 MB client-side guard
const ALLOWED_EXTS   = ['jpg','jpeg','png','pdf'];

const contentEl  = document.getElementById('invoice-content');
const successBanner = document.getElementById('invoice-success');
const modalRoot  = document.getElementById('modal-root');

let invoices = []; // cached so modals can look up by invoiceId

// ── Format helpers ────────────────────────────────────────────────────────────

function fmtLkr(n) {
  if (n == null) return '—';
  return 'LKR ' + Number(n).toFixed(2);
}

function fmtPeriod(month, year) {
  return `${MONTHS_SHORT[month - 1]} ${year}`;
}

function fmtDate(iso) {
  if (!iso) return '—';
  // ISO date string from the backend (e.g. "2026-10-31"); parse without time component
  const [y, m, d] = iso.split('-');
  return new Date(+y, +m - 1, +d).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function invoiceBadge(status) {
  const s = (status || '').toUpperCase();
  const cls = { PAID: 'badge--inv-paid', OVERDUE: 'badge--inv-overdue', PENDING: 'badge--inv-pending' }[s] || 'badge--inv-pending';
  return `<span class="badge ${cls}">${escapeHtml(s)}</span>`;
}

function paymentStatusLabel(status) {
  const map = { PAID: 'Approved', PENDING: 'Pending review', FAILED: 'Rejected', CANCELLED: 'Cancelled' };
  return map[(status || '').toUpperCase()] || escapeHtml(status || '');
}

// Payable amount for a new submission = balance minus PENDING amounts already submitted
function payable(inv) {
  return Math.max(0, Number(inv.balance) - Number(inv.pendingAmount || 0));
}

// ── Table ─────────────────────────────────────────────────────────────────────

function renderTable() {
  if (!invoices.length) {
    contentEl.innerHTML = `
      <div class="empty-state">
        <p style="font-size:15px;color:var(--text-muted);text-align:center;">
          No invoices yet. Invoices appear automatically after you book a trip.
        </p>
      </div>`;
    return;
  }

  const rows = invoices.map((inv) => {
    const isOverdue  = (inv.status || '').toUpperCase() === 'OVERDUE';
    const payableAmt = payable(inv);
    const canPay     = payableAmt > 0;

    return `
      <tr class="${isOverdue ? 'row--overdue' : ''}" data-inv-id="${inv.id}">
        <td><strong>${escapeHtml(fmtPeriod(inv.billingMonth, inv.billingYear))}</strong></td>
        <td>${escapeHtml(fmtDate(inv.dueDate))}</td>
        <td style="text-align:right">${escapeHtml(fmtLkr(inv.totalAmount))}</td>
        <td style="text-align:right">${escapeHtml(fmtLkr(inv.amountPaid))}</td>
        <td style="text-align:right">${escapeHtml(fmtLkr(inv.pendingAmount))}</td>
        <td style="text-align:right">${escapeHtml(fmtLkr(inv.balance))}</td>
        <td>${invoiceBadge(inv.status)}</td>
        <td>
          <div class="invoice-actions">
            <button class="btn--action" data-pay="${inv.id}" ${canPay ? '' : 'disabled'}>Pay</button>
            <button class="btn--action-ghost" data-history="${inv.id}">History</button>
          </div>
        </td>
      </tr>`;
  }).join('');

  contentEl.innerHTML = `
    <div class="invoice-table-wrap">
      <table class="invoice-table">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
            <th style="text-align:right">Total</th>
            <th style="text-align:right">Paid</th>
            <th style="text-align:right">Pending review</th>
            <th style="text-align:right">Balance</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;

  // Wire action buttons after innerHTML is set
  contentEl.querySelectorAll('[data-pay]').forEach((btn) => {
    btn.addEventListener('click', () => openPayModal(Number(btn.dataset.pay)));
  });
  contentEl.querySelectorAll('[data-history]').forEach((btn) => {
    btn.addEventListener('click', () => openHistoryModal(Number(btn.dataset.history)));
  });
}

// ── Load invoices ─────────────────────────────────────────────────────────────

async function loadInvoices() {
  contentEl.innerHTML = '<p class="muted-text center" style="padding:32px 0;">Loading…</p>';
  try {
    const data = await getMyInvoices();
    invoices = Array.isArray(data) ? data : [];
    renderTable();
  } catch (err) {
    contentEl.innerHTML = `<p class="error-text center">${escapeHtml(err.message || 'Failed to load invoices')}</p>`;
  }
}

// ── Success banner ────────────────────────────────────────────────────────────

function showSuccess(msg) {
  successBanner.textContent = msg;
  successBanner.classList.remove('hidden');
  // Auto-hide after 5 seconds
  setTimeout(() => successBanner.classList.add('hidden'), 5000);
}

// ── Pay modal ─────────────────────────────────────────────────────────────────

function openPayModal(invoiceId) {
  const inv = invoices.find((i) => i.id === invoiceId);
  if (!inv) return;

  const payableAmt = payable(inv);
  const period     = fmtPeriod(inv.billingMonth, inv.billingYear);

  modalRoot.innerHTML = `
    <div class="modal-overlay" id="pay-overlay">
      <div class="modal" id="pay-box">
        <button class="modal__close" id="pay-close" aria-label="Close">✕</button>
        <h2 class="modal__title">Pay Invoice</h2>
        <p style="color:var(--text-muted);font-size:13px;margin:8px 0 0;">${escapeHtml(period)}</p>

        <div class="modal-field">
          <label for="pay-amount">Amount (LKR)</label>
          <input id="pay-amount" class="field-input" type="number"
                 step="0.01" min="0.01" max="${payableAmt.toFixed(2)}"
                 value="${payableAmt.toFixed(2)}" />
          <p class="hint">Payable: ${escapeHtml(fmtLkr(payableAmt))} (balance minus pending)</p>
        </div>

        <div class="modal-field">
          <label for="pay-slip">Bank slip</label>
          <input id="pay-slip" type="file" accept=".jpg,.jpeg,.png,.pdf" />
          <p class="hint">JPG, PNG or PDF, max 5 MB</p>
        </div>

        <p id="pay-error" class="modal__error hidden"></p>

        <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:20px;">
          <button class="btn btn--ghost" id="pay-cancel-btn" style="padding:8px 20px;">Cancel</button>
          <button class="btn btn--primary" id="pay-submit-btn" style="padding:8px 20px;">Submit</button>
        </div>
      </div>
    </div>`;

  const close = () => { modalRoot.innerHTML = ''; };
  document.getElementById('pay-overlay').addEventListener('click', close);
  document.getElementById('pay-box').addEventListener('click', (e) => e.stopPropagation());
  document.getElementById('pay-close').addEventListener('click', close);
  document.getElementById('pay-cancel-btn').addEventListener('click', close);
  document.getElementById('pay-submit-btn').addEventListener('click', () => handlePay(inv, payableAmt));
}

async function handlePay(inv, payableAmt) {
  const amountInput = document.getElementById('pay-amount');
  const slipInput   = document.getElementById('pay-slip');
  const errEl       = document.getElementById('pay-error');
  const submitBtn   = document.getElementById('pay-submit-btn');

  errEl.classList.add('hidden');
  errEl.textContent = '';

  // ── Client-side validation ────────────────────────────────────────────────

  const amount = parseFloat(amountInput.value);
  if (!amount || amount <= 0) {
    return showPayError(errEl, 'Amount must be greater than 0.');
  }
  if (amount > payableAmt + 0.001) { // small tolerance for float rounding
    return showPayError(errEl, `Amount exceeds the payable balance (${fmtLkr(payableAmt)}).`);
  }

  const file = slipInput.files && slipInput.files[0];
  if (!file) {
    return showPayError(errEl, 'Please select a slip file.');
  }
  // Extension check — guards against wrong file types before the server even sees the request
  const ext = file.name.split('.').pop().toLowerCase();
  if (!ALLOWED_EXTS.includes(ext)) {
    return showPayError(errEl, 'File type not allowed. Please upload a JPG, PNG, or PDF.');
  }
  if (file.size > MAX_SLIP_BYTES) {
    return showPayError(errEl, 'File is too large. Maximum size is 5 MB.');
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Submitting…';

  const fd = new FormData();
  fd.append('amount', amount.toFixed(2));
  fd.append('slip', file);

  try {
    await submitSlip(inv.id, fd);
    modalRoot.innerHTML = '';
    showSuccess(`Slip submitted for ${fmtPeriod(inv.billingMonth, inv.billingYear)}. It is pending review.`);
    await loadInvoices(); // Reload so pending amount and Pay disabled state are updated
  } catch (err) {
    showPayError(errEl, err.message || 'Submission failed. Please try again.');
    submitBtn.disabled = false;
    submitBtn.textContent = 'Submit';
  }
}

function showPayError(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}

// ── History modal ─────────────────────────────────────────────────────────────

function openHistoryModal(invoiceId) {
  const inv = invoices.find((i) => i.id === invoiceId);
  if (!inv) return;

  const period   = fmtPeriod(inv.billingMonth, inv.billingYear);
  const payments = inv.payments || [];

  let paymentRows;
  if (!payments.length) {
    paymentRows = '<p class="muted-text" style="margin:12px 0;">No payment submissions yet.</p>';
  } else {
    paymentRows = payments.map((p) => {
      const statusLabel = paymentStatusLabel(p.status);
      const isRejected  = (p.status || '').toUpperCase() === 'FAILED';
      const reasonHtml  = isRejected && p.reviewNote
        ? `<div class="history-row__reason">Reason: ${escapeHtml(p.reviewNote)}</div>`
        : '';
      const viewSlipBtn = p.hasSlip
        ? `<button class="btn--action-ghost" style="margin-left:8px;" data-view-slip="${p.paymentId}">View slip</button>`
        : '';

      return `
        <div class="history-row">
          <div class="history-row__top">
            <span><strong>${escapeHtml(fmtLkr(p.amount))}</strong></span>
            <span>${escapeHtml(statusLabel)}${viewSlipBtn}</span>
          </div>
          <div class="history-row__meta">${escapeHtml(fmtDate(p.paymentDate))}</div>
          ${reasonHtml}
        </div>`;
    }).join('');
  }

  modalRoot.innerHTML = `
    <div class="modal-overlay" id="hist-overlay">
      <div class="modal" id="hist-box">
        <button class="modal__close" id="hist-close" aria-label="Close">✕</button>
        <h2 class="modal__title">Payment History</h2>
        <p style="color:var(--text-muted);font-size:13px;margin:8px 0 0;">${escapeHtml(period)}</p>
        <div style="margin-top:16px;" id="hist-payments">${paymentRows}</div>
        <p id="hist-error" class="modal__error hidden"></p>
      </div>
    </div>`;

  const close = () => { modalRoot.innerHTML = ''; };
  document.getElementById('hist-overlay').addEventListener('click', close);
  document.getElementById('hist-box').addEventListener('click', (e) => e.stopPropagation());
  document.getElementById('hist-close').addEventListener('click', close);

  // Wire "View slip" buttons
  document.querySelectorAll('[data-view-slip]').forEach((btn) => {
    btn.addEventListener('click', () => viewSlip(Number(btn.dataset.viewSlip)));
  });
}

// Fetch the slip file as a blob and open it in a new tab.
// A plain <a href> cannot send the Bearer token, so we use fetch + object URL.
async function viewSlip(paymentId) {
  const errEl = document.getElementById('hist-error');
  if (errEl) { errEl.classList.add('hidden'); errEl.textContent = ''; }

  const token = getToken();
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const res = await fetch(`${API_BASE}/slips/${paymentId}`, { headers });
    if (!res.ok) {
      const msg = await res.text().catch(() => '');
      throw new Error(msg || `Could not load slip (${res.status})`);
    }
    const blob = await res.blob();
    const url  = URL.createObjectURL(blob);
    window.open(url, '_blank');
    // Revoke the object URL after 60 s — the browser will have loaded it by then
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (errEl) {
      errEl.textContent = err.message || 'Could not load slip.';
      errEl.classList.remove('hidden');
    } else {
      alert(err.message || 'Could not load slip.');
    }
  }
}

// ── Boot ──────────────────────────────────────────────────────────────────────

loadInvoices();
