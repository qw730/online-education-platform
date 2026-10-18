# Online Personal Education Platform

An online learning platform for individual learners, built as a classic three tier
application.

| Layer     | Technology                                                        |
| --------- | ----------------------------------------------------------------- |
| Frontend  | Vue 3 (Composition API) · Vite · Element Plus · Pinia · Vue Router |
| Backend   | Spring Boot 3.3 · Spring Data JPA · Spring Validation · JJWT       |
| Database  | MySQL 8 (utf8mb4)                                                  |
| Build     | Maven 3.9 (wrapper included) · npm                                |

---

## 1. Features

| # | Requirement                                   | Where to find it                                                                 |
| - | --------------------------------------------- | -------------------------------------------------------------------------------- |
| 1 | Registration / sign in with STUDENT and ADMIN  | `AuthService`, `AuthController`, `views/LoginView.vue`, `views/RegisterView.vue` |
| 2 | Browse courses, enrol, build study plans, track progress | `CourseService`, `EnrollmentService`, `StudyPlanService`, `views/CourseListView.vue`, `views/LearningView.vue`, `views/StudyPlansView.vue`, `views/ProgressView.vue` |
| 3 | Online quiz with automatic scoring            | `QuizService`, `views/QuizView.vue`, `views/QuizResultView.vue`                  |
| 4 | Course recommendation from learning history   | `RecommendationService`, shown on `views/HomeView.vue`                           |
| 5 | Admin creates / edits course information      | `AdminCourseController`, `AdminQuizController`, `views/admin/*`                  |

### How the quiz is graded

`POST /api/quizzes/{id}/submit` receives the answers, compares them with the stored
correct answer of every question, adds up the score of the correct ones and stores a
`quiz_attempt` row. The response already contains the score, the pass/fail flag and a
per-question review (your answer, the correct answer and the explanation).

* `SINGLE` / `TRUE_FALSE` — exact match after trimming and upper-casing.
* `MULTIPLE` — set equality, so `C,A` and `A,C` are both accepted.

### How the recommendation works

1. Build an interest profile from all courses the student is enrolled in
   (category weights and keyword weights).
2. Score every published course that the student has **not** enrolled in yet:

   ```
   score = 10 × (times you already study this category)
         +  6 × (sum of the weights of the keywords shared with your courses)
         +  min(studentCount, 100) / 10
         +  2 × rating
   ```

3. Return the top N with a human readable reason such as
   *"Matches the topics you are learning: vue, frontend"*.
4. A brand new account with no history gets the most popular courses instead
   (cold-start fallback).

---

## 2. Project structure

```
online-education-platform/
├── backend/                      Spring Boot service
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/edu/platform/
│       │   ├── EduPlatformApplication.java
│       │   ├── common/           Result, PageResult, BizException, global handler
│       │   ├── config/           WebConfig (CORS + interceptor), DemoDataInitializer
│       │   ├── controller/       REST endpoints
│       │   ├── domain/
│       │   │   ├── entity/       12 JPA entities
│       │   │   └── enums/        Role, CourseLevel, EnrollmentStatus, ...
│       │   ├── dto/              request / response records
│       │   ├── repository/       Spring Data JPA repositories
│       │   ├── security/         JWT properties, JwtUtil, AuthInterceptor, UserContext
│       │   ├── service/          business logic
│       │   └── support/          DtoMapper
│       └── resources/application.yml
├── frontend/                     Vue 3 single page application
│   ├── package.json
│   ├── vite.config.js            dev proxy /api -> http://localhost:8080
│   └── src/
│       ├── api/                  axios instance + endpoint wrappers
│       ├── components/           CourseCard, StatCard, EmptyState
│       ├── layouts/MainLayout.vue
│       ├── router/index.js       routes + auth/admin guards
│       ├── stores/user.js        Pinia store, token persisted in localStorage
│       ├── styles/global.css
│       ├── utils/format.js
│       └── views/                20 pages incl. views/admin/*
└── sql/schema.sql                MySQL DDL script
```

---

## 3. Database design

12 tables, all InnoDB / utf8mb4:

| Table              | Purpose                                                |
| ------------------ | ------------------------------------------------------ |
| `sys_user`         | accounts, with the `role` column (STUDENT / ADMIN)      |
| `category`         | course categories                                       |
| `course`           | courses, with `tags` used by the recommender            |
| `lesson`           | chapters of a course, drive the progress                |
| `enrollment`       | enrolment records + cached progress counters            |
| `lesson_progress`  | which chapter a student has finished                    |
| `study_plan`       | personal study plan per course                          |
| `study_plan_item`  | dated tasks inside a plan                               |
| `quiz`             | quiz attached to a course                               |
| `question`         | question with type, correct answer, score, explanation  |
| `question_option`  | selectable options (A/B/C/D, or T/F)                    |
| `quiz_attempt`     | one submission with score and raw answers as JSON       |

Relationships: `course → category`, `lesson → course`, `enrollment → (user, course)`,
`question → quiz`, `question_option → question`, `quiz_attempt → (user, quiz, course)`.

---

## 4. Getting started

### 4.1 Prerequisites

* JDK 17 or newer (tested on JDK 22)
* MySQL 8 running on `localhost:3306`
* Node.js 18+ and npm

Maven is **not** required — the backend ships with the Maven Wrapper
(`mvnw` / `mvnw.cmd`), which downloads the correct Maven version on first run.

### 4.1.1 Opening the project in an IDE

The project is plain text, so any editor works. Nothing is generated by a
specific tool, and there are no IDE lock files. Two convenient setups:

**IntelliJ IDEA (recommended for the backend)**

1. `File > Open…` and select the `backend/pom.xml` file (choose *Open as
   Project*). IDEA reads the Maven model and indexes the sources automatically.
2. Make sure the project SDK is JDK 17+ (`File > Project Structure > Project >
   SDK`). The `pom.xml` targets Java 17.
3. Press ▶ on `EduPlatformApplication`, or use the Maven tool window to run
   `spring-boot:run`.
4. To work on the frontend in the same window, open the repository root instead
   and add `frontend` as a module — or simply open it in VS Code (below).

> Note: IDEA 2021.3 does not fully understand Java 22 syntax; if you use that
> version, point the project SDK at JDK 17/21. Newer IDEA versions handle
> JDK 22 directly. Compiling with Maven is unaffected either way.

**VS Code (recommended for the frontend)**

A ready-made multi-root workspace is included. Double-click
`online-education-platform.code-workspace`, or run:

```bash
code online-education-platform.code-workspace
```

VS Code will prompt you to install the recommended extensions (Java Pack,
Spring Boot Pack, Vue - Official). `.vscode/settings.json` already points the
Java language server at `D:\Java\jdk-22` — change that path if your JDK lives
somewhere else.

**Eclipse**

`File > Import > Maven > Existing Maven Projects`, then pick `backend`. Install
the Vue tooling separately if you want frontend support.

Any of these is optional: everything in section 4.4 / 4.5 also works from a
plain terminal.

### 4.2 Create the database

Two options — pick one.

**Option A (recommended): let the application create everything**

`application.yml` already contains `createDatabaseIfNotExist=true` and
`ddl-auto: update`, so the schema is created on the first start and the demo data is
inserted automatically.

**Option B: run the SQL script yourself**

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS online_education \
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p --default-character-set=utf8mb4 online_education < sql/schema.sql
```

The script only creates the 12 tables (no `INSERT`s); the demo data is still seeded by
the application on startup. Afterwards you can set `spring.jpa.hibernate.ddl-auto` to
`none` in `backend/src/main/resources/application.yml` so Hibernate never touches the
schema again.

### 4.3 Configure the connection

`backend/src/main/resources/application.yml` already points at the local development
database and falls back to the password used on this machine:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/online_education?...&createDatabaseIfNotExist=true
    username: root
    password: "${DB_PASSWORD:xskllx060623@}"
```

To use a different account, either edit that line or keep the file untouched and pass
environment variables — the env var always wins:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password_here
export DB_NAME=online_education
```

or pass them straight to the start command, which is handy on Windows:

```bash
cd backend
DB_PASSWORD=your_password ./mvnw spring-boot:run
# Windows cmd:  set DB_PASSWORD=your_password && mvnw.cmd spring-boot:run
# PowerShell:   $env:DB_PASSWORD="your_password"; .\mvnw.cmd spring-boot:run
```

### 4.4 Start the backend

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API is then available on <http://localhost:8080>. Quick check:

```bash
curl http://localhost:8080/api/health
```

### 4.5 Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `http://localhost:8080`, so no
CORS configuration is needed during development.

### 4.6 Demo accounts

Created automatically on the first start when `app.init-demo-data` is `true`:

| Role          | Username  | Password     |
| ------------- | --------- | ------------ |
| Administrator | `admin`   | `admin123`   |
| Student       | `student` | `student123` |

The student account already has history so the recommendation engine has something to
work with: **Java Programming Fundamentals** (3 of 6 chapters done, plus a four week
study plan), **Vue 3 Front-end Engineering** (just started) and
**Personal Productivity & Study Skills** (finished, 100%).

That history is deliberately narrow — `MySQL Database Design` and
`Spring Boot REST API Development` are left un-enrolled on purpose. Both share the
`backend` / `java` keywords with the Java course, so the home page shows real
history-driven suggestions such as
*"Matches the topics you are learning: java, backend"* instead of the popularity
fallback.

The seed data covers 10 courses, 48 chapters, 4 quizzes with 21 questions and 76 options.

### 4.7 Troubleshooting

* **The backend starts on a random port** — if a `SERVER__PORT` / `SERVER_PORT`
  environment variable is set in your shell (some IDEs and agent tooling do this), it
  overrides `application.yml`. Pass the port explicitly:
  `mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`.
* **`Access denied for user 'root'@'localhost'`** — no database password is committed
  to the repository. Export `DB_PASSWORD` before starting the backend, e.g.
  `DB_PASSWORD=secret ./mvnw spring-boot:run`.
* **Re-seeding from scratch** — the initialiser only runs when `sys_user` is empty, so
  empty the tables and restart the backend to get a pristine demo dataset.

---

## 5. Automated tests

`backend/src/test/java/com/edu/platform/PlatformApiIntegrationTest.java` boots the whole
Spring context against an in-memory H2 database and drives the real REST endpoints with
MockMvc. It covers every requirement of the specification:

* public endpoints (health, catalogue) work without a token
* registration, sign in, validation errors and the STUDENT / ADMIN split
* a student cannot reach `/api/admin/**` (403)
* enrol → tick chapters → progress rises → course becomes COMPLETED
* study plan creation with auto generated tasks, ticking, adding and deleting tasks
* quiz submission: full marks, zero marks, order independent multiple choice,
  answers hidden before submitting, attempt review
* recommendations follow the learning history and never repeat an enrolled course
* cold-start recommendations for a brand new account
* admin creating a course, adding chapters, creating a quiz with a question,
  publishing, and cascading delete
* admin statistics and enabling / disabling an account

Run them with:

```bash
cd backend
./mvnw test          # Windows: mvnw.cmd test
```

No MySQL server is required for the tests. **Status: 20 tests, 0 failures.**

### Continuous integration

Two GitHub Actions workflows live in `.github/workflows/`:

| Workflow | Trigger | What it does |
| -------- | ------- | ------------ |
| `ci.yml` | every push / pull request on `main` | runs the backend tests on JDK 17, packages the jar, installs the frontend from `package-lock.json` (Node 20) and builds it with Vite; both bundles are kept as workflow artifacts for 14 days |
| `release.yml` | pushing a tag like `v1.0.0`, or manually with a tag name | runs the full build and tests, then publishes a GitHub Release with `online-education-platform-<tag>.jar` and `frontend-dist-<tag>.zip` attached |

Cutting a release therefore only takes a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 6. REST API

All responses use the envelope `{ "code": 200, "message": "success", "data": ... }`.
Endpoints marked **JWT** need the header `Authorization: Bearer <token>`.

### Authentication

| Method | Path                  | Notes                              |
| ------ | --------------------- | ---------------------------------- |
| POST   | `/api/auth/register`  | public, always creates a STUDENT   |
| POST   | `/api/auth/login`     | public, returns the JWT + profile  |
| GET    | `/api/auth/me`        | JWT                                |
| PUT    | `/api/auth/profile`   | JWT                                |
| PUT    | `/api/auth/password`  | JWT                                |

### Catalogue

| Method | Path                        | Notes                                          |
| ------ | --------------------------- | ---------------------------------------------- |
| GET    | `/api/categories`           | public                                         |
| GET    | `/api/courses`              | public. `keyword`, `categoryId`, `level`, `sortBy`, `page`, `size` |
| GET    | `/api/courses/{id}`         | public. detail + chapters + quizzes + my progress |

### Learning (student)

| Method | Path                              | Notes                                  |
| ------ | --------------------------------- | -------------------------------------- |
| POST   | `/api/enrollments`                | body `{ "courseId": 1 }`               |
| DELETE | `/api/enrollments/{courseId}`     | drop the course                        |
| GET    | `/api/enrollments/my`             | paged list                             |
| GET    | `/api/enrollments/my/all`         | full list, used by dropdowns           |
| GET    | `/api/progress/course/{courseId}` | chapter by chapter progress            |
| POST   | `/api/progress/lesson/{lessonId}` | body `{ "completed": true }`           |
| GET    | `/api/plans`                      | my study plans with their tasks        |
| POST   | `/api/plans`                      | create, `autoGenerateItems` optional   |
| PUT    | `/api/plans/{id}`                 | update                                 |
| DELETE | `/api/plans/{id}`                 | delete                                 |
| POST   | `/api/plans/{id}/items`           | add a task                             |
| PATCH  | `/api/plans/{id}/items/{itemId}`  | toggle a task                          |
| DELETE | `/api/plans/{id}/items/{itemId}`  | remove a task                          |
| GET    | `/api/recommendations?limit=6`    | recommended courses                    |
| GET    | `/api/dashboard/student`          | dashboard counters                     |

### Quiz (student)

| Method | Path                            | Notes                                        |
| ------ | ------------------------------- | -------------------------------------------- |
| GET    | `/api/quizzes/course/{courseId}`| quizzes of a course                          |
| GET    | `/api/quizzes/{id}`             | questions **without** the correct answers     |
| POST   | `/api/quizzes/{id}/submit`      | body `{ "answers": [{ "questionId": 1, "answer": "A" }], "durationSeconds": 120 }` |
| GET    | `/api/quizzes/{id}/attempts`    | my submissions of this quiz                  |
| GET    | `/api/attempts/my`              | paged quiz history                           |
| GET    | `/api/attempts/{attemptId}`     | full review of one submission                |

### Administration (role ADMIN required)

| Method | Path                                       | Notes                       |
| ------ | ------------------------------------------ | --------------------------- |
| GET    | `/api/admin/courses`                       | includes drafts             |
| POST   | `/api/admin/courses`                       | create a course             |
| PUT    | `/api/admin/courses/{id}`                  | edit a course               |
| DELETE | `/api/admin/courses/{id}`                  | delete course + dependents  |
| GET    | `/api/admin/courses/{id}/lessons`          | list chapters               |
| POST   | `/api/admin/courses/{id}/lessons`          | add a chapter               |
| PUT    | `/api/admin/lessons/{lessonId}`            | edit a chapter              |
| DELETE | `/api/admin/lessons/{lessonId}`            | delete a chapter            |
| POST   | `/api/admin/categories`                    | create a category           |
| PUT    | `/api/admin/categories/{id}`               | edit a category             |
| GET    | `/api/admin/quizzes/course/{courseId}`     | quizzes of a course         |
| POST   | `/api/admin/quizzes`                       | create a quiz               |
| PUT    | `/api/admin/quizzes/{id}`                  | edit a quiz                 |
| DELETE | `/api/admin/quizzes/{id}`                  | delete a quiz               |
| GET    | `/api/admin/quizzes/{id}/questions`        | questions **with** answers  |
| POST   | `/api/admin/quizzes/{id}/questions`        | add a question              |
| PUT    | `/api/admin/questions/{id}`                | edit a question             |
| DELETE | `/api/admin/questions/{id}`                | delete a question           |
| GET    | `/api/admin/users`                         | paged account list          |
| PATCH  | `/api/admin/users/{id}/status`             | enable / disable an account |
| GET    | `/api/dashboard/admin`                     | platform statistics         |

### Quick example

```bash
# sign in
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"student123"}' | jq -r '.data.token')

# my recommendations
curl -s http://localhost:8080/api/recommendations -H "Authorization: Bearer $TOKEN" | jq
```

---

## 7. Frontend pages

| Route                          | Page                                                    |
| ------------------------------ | ------------------------------------------------------- |
| `/login`, `/register`          | sign in / create an account                             |
| `/`                            | dashboard, recommendations, categories                  |
| `/courses`                     | catalogue with search, category / level / sort filters  |
| `/courses/:id`                 | course detail, curriculum, quizzes, enrol button        |
| `/my/courses`                  | my courses with progress                                |
| `/my/courses/:id`              | learning page: tick chapters, take quizzes              |
| `/my/plans`                    | study plans with dated tasks                            |
| `/my/progress`                 | progress table + per chapter timeline in a drawer       |
| `/my/attempts`                 | quiz history                                            |
| `/quiz/:id`                    | take a quiz (timer, single / multiple / true-false)      |
| `/quiz/result/:attemptId`      | score dial + full answer review                         |
| `/profile`                     | profile and password                                    |
| `/admin`                       | platform statistics                                     |
| `/admin/courses`               | course table with publish switch                        |
| `/admin/courses/new`           | create / edit a course                                  |
| `/admin/courses/:id/lessons`   | manage chapters                                         |
| `/admin/courses/:id/quizzes`   | manage quizzes                                          |
| `/admin/quizzes/:id/questions` | question bank editor                                    |
| `/admin/categories`            | categories                                              |
| `/admin/users`                 | accounts                                                |

---

## 8. Notes and possible extensions

* Passwords are hashed with BCrypt (`spring-security-crypto`), never stored in clear text.
* The JWT secret in `application.yml` is a development default — replace it in production,
  for example with `JWT_SECRET=...`.
* Delete operations on a course cascade to its lessons, quizzes, questions, attempts,
  enrolments and study plans to keep the data consistent.
* Ideas for a next iteration: file upload for course covers, an avatar upload, a comment
  section, e-mail notifications, pagination statistics charts, unit tests with Testcontainers.
