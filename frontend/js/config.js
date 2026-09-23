// Global config + shared auth helpers. Loaded first on every page.

const API_BASE = 'http://localhost:8080/api';

// --- JWT helpers -----------------------------------------------------------

// Decode the payload (middle) segment of a JWT. Returns the claims object,
// or null if the token is missing/malformed. Handles URL-safe base64 + UTF-8.
function decodeToken(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(json);
  } catch (e) {
    return null;
  }
}

function getToken() {
  return localStorage.getItem('token');
}

// Returns fetch headers including Authorization: Bearer <token> when present.
function getAuthHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

// The backend adds a 'studentId' claim at login (looked up from the Student
// table). It is present only for STUDENT accounts; other roles won't have it.
function getStudentIdFromToken() {
  const claims = decodeToken(getToken());
  if (!claims || claims.studentId == null) return null;
  return Number(claims.studentId);
}

function redirectToLogin() {
  window.location.replace('login.html');
}

// Page guard. Ensures a valid, unexpired token whose role matches requiredRole
// (a string or array of strings). Redirects to login.html and returns null on
// failure; returns the decoded claims on success.
function checkAuth(requiredRole) {
  const claims = decodeToken(getToken());
  if (!claims) {
    redirectToLogin();
    return null;
  }
  if (claims.exp && Date.now() >= claims.exp * 1000) {
    localStorage.clear();
    redirectToLogin();
    return null;
  }
  if (requiredRole) {
    const allowed = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
    if (!allowed.includes(claims.role)) {
      redirectToLogin();
      return null;
    }
  }
  return claims;
}
