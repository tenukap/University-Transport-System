const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const ROOT = __dirname;
const FRONTEND_ROOT = path.resolve(__dirname, '..');
const LOGIN_ROOT = path.resolve(__dirname, '..', 'front end of sign in');
const PROFILE_ROOT = path.resolve(__dirname, '..', '..', 'stitch_transport_profile_interface');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

http.createServer((req, res) => {
  const requestPath = req.url === '/' ? '/login.html' : req.url;
  const safePath = path.normalize(requestPath).replace(/^\.+/, '');
  const urlPath = requestPath.split('?')[0].replace(/\\/g, '/');

  let filePath;
  if (urlPath === '/login.html') {
    filePath = path.join(FRONTEND_ROOT, 'login.html');
  } else if (urlPath === '/profile.html') {
    filePath = path.join(FRONTEND_ROOT, 'profile.html');
  } else {
    filePath = path.join(ROOT, safePath);
  }

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
    res.end(content);
  });
}).listen(PORT, () => {
  console.log(`Frontend running at http://localhost:${PORT}/login.html`);
  console.log(`Admin dashboard at http://localhost:${PORT}/code.html`);
  console.log(`Transport profile at http://localhost:${PORT}/profile.html`);
});

