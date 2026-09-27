# TransitPass — Frontend (Plain HTML/CSS/JS)

The frontend for the University Transport Management System. It is built with plain
HTML, CSS, and vanilla JavaScript — **no build step, no npm, no framework**. It talks
to the Spring Boot REST API at `http://localhost:8080/api`.

## Pages

| File            | Purpose                                          |
| --------------- | ------------------------------------------------ |
| `index.html`    | Redirects to `book.html`                          |
| `book.html`     | Book a Trip — pick a route + date, reserve a seat |
| `map.html`      | Live Trip Map — Leaflet map of routes             |
| `bookings.html` | My Bookings — list, filter, view details, cancel  |
| `profile.html`  | My Profile — view/edit student details            |

## Structure

```
css/styles.css     All styling (design tokens + components)
js/config.js       API base URL + hardcoded STUDENT_ID
js/api.js          fetch wrappers for the REST API
js/layout.js       Injects the shared sidebar + top bar
js/book.js         Per-page logic
js/map.js
js/bookings.js
js/profile.js
```

Leaflet (used by the map page) is loaded from a CDN in `map.html`.

## Running

No build required. Start the Spring Boot backend on port 8080, then serve this folder
with any static server, for example:

```bash
python3 -m http.server 5500
```

Then open <http://localhost:5500/book.html>. (VS Code "Live Server" also works.)

The backend enables CORS for all origins, so no extra configuration is needed.

> Note: opening the files directly via `file://` may cause the API `fetch` calls to be
> blocked — use a static server as shown above.
