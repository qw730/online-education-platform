# Sprint 1 — Retrospective

**Scope.** Sprint 1 stops at the door: register → login → the pages behind the guard.
Everything past that point is deferred to Sprint 2.

Delivered:

- Register + login end to end — `POST /api/auth/register`, `POST /api/auth/login`, BCrypt, JWT in `localStorage`.
- `AuthInterceptor` + `UserContext`: `/login`, `/register` and 404 are the only public routes; the other **19 pages** (11 student + 8 admin) sit behind the guard.
- Authenticated shell: layout, nav, route guard, axios interceptor that attaches `Bearer` and clears the session on 401.

## Went well

- The contract was frozen in week 1 — `{ code, message, data }` envelope, `Bearer` header, `redirect` query on the guard — and never changed, so no page needed rework.
- Session rules live in exactly two places (router guard + axios interceptor); no view touches a token itself.
- The slice is fully covered by tests: register → login chain, 401 without a token, 403 for a STUDENT on `/api/admin/**`.
- Verified against the running app, not just in tests: both demo accounts log in, the guard redirects, the admin-only pages reject a student.

## Did not go well

- The 19 authenticated pages were built against the API, so none of them renders without the backend and database running — the demo depends entirely on the live stack.
- Diagrams were left to the last day; the C4 set entered the review incomplete and had to be finished afterwards.
- The JWT secret is still the dev default in `application.yml`; hardening was never written down as a subtask.
- Only two seed accounts exist, so the admin and student views stay thinner than the UI suggests.

## Actions for Sprint 2

| # | Action | Owner | Done when |
| --- | --- | --- | --- |
| 1 | Fill the pages: catalogue, enrolment, progress, quizzes, recommendations behind the screens that already exist | AI | every menu entry has real data |
| 2 | Externalise secrets (`JWT_SECRET`, `DB_PASSWORD`) — the env override already works, make it the default | B | no secret in git |
| 3 | Keep each diagram in the same subtask as the code it describes | AI + A | **done** — `docs/system-*-diagram.*` |
| 4 | Confirmation dialog before every admin destructive action | C | every delete asks first |
| 5 | Seed enough demo data to show every page, and test continuously rather than after code freeze | C | no post-freeze test phase |
