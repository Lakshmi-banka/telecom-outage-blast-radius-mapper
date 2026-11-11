# Telecom Outage Blast-Radius Mapper — React Frontend

React 18 + Vite + React Router + Axios. No UI framework, no chart library —
the dependency graph is hand-drawn SVG, so `npm install` pulls 90 packages and
nothing can break on a version bump.

Built against the `telecom-outage-backend` Spring Boot API (port 8080), which in
turn calls the FastAPI engine (port 8001).

---

## 1. Prerequisites

| Thing | Version | Must be running |
|---|---|---|
| Node.js | 18 or newer | — |
| Spring Boot backend | — | `http://localhost:8080` |
| Python FastAPI engine | — | `http://127.0.0.1:8001` |
| PostgreSQL | — | with your `telecom` schema seeded |

## 2. Install and run

```cmd
cd telecom-outage-frontend
npm install
npm run dev
```

Open **http://localhost:5173** and sign in with a user from your `users` table.

Production build:

```cmd
npm run build      :: outputs to dist/
npm run preview    :: serves dist/ locally
```

---

## 3. How it talks to the backend

The Vite dev server **proxies `/api` to `http://localhost:8080`** (see
`vite.config.js`). The browser therefore only ever talks to `localhost:5173`,
so **CORS never applies during development** — no backend change needed.

```
Browser :5173  ──/api/*──►  Vite proxy  ──►  Spring Boot :8080  ──►  FastAPI :8001
                                                      │
                                                 PostgreSQL
```

To bypass the proxy and call Spring directly, copy `.env.example` to `.env` and set:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

Your `SecurityConfig` already allows origin `http://localhost:5173`, so this works
too — the proxy is just one less moving part.

### Authentication

`POST /api/auth/login` returns `{ token, tokenType, email, role }`. The token is
stored in `localStorage` and attached as `Authorization: Bearer <token>` by an
Axios request interceptor (`src/api/client.js`). A `401` clears the token and
bounces to the login screen.

Role names (`ADMIN`, `NOC_MANAGER`, `NOC_ANALYST`, `NETWORK_ENGINEER`) come from
the login response and drive which buttons appear. **The UI only hides controls —
`@PreAuthorize` on the backend is what actually enforces access.**

---

## 4. Screens and the endpoints behind them

| Screen | Endpoints |
|---|---|
| Login | `POST /api/auth/login` |
| Dashboard | `/api/incidents`, `/api/components`, `/api/alarms`, `/api/services`, `/api/network/dependencies`, `/api/python/health` |
| Incidents | `GET/POST /api/incidents`, `PATCH /api/incidents/{id}/status` |
| **Incident detail** | `POST /api/incidents/{id}/blast-radius` (BFS), `POST /api/incidents/{id}/blast-radius/dfs` |
| Components | `GET/POST /api/components`, `PATCH /api/components/{id}/status` |
| Network | `GET /api/network/dependencies`, `GET /api/services` |
| Alarms | `GET /api/alarms`, `GET /api/alarms/status/{status}`, `POST /api/alarms/correlate` |
| Simulation | `POST /api/simulations/outage` |
| Restorations | `GET/POST /api/restorations`, `PATCH /api/restorations/{id}/status` |
| Admin Data (ADMIN only) | `GET /api/admin/data/{table}` |

The **Incident detail** screen is the centrepiece: pick an incident, run BFS or
DFS, and it renders the affected components, affected services, estimated
customer impact, the traversal order, and a topology graph with the root cause in
red and everything downstream in amber.

---

## 5. REQUIRED backend patch (`backend-patch/`)

Four of these screens will return **HTTP 500** against the backend as it stands.
Not a frontend bug — a JPA serialization one, and it is verified, not theoretical:

```
HttpMessageNotWritableException: Could not write JSON:
Could not initialize proxy [Component#1] - no session
```

`application.properties` sets `spring.jpa.open-in-view=false`, so the Hibernate
session is closed by the time Jackson serializes the response. Any entity
returned straight to Jackson with a `@ManyToOne(fetch = LAZY)` relation blows up.

Affected: `/api/incidents`, `/api/alarms`, `/api/network/dependencies`,
`/api/restorations`.
Unaffected: `/api/components`, `/api/services`, `/api/admin/data/*`, and every
blast-radius / simulation / correlation endpoint (those return `Map`, not entities).

Copy the five files from `backend-patch/` over
`src/main/java/com/telecom/outage/entity/`, then `mvn clean package`.

They switch the relations the UI reads to `FetchType.EAGER`, and add `@JsonIgnore`
to `Incident.createdBy`, `RestorationHistory.performedBy` and
`User.passwordHash` — the last one closes a real hole, since a bcrypt hash would
otherwise be serialized to the browser.

There are no bidirectional mappings in the schema, so EAGER introduces no
serialization cycles. At 8 components / 5 dependencies the extra joins cost nothing.

---

## 6. Project layout

```
src/
  api/client.js            Axios instance, JWT interceptor, every endpoint wrapper
  auth/AuthContext.jsx     login / logout / role checks, localStorage persistence
  components/
    Layout.jsx             sidebar, topbar, live Python-engine indicator
    TopologyGraph.jsx      hand-rolled SVG dependency graph
    ProtectedRoute.jsx     redirects to /login when signed out
    Badge.jsx  DataTable.jsx  Spinner.jsx  ErrorBox.jsx
  pages/                   one file per screen
  styles.css               single dark NOC-style stylesheet
```

## 7. Troubleshooting

| Symptom | Cause |
|---|---|
| "Cannot reach the backend" | Spring Boot not running on 8080 |
| Sidebar shows `Python engine: DOWN` | FastAPI not running on 8001, or `python.engine.base-url` wrong |
| 500 on Incidents / Alarms / Network / Restorations | Backend patch in step 5 not applied |
| 403 on an action | Your role lacks it — check `@PreAuthorize` on the controller |
| Login works, everything else 401 | JWT expired (`app.jwt.expiration-ms` is 1 hour); sign in again |
