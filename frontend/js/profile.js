// Profile page (ports Profile.jsx).

function getInitials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

let student = null;
let editing = false;

const root = document.getElementById('profile-root');

function render() {
  if (!student) return;

  const fullName = student.fullName || '';
  const phone = student.phone || '';

  root.innerHTML = `
    <div class="card">
      <div class="profile-top">
        <div class="profile-avatar">${getInitials(student.fullName)}</div>
        <h2 class="profile-name">${student.fullName || ''}</h2>
        <span class="profile-pass">Student Transport Pass • Active</span>
      </div>

      <div id="profile-success" class="notice--success hidden"></div>

      <div class="fields">
        ${field('Full Name', fullName, editing, 'fullName')}
        ${field('Student Index', student.studentIndex, false)}
        ${field('Email Address', student.email, false)}
        ${field('Phone Number', phone, editing, 'phone')}
      </div>

      <div class="actions">
        ${editing
          ? `<button class="btn btn--ghost" id="cancel-btn">Cancel</button>
             <button class="btn btn--save" id="save-btn">Save</button>`
          : `<button class="btn btn--outline" id="edit-btn">Edit Profile</button>`}
      </div>
    </div>`;

  wireActions();
}

function field(label, value, editable, name) {
  const safe = value == null ? '' : value;
  const control = editable
    ? `<input class="field-input" data-field="${name}" value="${escapeAttr(safe)}" />`
    : `<div class="field__value">${safe || '—'}</div>`;
  return `<div class="field"><label class="field__label">${label}</label>${control}</div>`;
}

function escapeAttr(s) {
  return String(s).replace(/"/g, '&quot;');
}

function wireActions() {
  const editBtn = document.getElementById('edit-btn');
  const cancelBtn = document.getElementById('cancel-btn');
  const saveBtn = document.getElementById('save-btn');

  if (editBtn) editBtn.addEventListener('click', () => { editing = true; render(); });
  if (cancelBtn) cancelBtn.addEventListener('click', () => { editing = false; render(); });
  if (saveBtn) saveBtn.addEventListener('click', handleSave);
}

async function handleSave() {
  const saveBtn = document.getElementById('save-btn');
  const fullName = document.querySelector('[data-field="fullName"]').value;
  const phone = document.querySelector('[data-field="phone"]').value;

  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving...';
  try {
    student = await updateStudent(STUDENT_ID, { fullName, phone });
    editing = false;
    render();
    const success = document.getElementById('profile-success');
    success.textContent = 'Profile updated successfully';
    success.classList.remove('hidden');
  } catch (err) {
    alert(err.message || 'Update failed');
    saveBtn.disabled = false;
    saveBtn.textContent = 'Save';
  }
}

async function loadStudent() {
  root.innerHTML = '<p class="muted-text center">Loading...</p>';
  try {
    student = await getStudentById(STUDENT_ID);
    render();
  } catch (err) {
    root.innerHTML = `<p class="error-text center">${err.message || 'Failed to load profile'}</p>`;
  }
}

loadStudent();
