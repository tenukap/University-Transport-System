// Authentication actions. Depends on config.js (API_BASE, decodeToken, getToken).

// POST /api/portal/login. On success stores the JWT under 'token' (plus a
// cached 'user' object and 'role'), and returns the raw response object.
// Throws an Error with the backend message on failure.
async function login(email, password) {
  const res = await fetch(`${API_BASE}/portal/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (e) {
    /* backend returned a plain-text error */
  }

  if (!res.ok) {
    const msg = (data && (data.message || data.error)) || text || `Login failed (${res.status})`;
    throw new Error(msg);
  }
  if (!data || !data.token) {
    throw new Error('Server did not return a token');
  }

  localStorage.setItem('token', data.token);
  if (data.user) localStorage.setItem('user', JSON.stringify(data.user));
  const claims = decodeToken(data.token);
  if (claims && claims.role) localStorage.setItem('role', claims.role);

  return data;
}

// Clear all stored auth state and return to the login page.
function logout() {
  localStorage.clear();
  window.location.replace('login.html');
}

// Role string from the JWT ('ADMIN' | 'STUDENT' | 'DRIVER' | 'FINANCE_OFFICER'), or null.
function getRole() {
  const claims = decodeToken(getToken());
  return claims ? claims.role : null;
}

// User id (JWT 'sub' claim) as a Number, or null.
function getUserId() {
  const claims = decodeToken(getToken());
  if (!claims || claims.sub == null) return null;
  return Number(claims.sub);
}

// True when a non-expired token is present.
function isAuthenticated() {
  const claims = decodeToken(getToken());
  if (!claims) return false;
  if (claims.exp && Date.now() >= claims.exp * 1000) return false;
  return true;
}
