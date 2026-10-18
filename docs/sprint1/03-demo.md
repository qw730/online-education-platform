# Sprint 1 — Demo

Goal: walk through every user story live and close with the automated test suite.

## Preparation

```bash
cd backend  && ./mvnw spring-boot:run    # API on :8080
cd frontend && npm install && npm run dev  # SPA on :5173
```

Demo accounts (seeded automatically): `admin / admin123`, `student / student123`.
The student already has history (3 courses) so recommendations are non-trivial.

## Script (≈ 8 minutes)

| # | Step | Story | What to point out |
| --- | --- | --- | --- |
| 1 | Register a fresh account, then sign out and sign in as it | US-01 | Always STUDENT; JWT issued; 401 redirect handled by the axios interceptor |
| 2 | Open `/courses` while signed out | US-02 | Public catalogue, keyword / category / level filters, sorting, paging |
| 3 | Sign in as the new user, enrol in a course | US-03 | Course appears in My courses at 0% |
| 4 | Open the learning page and tick two chapters | US-03 | Progress counters rise; timeline drawer |
| 5 | Create a study plan with `autoGenerateItems` | US-04 | One dated task per chapter; toggle and delete a task |
| 6 | Take the course quiz and submit | US-05 | Answers hidden before submit; score dial; per-question review incl. explanations |
| 7 | Back to the home page as `student` | US-06 | Recommendations cite reasons like *"Matches the topics you are learning: java, backend"* and never repeat an enrolled course |
| 8 | Sign in as `admin` | US-07 / US-08 | Create a course (draft), add a chapter, create a quiz with one question, publish, then check `/admin` counters and enable / disable an account |

## Test evidence

```bash
cd backend && ./mvnw test
```

`PlatformApiIntegrationTest` boots the whole Spring context on in-memory H2 and drives the real REST
endpoints with MockMvc — **20 tests, 0 failures**, covering every story above including the 403 split
between STUDENT and ADMIN, order-insensitive multiple choice, and cold-start recommendations.
