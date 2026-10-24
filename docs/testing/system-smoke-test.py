"""System-level smoke test for the Online Personal Education Platform.

Boots nothing by itself: point it at a RUNNING backend (H2 or MySQL) and it
exercises the main user journey over real HTTP:

    health -> register -> duplicate register -> bad login -> login ->
    /me -> course list -> enrol -> progress -> quiz detail (answers hidden) ->
    quiz submit (auto graded) -> attempt review -> recommendations

Usage:
    python system-smoke-test.py [base_url]

Exit code 0 = every check passed; 1 = at least one check failed.
"""

import json
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:18080"
SUFFIX = str(int(time.time()))[-6:]  # keeps the script re-runnable

results = []


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as err:
        try:
            return err.code, json.loads(err.read().decode())
        except Exception:
            return err.code, {}


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("PASS  " if ok else "FAIL  ") + name + ("  -> " + detail if detail else ""))


# 1. health
code, body = call("GET", "/api/health")
check("SYS-01 health endpoint returns UP",
      code == 200 and body.get("data", {}).get("status") == "UP", str(body.get("data", {}).get("status")))

# 2. register a brand new student
code, body = call("POST", "/api/auth/register",
                  {"username": "sys_tester" + SUFFIX, "password": "Passw0rd!",
                   "realName": "System Tester", "email": "sys_tester@example.com"})
check("SYS-02 register a new student", code == 200, str(body.get("message")))

# 3. duplicate registration is rejected
code, body = call("POST", "/api/auth/register",
                  {"username": "sys_tester" + SUFFIX, "password": "Passw0rd!",
                   "realName": "Dup", "email": "dup@example.com"})
check("SYS-03 duplicate registration is rejected", code != 200 or body.get("code") != 200,
      "code=%s message=%s" % (code, body.get("message")))

# 4. wrong password is rejected
code, body = call("POST", "/api/auth/login", {"username": "sys_tester" + SUFFIX, "password": "wrong!"})
check("SYS-04 wrong password is rejected", code != 200 or body.get("code") != 200,
      "code=%s" % code)

# 5. sign in with the demo student account
code, body = call("POST", "/api/auth/login", {"username": "student", "password": "student123"})
token = (body.get("data") or {}).get("token", "")
check("SYS-05 sign in with demo student returns a JWT", code == 200 and token != "",
      "expiresIn=%s" % (body.get("data") or {}).get("expiresIn"))

# 6. /me shows the signed-in identity
code, body = call("GET", "/api/auth/me", token=token)
check("SYS-06 /me returns the signed-in user",
      code == 200 and (body.get("data") or {}).get("username") == "student",
      str((body.get("data") or {}).get("username")))

# 7. protected endpoint without a token is unauthorized
code, body = call("GET", "/api/auth/me")
check("SYS-07 protected endpoint without token -> 401", code == 401 or body.get("code") == 401,
      "code=%s" % code)

# 8. course catalogue is readable
code, body = call("GET", "/api/courses?page=1&size=50", token=token)
courses = ((body.get("data") or {}).get("records")
           or (body.get("data") or {}).get("list") or [])
published = [c for c in courses if c.get("status") == 1]
check("SYS-08 course catalogue lists published courses", code == 200 and len(published) > 0,
      "%d published courses" % len(published))
course = published[0]
course_id = course["id"]
count_before = course.get("studentCount")

# The rest of the journey runs as the freshly registered student so the
# script can be re-run against the same database.
code, body = call("POST", "/api/auth/login", {"username": "sys_tester" + SUFFIX, "password": "Passw0rd!"})
tester_token = (body.get("data") or {}).get("token", "")

# 9. enrol in the first course
code, body = call("POST", "/api/enrollments", {"courseId": course_id}, token=tester_token)
check("SYS-09 enrol in a published course", code == 200, str(body.get("message")))

# 10. studentCount increases by exactly one after enrolment
code, body = call("GET", "/api/courses/%s" % course_id, token=tester_token)
count_after = ((body.get("data") or {}).get("course") or {}).get("studentCount")
check("SYS-10 student counter incremented after enrolment",
      count_after == (count_before or 0) + 1,
      "before=%s after=%s" % (count_before, count_after))

# 11. progress endpoint works for the new enrolment
code, body = call("GET", "/api/progress/course/%s" % course_id, token=tester_token)
progress = body.get("data") or {}
check("SYS-11 progress breakdown of the new enrolment",
      code == 200 and progress.get("courseId") == course_id,
      "lessons=%s" % len(progress.get("lessons") or []))

# 12. quiz list of the course
code, body = call("GET", "/api/quizzes/course/%s" % course_id, token=tester_token)
quizzes = body.get("data") or []
check("SYS-12 course has a published quiz", code == 200 and len(quizzes) > 0,
      "%d quizzes" % len(quizzes))
quiz = quizzes[0]
quiz_id = quiz.get("id") or quiz.get("quizId")

# 13. quiz detail hides correct answers from the student
code, body = call("GET", "/api/quizzes/%s" % quiz_id, token=tester_token)
detail = body.get("data") or {}
questions = detail.get("questions") or []
leaked = any(q.get("correctAnswer") for q in questions)
check("SYS-13 quiz detail hides correct answers", code == 200 and questions and not leaked,
      "%d questions, leaked=%s" % (len(questions), leaked))

# 14. submit answers and get auto grading
answers = []
for index, q in enumerate(questions):
    answer = "A" if index % 3 != 2 else "F"   # deliberately imperfect
    answers.append({"questionId": q["id"], "answer": answer})
code, body = call("POST", "/api/quizzes/%s/submit" % quiz_id,
                  {"answers": answers, "durationSeconds": 120}, token=tester_token)
result = body.get("data") or {}
check("SYS-14 quiz submission is auto graded",
      code == 200 and result.get("totalScore", 0) > 0,
      "score=%s/%s passed=%s" % (result.get("score"), result.get("totalScore"), result.get("passed")))

# 15. the attempt is stored and reviewable
code, body = call("GET", "/api/attempts/%s" % result.get("attemptId"), token=tester_token)
review = body.get("data") or {}
check("SYS-15 stored attempt can be reviewed",
      code == 200 and review.get("score") == result.get("score")
      and len(review.get("details") or []) == len(questions),
      "attemptId=%s" % result.get("attemptId"))

# 16. recommendation engine answers for this student
code, body = call("GET", "/api/recommendations?limit=6", token=tester_token)
recs = body.get("data") or []
check("SYS-16 recommendations returned with reasons",
      code == 200 and len(recs) > 0 and all(r.get("reason") for r in recs),
      "%d recommendations" % len(recs))

# 17. dropping the course decrements the counter again (mirror of SYS-10)
code, body = call("DELETE", "/api/enrollments/%s" % course_id, token=tester_token)
check("SYS-17 drop the enrolled course", code == 200, str(body.get("message")))
code, body = call("GET", "/api/courses/%s" % course_id, token=tester_token)
count_dropped = ((body.get("data") or {}).get("course") or {}).get("studentCount")
check("SYS-18 student counter decremented after dropping",
      count_dropped == count_before,
      "before=%s after-drop=%s" % (count_before, count_dropped))

# 19. a student who is not enrolled is forbidden to submit the quiz over HTTP
code, body = call("POST", "/api/auth/register",
                  {"username": "sys_taker" + SUFFIX, "password": "Passw0rd!",
                   "realName": "Quiz Taker", "email": "sys_taker@example.com"})
code, body = call("POST", "/api/auth/login", {"username": "sys_taker" + SUFFIX, "password": "Passw0rd!"})
taker_token = (body.get("data") or {}).get("token", "")
code, body = call("POST", "/api/quizzes/%s/submit" % quiz_id,
                  {"answers": answers, "durationSeconds": 30}, token=taker_token)
check("SYS-19 un-enrolled student is forbidden to submit the quiz",
      code == 403 or (body.get("code") == 403),
      "code=%s message=%s" % (code, body.get("message")))

# 20. a STUDENT account cannot reach the admin endpoints
code, body = call("GET", "/api/admin/users?page=1&size=10", token=token)
check("SYS-20 student account blocked from admin endpoints",
      code == 403 or (body.get("code") == 403),
      "code=%s" % code)

# ------------------------------------------------------------------ summary
failed = [r for r in results if not r[1]]
print()
print("System smoke test: %d checks, %d passed, %d failed"
      % (len(results), len(results) - len(failed), len(failed)))
sys.exit(1 if failed else 0)
