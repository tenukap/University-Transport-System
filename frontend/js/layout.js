// Shared layout: injects the Sidebar + TopBar into every page.
// Replaces App.jsx, Sidebar.jsx and TopBar.jsx from the old React app.
// The active nav link and page title are chosen from <body data-page="...">.

const NAV_ITEMS = [
  { page: 'book',     href: 'book.html',     label: 'Book a Trip',   title: 'Book a Trip',    icon: calendarIcon() },
  { page: 'map',      href: 'map.html',      label: 'Live Trip Map', title: 'Live Trip Map',  icon: mapIcon() },
  { page: 'bookings', href: 'bookings.html', label: 'My Bookings',   title: 'My Bookings',    icon: bookIcon() },
  { page: 'profile',  href: 'profile.html',  label: 'My Profile',    title: 'My Profile',     icon: userIcon() },
];

function layoutInitials(name) {
  if (!name) return 'ST';
  return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

function renderLayout() {
  const current = document.body.dataset.page || '';

  // Signed-in user (cached at login). Falls back gracefully when absent.
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const displayName = (user && (user.fullName || user.name)) || 'Student';
  const roleLabel = (typeof getRole === 'function' && getRole()) || 'STUDENT';
  const initials = layoutInitials(displayName);

  const sidebarEl = document.getElementById('sidebar');
  if (sidebarEl) {
    const links = NAV_ITEMS.map((item) => {
      const active = item.page === current ? ' is-active' : '';
      return `<a class="sidebar__link${active}" href="${item.href}">${item.icon}<span>${item.label}</span></a>`;
    }).join('');

    sidebarEl.className = 'sidebar';
    sidebarEl.innerHTML = `
      <div class="sidebar__logo">
        ${busIcon(18)}
        <span class="sidebar__logo-text">TransitPass</span>
      </div>
      <nav class="sidebar__nav">${links}</nav>
      <div class="sidebar__user">
        <div class="sidebar__avatar">${initials}</div>
        <div>
          <div class="sidebar__user-name">${displayName}</div>
          <div class="sidebar__user-id">${roleLabel}</div>
        </div>
      </div>`;
  }

  const topbarEl = document.getElementById('topbar');
  if (topbarEl) {
    const match = NAV_ITEMS.find((item) => item.page === current);
    const title = match ? match.title : 'UTMS';
    topbarEl.className = 'topbar';
    topbarEl.innerHTML = `
      <h1 class="topbar__title">${title}</h1>
      <div class="topbar__right">
        <button class="topbar__bell" aria-label="Notifications">${bellIcon()}</button>
        <div class="topbar__avatar">${initials}</div>
        <button class="btn btn--outline" id="layout-logout" style="padding:6px 14px;">Logout</button>
      </div>`;

    const logoutBtn = document.getElementById('layout-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        if (typeof logout === 'function') logout();
        else { localStorage.clear(); window.location.replace('login.html'); }
      });
    }
  }
}

/* --- Inline SVG icons (same shapes the React app used) --- */
function busIcon(size = 20, color = '#FFFFFF') {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6v6M16 6v6M2 12h19.6M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v8c0 .4.1.8.2 1.2C2.5 16.3 3 18 3 18h3"/><circle cx="7" cy="18" r="2"/><path d="M9 18h5"/><circle cx="16" cy="18" r="2"/></svg>`;
}
function calendarIcon() {
  return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`;
}
function mapIcon() {
  return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/><line x1="8" y1="2" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="22"/></svg>`;
}
function bookIcon() {
  return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>`;
}
function userIcon() {
  return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`;
}
function bellIcon() {
  return `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>`;
}

document.addEventListener('DOMContentLoaded', renderLayout);
