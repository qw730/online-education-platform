# Sprint 1 — User stories & acceptance criteria

Owner: Program owner. Format: *As a … I want … so that …*, acceptance criteria in Given / When / Then.
Every story maps to a shipped feature of the Online Personal Education Platform.

---

**US-01 Registration and sign in** — As a visitor, I want to register and sign in, so that my learning data is kept per account. *(Role.STUDENT / Role.ADMIN)*
- Given I am on `/register`, when I submit a username (≥3), password (≥6) and nickname, then a STUDENT account is created and I am signed in.
- Given a taken username, when I register, then I get an error and no account is created.
- Given valid credentials, when I sign in, then I receive a JWT and land on the dashboard; given wrong credentials, I stay on `/login` with an error.

**US-02 Browse the catalogue** — As a visitor, I want to browse and search published courses without signing in, so that I can decide what to learn.
- Given published courses, when I open `/courses` without a token, then I see title, category, level, rating and student count.
- When I filter by keyword / category / level or change sorting / page, then the list updates accordingly.

**US-03 Enrol and track progress** — As a student, I want to enrol in a course and tick chapters, so that I can see how far I am.
- Given a published course, when I enrol, then it appears in My courses with 0% progress.
- When I tick a chapter, then the cached progress counters rise; when I tick the last chapter, the status becomes COMPLETED.
- When I drop the course, the enrolment and its progress are removed.

**US-04 Study plan** — As a student, I want a dated plan per course, so that I can schedule my learning.
- When I create a plan with `autoGenerateItems`, then one dated task per chapter is generated.
- When I toggle / add / delete a task, the change persists and tasks stay sorted by date.

**US-05 Quiz with automatic scoring** — As a student, I want to take a quiz and see my result immediately, so that I can check my understanding.
- Given a quiz, when I fetch it, then questions come without correct answers.
- When I submit, then SINGLE / TRUE_FALSE are matched exactly (trim + upper case), MULTIPLE is order-insensitive (`C,A` = `A,C`), the score is the sum of correct question scores, and I get pass/fail plus a per-question review.
- Every submission is stored as a `quiz_attempt` and appears in my quiz history.

**US-06 Recommendations** — As a student, I want course suggestions from my learning history, so that I can pick the next course.
- Given I have enrolments, when I open the home page, then recommendations are derived from my category / keyword profile and never include an enrolled course, each with a reason.
- Given a brand-new account, then the most popular courses are returned (cold-start fallback).

**US-07 Admin authors courses and quizzes** — As an administrator, I want to author courses, chapters, quizzes and questions, so that students have content to learn.
- When I create a course, it starts as a draft and is invisible to students until published.
- When I delete a course, its chapters, quizzes, questions, attempts, enrolments and plans are removed with it (cascade).

**US-08 Admin accounts and statistics** — As an administrator, I want to manage accounts and see platform counters, so that I can keep the platform healthy.
- When I open `/admin/users`, I get a paged account list and can enable / disable an account.
- `/api/dashboard/admin` returns platform statistics.
