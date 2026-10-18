package com.edu.platform;

import com.edu.platform.config.DemoDataInitializer;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.Question;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the REST API. Runs against an in-memory H2 database so it
 * needs no MySQL server. Every business feature of the specification is
 * exercised here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Online Personal Education Platform - API integration test")
class PlatformApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // @SpringBootTest does not always trigger ApplicationRunner beans, so
        // seed explicitly (the initialiser is idempotent).
        if (userRepository.count() == 0) {
            demoDataInitializer.run(null);
        }
        studentToken = login("student", "student123");
        adminToken = login("admin", "admin123");
    }

    // ==================================================================== helpers

    private String login(String username, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode getJson(String url, String token) throws Exception {
        return getJson(url, token, Map.of());
    }

    /**
     * Sends a GET request. Query values are passed with {@code param()} rather
     * than embedded in the URL, so that spaces are encoded exactly once.
     */
    private JsonNode getJson(String url, String token, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder builder = get(url)
                .header("Authorization", "Bearer " + token);
        params.forEach(builder::param);
        String response = mvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private JsonNode postJson(String url, String token, Object body) throws Exception {
        String response = mvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private Course publishedCourse(String title) {
        return courseRepository.findAll().stream()
                .filter(c -> title.equals(c.getTitle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Course not seeded: " + title));
    }

    // ============================================================== 0. public API

    @Test
    @DisplayName("the health endpoint is public and reports UP")
    void healthEndpointIsPublic() throws Exception {
        JsonNode body = getJson("/api/health", "");
        assertThat(body.path("data").path("status").asText()).isEqualTo("UP");
    }

    @Test
    @DisplayName("the course catalogue is readable without a token")
    void catalogueIsPublic() throws Exception {
        JsonNode page = getJson("/api/courses?page=1&size=5", "");
        assertThat(page.path("data").path("total").asLong()).isGreaterThan(0);
        assertThat(page.path("data").path("records")).isNotEmpty();
    }

    // ========================================================= 1. sign up / roles

    @Test
    @DisplayName("registration creates a STUDENT account that can sign in")
    void registrationCreatesStudent() throws Exception {
        String username = "stu_" + System.currentTimeMillis();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", username,
                                "password", "secret123",
                                "realName", "Test Learner",
                                "email", "learner@example.com"))))
                .andExpect(status().isOk());

        String token = login(username, "secret123");
        JsonNode me = getJson("/api/auth/me", token);
        assertThat(me.path("data").path("role").asText()).isEqualTo("STUDENT");
        assertThat(me.path("data").path("realName").asText()).isEqualTo("Test Learner");
    }

    @Test
    @DisplayName("a weak password is rejected with 400")
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", "weak_" + System.currentTimeMillis(),
                                "password", "123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a bad password is rejected with 401")
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "student", "password", "nope"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot reach the admin endpoints")
    void studentCannotReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/courses").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    // ============================================== 2. enrol, plan, track progress

    @Test
    @DisplayName("a student can enrol, tick a chapter and see the progress rise")
    void enrolAndTrackProgress() throws Exception {
        Course course = publishedCourse("Spring Boot REST API Development");
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId());

        JsonNode enrolled = postJson("/api/enrollments", studentToken, Map.of("courseId", course.getId()));
        assertThat(enrolled.path("data").path("courseTitle").asText())
                .isEqualTo("Spring Boot REST API Development");

        // Enrolling twice must fail.
        mvc.perform(post("/api/enrollments").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("courseId", course.getId()))))
                .andExpect(status().isBadRequest());

        JsonNode progress = getJson("/api/progress/course/" + course.getId(), studentToken);
        assertThat(progress.path("data").path("lessons")).hasSize(lessons.size());
        assertThat(progress.path("data").path("progressPercent").asInt()).isZero();

        // Finish the first two chapters.
        for (int i = 0; i < 2; i++) {
            postJson("/api/progress/lesson/" + lessons.get(i).getId(), studentToken,
                    Map.of("completed", true));
        }

        JsonNode after = getJson("/api/progress/course/" + course.getId(), studentToken);
        int expected = (int) Math.round(2 * 100.0 / lessons.size());
        assertThat(after.path("data").path("completedLessons").asInt()).isEqualTo(2);
        assertThat(after.path("data").path("progressPercent").asInt()).isEqualTo(expected);

        JsonNode mine = getJson("/api/enrollments/my/all", studentToken);
        assertThat(mine.path("data").toString()).contains("Spring Boot REST API Development");
    }

    @Test
    @DisplayName("completing every chapter marks the enrolment as COMPLETED")
    void completingAllChaptersCompletesTheCourse() throws Exception {
        Course course = publishedCourse("Business English Communication");
        postJson("/api/enrollments", studentToken, Map.of("courseId", course.getId()));

        for (Lesson lesson : lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
            postJson("/api/progress/lesson/" + lesson.getId(), studentToken, Map.of("completed", true));
        }

        JsonNode progress = getJson("/api/progress/course/" + course.getId(), studentToken);
        assertThat(progress.path("data").path("progressPercent").asInt()).isEqualTo(100);
        assertThat(progress.path("data").path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("a study plan is created with one task per chapter and tasks can be ticked")
    void studyPlanLifecycle() throws Exception {
        // The demo student is not enrolled in this one yet.
        Course course = publishedCourse("Data Analysis with Python");
        postJson("/api/enrollments", studentToken, Map.of("courseId", course.getId()));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("courseId", course.getId());
        request.put("title", "MySQL in two weeks");
        request.put("goal", "Be comfortable writing joins");
        request.put("startDate", "2026-10-01");
        request.put("endDate", "2026-10-14");
        request.put("dailyMinutes", 40);
        request.put("autoGenerateItems", true);

        JsonNode created = postJson("/api/plans", studentToken, request);
        long planId = created.path("data").path("id").asLong();
        int totalItems = created.path("data").path("totalItems").asInt();
        assertThat(totalItems)
                .isEqualTo(lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId()).size());

        JsonNode items = created.path("data").path("items");
        long firstItemId = items.get(0).path("id").asLong();

        mvc.perform(patch("/api/plans/" + planId + "/items/" + firstItemId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        JsonNode afterToggle = getJson("/api/plans/" + planId, studentToken);
        assertThat(afterToggle.path("data").path("doneItems").asInt()).isEqualTo(1);
        assertThat(afterToggle.path("data").path("status").asText()).isEqualTo("IN_PROGRESS");

        // A manually added task can be removed again.
        JsonNode withExtra = postJson("/api/plans/" + planId + "/items", studentToken,
                Map.of("title", "Revise normalisation", "plannedDate", "2026-10-20"));
        assertThat(withExtra.path("data").path("totalItems").asInt()).isEqualTo(totalItems + 1);
        long extraId = withExtra.path("data").path("items").get(totalItems).path("id").asLong();

        mvc.perform(delete("/api/plans/" + planId + "/items/" + extraId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/plans/" + planId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        JsonNode plans = getJson("/api/plans", studentToken);
        assertThat(plans.path("data").toString()).doesNotContain("MySQL in two weeks");
    }

    @Test
    @DisplayName("a study plan cannot be created for a course the student is not enrolled in")
    void cannotPlanANonEnrolledCourse() throws Exception {
        Course course = publishedCourse("Machine Learning Foundations");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("courseId", course.getId());
        request.put("title", "Too early");
        request.put("startDate", "2026-10-01");
        request.put("endDate", "2026-10-10");

        mvc.perform(post("/api/plans").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the student dashboard aggregates the learning counters")
    void studentDashboard() throws Exception {
        JsonNode data = getJson("/api/dashboard/student", studentToken).path("data");
        assertThat(data.path("enrolledCourses").asInt()).isGreaterThan(0);
        assertThat(data.path("completedLessons").asInt()).isGreaterThan(0);
        assertThat(data.path("recentCourses")).isNotEmpty();
    }

    // ================================================== 3. quiz with auto grading

    @Test
    @DisplayName("submitting every correct answer gives the full score and passes")
    void quizFullMarks() throws Exception {
        ensureEnrolled("Java Programming Fundamentals");
        Quiz quiz = quizFor("Java Programming Fundamentals");
        JsonNode detail = getJson("/api/quizzes/" + quiz.getId(), studentToken);
        assertThat(detail.path("data").path("questions")).isNotEmpty();
        // Students must never see the correct answer before submitting.
        detail.path("data").path("questions")
                .forEach(q -> assertThat(q.hasNonNull("correctAnswer")).isFalse());

        JsonNode result = postJson("/api/quizzes/" + quiz.getId() + "/submit", studentToken,
                Map.of("answers", correctAnswers(quiz), "durationSeconds", 120));

        JsonNode data = result.path("data");
        assertThat(data.path("score").asInt()).isEqualTo(data.path("totalScore").asInt());
        assertThat(data.path("passed").asBoolean()).isTrue();
        assertThat(data.path("correctCount").asInt()).isEqualTo(data.path("totalCount").asInt());
        assertThat(data.path("details").get(0).path("correctAnswer").asText()).isNotBlank();
    }

    @Test
    @DisplayName("wrong answers score zero and the attempt can be reviewed afterwards")
    void quizWrongAnswers() throws Exception {
        ensureEnrolled("MySQL Database Design");
        Quiz quiz = quizFor("MySQL Database Design");
        List<Map<String, Object>> answers = new ArrayList<>();
        for (Question question : questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId())) {
            answers.add(Map.of("questionId", question.getId(), "answer", "Z"));
        }

        JsonNode data = postJson("/api/quizzes/" + quiz.getId() + "/submit", studentToken,
                Map.of("answers", answers)).path("data");

        assertThat(data.path("score").asInt()).isZero();
        assertThat(data.path("passed").asBoolean()).isFalse();
        assertThat(data.path("correctCount").asInt()).isZero();

        long attemptId = data.path("attemptId").asLong();
        JsonNode review = getJson("/api/attempts/" + attemptId, studentToken).path("data");
        assertThat(review.path("details")).hasSize(answers.size());
        assertThat(review.path("attemptId").asLong()).isEqualTo(attemptId);
        assertThat(review.path("quizTitle").asText()).isEqualTo("SQL & Relational Modelling Quiz");
    }

    @Test
    @DisplayName("a multiple choice question accepts the answers in any order")
    void multipleChoiceOrderDoesNotMatter() throws Exception {
        ensureEnrolled("Vue 3 Front-end Engineering");
        Quiz quiz = quizFor("Vue 3 Front-end Engineering");
        List<Map<String, Object>> answers = new ArrayList<>();
        for (Question question : questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId())) {
            String answer = question.getCorrectAnswer();
            if (answer.contains(",")) {
                List<String> parts = new ArrayList<>(List.of(answer.split(",")));
                java.util.Collections.reverse(parts);
                answer = String.join(",", parts);
            }
            answers.add(Map.of("questionId", question.getId(), "answer", answer));
        }

        JsonNode data = postJson("/api/quizzes/" + quiz.getId() + "/submit", studentToken,
                Map.of("answers", answers)).path("data");
        assertThat(data.path("score").asInt()).isEqualTo(data.path("totalScore").asInt());
    }

    @Test
    @DisplayName("the quiz history is paginated")
    void quizHistory() throws Exception {
        JsonNode page = getJson("/api/attempts/my?page=1&size=5", studentToken).path("data");
        assertThat(page.path("page").asInt()).isEqualTo(1);
        assertThat(page.path("size").asInt()).isEqualTo(5);
    }

    // ==================================================== 4. recommendation engine

    @Test
    @DisplayName("recommendations are related to the courses the student already studies")
    void recommendationsUseLearningHistory() throws Exception {
        JsonNode list = getJson("/api/recommendations?limit=5", studentToken).path("data");
        assertThat(list).isNotEmpty();
        assertThat(list.size()).isLessThanOrEqualTo(5);

        JsonNode first = list.get(0);
        assertThat(first.path("course").path("title").asText()).isNotBlank();
        assertThat(first.path("matchScore").asInt()).isGreaterThan(0);
        assertThat(first.path("reason").asText()).isNotBlank();

        // Courses the student is enrolled in must not be recommended again.
        JsonNode mine = getJson("/api/enrollments/my/all", studentToken).path("data");
        List<String> enrolled = new ArrayList<>();
        mine.forEach(row -> enrolled.add(row.path("courseId").asText()));
        list.forEach(item -> assertThat(enrolled)
                .doesNotContain(item.path("course").path("id").asText()));
    }

    @Test
    @DisplayName("a brand new account still gets suggestions (cold start)")
    void recommendationsColdStart() throws Exception {
        String username = "cold_" + System.currentTimeMillis();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", "secret123"))))
                .andExpect(status().isOk());
        String token = login(username, "secret123");

        JsonNode list = getJson("/api/recommendations", token).path("data");
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).path("course").path("title").asText()).isNotBlank();
    }

    // ========================================================== 5. administration

    @Test
    @DisplayName("an admin can create a course, add chapters and publish it")
    void adminCourseLifecycle() throws Exception {
        long categoryId = getJson("/api/categories", "").path("data").get(0).path("id").asLong();

        Map<String, Object> course = new LinkedHashMap<>();
        course.put("title", "Introduction to Cloud Computing");
        course.put("subtitle", "Servers, containers and the cloud");
        course.put("description", "A gentle introduction for beginners.");
        course.put("categoryId", categoryId);
        course.put("teacherName", "Dr. Cloud");
        course.put("level", "BEGINNER");
        course.put("price", 0);
        course.put("durationHours", 20);
        course.put("tags", "cloud, devops, infrastructure");
        course.put("status", 0);

        JsonNode created = postJson("/api/admin/courses", adminToken, course).path("data");
        long courseId = created.path("id").asLong();
        assertThat(created.path("title").asText()).isEqualTo("Introduction to Cloud Computing");
        assertThat(created.path("status").asInt()).isZero();

        // A draft must not appear in the public catalogue.
        JsonNode publicSearch = getJson("/api/courses", "", Map.of("keyword", "Cloud Computing"));
        assertThat(publicSearch.path("data").path("total").asLong()).isZero();

        // The admin list does contain it.
        JsonNode adminSearch = getJson("/api/admin/courses", adminToken, Map.of("keyword", "Cloud Computing"));
        assertThat(adminSearch.path("data").path("total").asLong()).isEqualTo(1);

        // Add two chapters.
        for (int i = 1; i <= 2; i++) {
            postJson("/api/admin/courses/" + courseId + "/lessons", adminToken, Map.of(
                    "title", "Chapter " + i,
                    "summary", "Summary " + i,
                    "durationMinutes", 30,
                    "sortOrder", i));
        }
        JsonNode lessons = getJson("/api/admin/courses/" + courseId + "/lessons", adminToken).path("data");
        assertThat(lessons).hasSize(2);

        // Publish it.
        course.put("status", 1);
        mvc.perform(put("/api/admin/courses/" + courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(course)))
                .andExpect(status().isOk());

        JsonNode detail = getJson("/api/courses/" + courseId, "").path("data");
        assertThat(detail.path("course").path("status").asInt()).isEqualTo(1);
        assertThat(detail.path("lessons")).hasSize(2);
        assertThat(detail.path("totalLessons").asInt()).isEqualTo(2);

        // Create a quiz with a single choice question.
        JsonNode quiz = postJson("/api/admin/quizzes", adminToken, Map.of(
                "courseId", courseId,
                "title", "Cloud basics",
                "description", "Two quick questions",
                "durationMinutes", 10,
                "passScore", 60,
                "maxAttempts", 0,
                "status", 1)).path("data");
        long quizId = quiz.path("id").asLong();

        postJson("/api/admin/quizzes/" + quizId + "/questions", adminToken, Map.of(
                "content", "What does IaaS stand for?",
                "type", "SINGLE",
                "correctAnswer", "B",
                "score", 10,
                "analysis", "Infrastructure as a Service.",
                "sortOrder", 1,
                "options", List.of(
                        Map.of("optionKey", "A", "content", "Internet as a Service"),
                        Map.of("optionKey", "B", "content", "Infrastructure as a Service"),
                        Map.of("optionKey", "C", "content", "Integration as a Service"))));

        JsonNode questions = getJson("/api/admin/quizzes/" + quizId + "/questions", adminToken).path("data");
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).path("correctAnswer").asText()).isEqualTo("B");

        // The quiz now shows up for students looking at the course.
        JsonNode studentView = getJson("/api/courses/" + courseId, studentToken).path("data");
        assertThat(studentView.path("quizzes")).hasSize(1);

        // Clean up: deleting the course removes dependents.
        mvc.perform(delete("/api/admin/courses/" + courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/courses/" + courseId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the admin dashboard reports platform statistics")
    void adminDashboard() throws Exception {
        JsonNode data = getJson("/api/dashboard/admin", adminToken).path("data");
        assertThat(data.path("totalUsers").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(data.path("totalCourses").asLong()).isEqualTo(courseRepository.count());
        assertThat(data.path("publishedCourses").asLong()).isGreaterThan(0);
        assertThat(data.path("totalCategories").asLong()).isGreaterThan(0);
        assertThat(data.path("topCourses")).isNotEmpty();
    }

    @Test
    @DisplayName("the admin can list accounts and disable a student")
    void adminManagesUsers() throws Exception {
        String username = "toggle_" + System.currentTimeMillis();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", "secret123"))))
                .andExpect(status().isOk());

        JsonNode page = getJson("/api/admin/users", adminToken, Map.of("keyword", username)).path("data");
        assertThat(page.path("records")).hasSize(1);
        long userId = page.path("records").get(0).path("id").asLong();

        mvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // A disabled account can no longer sign in.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", "secret123"))))
                .andExpect(status().isForbidden());
    }

    // ================================================================= internals

    /**
     * Enrols the demo student through the real endpoint. Submitting a quiz requires an
     * active enrolment, and the seed deliberately leaves some courses un-enrolled so the
     * recommender has candidates — tests must therefore not rely on the seed for this.
     * A repeat enrolment simply returns 400, which is irrelevant here.
     */
    private void ensureEnrolled(String courseTitle) throws Exception {
        Course course = publishedCourse(courseTitle);
        mvc.perform(post("/api/enrollments")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("courseId", course.getId()))));
    }

    private Quiz quizFor(String courseTitle) {
        Course course = publishedCourse(courseTitle);
        return quizRepository.findByCourseIdOrderByIdAsc(course.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Quiz not seeded for " + courseTitle));
    }

    private List<Map<String, Object>> correctAnswers(Quiz quiz) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (Question question : questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId())) {
            answers.add(Map.of("questionId", question.getId(), "answer", question.getCorrectAnswer()));
        }
        return answers;
    }
}
