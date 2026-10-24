# 5.x Testing

## Overview

The project was verified with a three-layer testing strategy following the classic
testing pyramid. Unit tests (JUnit 5 + Mockito) verify the business rules of single
classes without any Spring context; integration tests (`@SpringBootTest` + MockMvc)
exercise the full REST stack — controller, service, repository — against an in-memory
H2 database in MySQL compatibility mode; system tests execute a black-box HTTP smoke
suite against a real running Tomcat server, following the complete user journey
(register → login → enrol → study → quiz → recommendation). In total **66 test items
were executed and all 66 passed (0 failures)**, covering all five features of the
specification: authentication, enrolment & learning progress, auto-graded quizzes,
course recommendation and admin management. The denormalised course student counter —
a previously fixed defect — is regression-guarded at both the unit level and the
system level.

## Unit Testing (26 cases, 26 passed)

| Test Class | Case | Scenario | Result |
| --- | --- | --- | --- |
| JwtUtilTest | roundTrip | Token issue + parse keeps userId / username / role | ✅ |
| JwtUtilTest | tamperedTokenIsRejected | Token signed with a different key is rejected | ✅ |
| JwtUtilTest | expiredTokenIsRejected | Expired token → unauthorized | ✅ |
| JwtUtilTest | wrongIssuerIsRejected | Token from a foreign issuer is rejected | ✅ |
| AuthServiceTest | registerStoresHashedPasswordAndStudentRole | Only bcrypt hash stored, role forced to STUDENT | ✅ |
| AuthServiceTest | duplicateUsernameIsRejected | Taken username rejected, nothing written | ✅ |
| AuthServiceTest | disabledAccountCannotLogin | Disabled account blocked even with correct password | ✅ |
| AuthServiceTest | passwordChangeVerifiesCurrentPassword | Wrong old password rejected; correct one re-hashes | ✅ |
| QuizServiceTest | gradesMixedQuestionTypes | SINGLE case-insensitive, MULTIPLE order-independent, TRUE_FALSE wrong | ✅ |
| QuizServiceTest | failingSubmissionIsNotPassed | 25% < 60% pass line → not passed | ✅ |
| QuizServiceTest | blankAnswerScoresZero | Blank answers score zero instead of failing | ✅ |
| QuizServiceTest | draftQuizIsRejected | Draft quiz cannot be submitted | ✅ |
| QuizServiceTest | notEnrolledIsForbidden | Un-enrolled student → 403 | ✅ |
| QuizServiceTest | droppedEnrolmentBlocksSubmission | Dropped enrolment → 403 | ✅ |
| QuizServiceTest | maxAttemptsIsEnforced | Attempt limit enforced | ✅ |
| QuizServiceTest | emptyQuizIsRejected | Quiz without questions rejected | ✅ |
| QuizServiceTest | trueFalseAnswerNormalisation | Admin input "true" normalised to "T" | ✅ |
| RecommendationServiceTest | coldStartFallsBackToPopularCourses | New account gets popular courses | ✅ |
| RecommendationServiceTest | scoresCoursesByInterestProfile | Formula verified: 10×category + 6×tag + popularity + 2×rating | ✅ |
| RecommendationServiceTest | limitIsClamped | Invalid limit clamped, no crash | ✅ |
| RecommendationServiceTest | noMatchFallsBackToPopular | No profile match → popular fallback | ✅ |
| EnrollmentServiceTest | newEnrolmentRecalculatesCounter | Counter equals real non-dropped enrolments | ✅ |
| EnrollmentServiceTest | reEnrolmentRecalculatesCounter | Re-enrolment recalculates the counter | ✅ |
| EnrollmentServiceTest | droppingRecalculatesCounter | Drop recalculates instead of blind decrement | ✅ |
| EnrollmentServiceTest | draftCourseIsRejected | Draft course cannot be enrolled | ✅ |
| EnrollmentServiceTest | doubleEnrolmentIsRejected | Duplicate enrolment rejected | ✅ |

## Integration Testing (20 cases, 20 passed)

| # | Scenario | Aspect Verified | Result |
| --- | --- | --- | --- |
| 1 | Health endpoint is public and reports UP | Health check | ✅ |
| 2 | Course catalogue readable without a token | Public read-only API | ✅ |
| 3 | Registration creates a STUDENT that can sign in | Auth closed loop | ✅ |
| 4 | Weak password rejected with 400 | Bean validation | ✅ |
| 5 | Bad password rejected with 401 | Authentication failure | ✅ |
| 6 | Student cannot reach admin endpoints | Role-based access | ✅ |
| 7 | Enrol → tick chapter → progress rises | Learning progress chain | ✅ |
| 8 | Completing every chapter marks COMPLETED | Progress state machine | ✅ |
| 9 | Study plan created per chapter, tasks tickable | Study plan feature | ✅ |
| 10 | Study plan requires enrolment | Business constraint | ✅ |
| 11 | Student dashboard aggregates counters | Aggregate query | ✅ |
| 12 | All correct answers → full score and pass | Grading happy path | ✅ |
| 13 | Wrong answers score zero, attempt reviewable | Grading + attempt storage | ✅ |
| 14 | Multiple choice accepts answers in any order | Grading rule detail | ✅ |
| 15 | Quiz history is paginated | Pagination contract | ✅ |
| 16 | Recommendations relate to studied courses | Recommendation integration | ✅ |
| 17 | New account still gets suggestions | Cold-start fallback | ✅ |
| 18 | Admin creates course, adds chapters, publishes | Admin CRUD | ✅ |
| 19 | Admin dashboard reports platform statistics | Statistics aggregate | ✅ |
| 20 | Admin lists accounts and disables a student | Account management | ✅ |

## System Testing (20 checks, 20 passed)

Black-box HTTP suite (`docs/testing/system-smoke-test.py`) against a real Tomcat
instance; frontend production build (`vite build`) also passed.

| # | Check | Expected | Result |
| --- | --- | --- | --- |
| SYS-01 | GET /api/health | status UP | ✅ |
| SYS-02 | Register a new student | 200 | ✅ |
| SYS-03 | Duplicate registration | 400 rejected | ✅ |
| SYS-04 | Wrong password login | 401 | ✅ |
| SYS-05 | Demo student login | JWT issued, expires in 3600 s | ✅ |
| SYS-06 | GET /me with JWT | Identity "student" returned | ✅ |
| SYS-07 | GET /me without token | 401 | ✅ |
| SYS-08 | Course catalogue | 9 published courses | ✅ |
| SYS-09 | Enrol in a course | 200 | ✅ |
| SYS-10 | studentCount after enrolment | Exactly +1 (1 → 2) | ✅ |
| SYS-11 | Progress breakdown of new enrolment | 4 lessons listed | ✅ |
| SYS-12 | Quiz list of the course | 1 published quiz | ✅ |
| SYS-13 | Quiz detail hides correct answers | No answer leaked | ✅ |
| SYS-14 | Quiz submission auto-graded | 10/50, passed=false | ✅ |
| SYS-15 | Stored attempt reviewable | Per-question detail consistent | ✅ |
| SYS-16 | Recommendations | 6 items with reasons | ✅ |
| SYS-17 | Drop the enrolled course | 200 | ✅ |
| SYS-18 | studentCount after drop | Back to pre-enrol value (2 → 1) | ✅ |
| SYS-19 | Un-enrolled student submits quiz | 403 forbidden | ✅ |
| SYS-20 | Student reaches admin endpoint | 403 forbidden | ✅ |
