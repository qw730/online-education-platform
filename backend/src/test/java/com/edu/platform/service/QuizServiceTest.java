package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.entity.Question;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.domain.enums.QuestionType;
import com.edu.platform.domain.enums.Role;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.QuestionOptionRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.support.DtoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the quiz auto-grading logic (feature 3 of the specification).
 * All repositories are mocked, so every rule of the grading engine is verified
 * in isolation and fast.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QuizService - submission & auto grading")
class QuizServiceTest {

    private static final long USER_ID = 1L;
    private static final long COURSE_ID = 5L;
    private static final long QUIZ_ID = 10L;

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository optionRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private DtoMapper mapper;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepository, questionRepository, optionRepository,
                attemptRepository, courseRepository, enrollmentRepository, mapper, new ObjectMapper());
    }

    // ------------------------------------------------------------ fixtures

    private Quiz publishedQuiz(int maxAttempts) {
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setCourseId(COURSE_ID);
        quiz.setTitle("Java Basics Quiz");
        quiz.setStatus(CourseService.STATUS_PUBLISHED);
        quiz.setPassScore(60);
        quiz.setMaxAttempts(maxAttempts);
        return quiz;
    }

    private Question question(long id, QuestionType type, String correctAnswer, int score) {
        Question q = new Question();
        q.setId(id);
        q.setQuizId(QUIZ_ID);
        q.setContent("Q" + id);
        q.setType(type);
        q.setCorrectAnswer(correctAnswer);
        q.setScore(score);
        q.setSortOrder((int) id);
        return q;
    }

    private void mockHappyPathRepos(Quiz quiz, List<Question> questions) {
        lenient().when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        lenient().when(questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(QUIZ_ID)).thenReturn(questions);
        lenient().when(optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(anyLong())).thenReturn(List.of());
        lenient().when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTitle("Java Programming");
        lenient().when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
    }

    private static QuizDtos.AnswerItem answer(long questionId, String answer) {
        return new QuizDtos.AnswerItem(questionId, answer);
    }

    private void mockEnrolled() {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(USER_ID);
        enrollment.setCourseId(COURSE_ID);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment));
    }

    // ------------------------------------------------------------ grading

    @Test
    @DisplayName("SINGLE / MULTIPLE / TRUE_FALSE are graded correctly, MULTIPLE is order independent")
    void gradesMixedQuestionTypes() {
        Quiz quiz = publishedQuiz(0);
        List<Question> questions = List.of(
                question(101, QuestionType.SINGLE, "A", 10),
                question(102, QuestionType.MULTIPLE, "A,B", 20),
                question(103, QuestionType.TRUE_FALSE, "T", 10));
        mockHappyPathRepos(quiz, questions);
        mockEnrolled();
        when(attemptRepository.countByUserIdAndQuizId(USER_ID, QUIZ_ID)).thenReturn(0L);
        when(mapper.toOptionVO(any())).thenReturn(null);

        // "a" = case insensitive; "b, a" = order independent; "f" = wrong
        QuizDtos.QuizSubmitRequest request = new QuizDtos.QuizSubmitRequest(
                List.of(answer(101, " a "), answer(102, "b, a"), answer(103, "f")), 120);

        QuizDtos.QuizResultVO result = quizService.submit(USER_ID, QUIZ_ID, request);

        assertThat(result.score()).isEqualTo(30);          // 10 + 20 + 0
        assertThat(result.totalScore()).isEqualTo(40);
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.passed()).isTrue();              // 75% >= 60%
        assertThat(result.details()).hasSize(3);
        assertThat(result.details().get(0).correct()).isTrue();
        assertThat(result.details().get(1).correct()).isTrue();   // MULTIPLE, order independent
        assertThat(result.details().get(2).correct()).isFalse();  // TRUE_FALSE wrong
        assertThat(result.details().get(2).gainedScore()).isZero();
    }

    @Test
    @DisplayName("a failing score is reported as not passed")
    void failingSubmissionIsNotPassed() {
        Quiz quiz = publishedQuiz(0);
        mockHappyPathRepos(quiz, List.of(
                question(101, QuestionType.SINGLE, "A", 10),
                question(102, QuestionType.SINGLE, "B", 30)));
        mockEnrolled();
        when(attemptRepository.countByUserIdAndQuizId(USER_ID, QUIZ_ID)).thenReturn(0L);
        when(mapper.toOptionVO(any())).thenReturn(null);

        QuizDtos.QuizResultVO result = quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(answer(101, "A"), answer(102, "A")), 60));

        assertThat(result.score()).isEqualTo(10);
        assertThat(result.totalScore()).isEqualTo(40);
        assertThat(result.passed()).isFalse();             // 25% < 60%
    }

    @Test
    @DisplayName("blank answers simply score zero instead of throwing")
    void blankAnswerScoresZero() {
        Quiz quiz = publishedQuiz(0);
        mockHappyPathRepos(quiz, List.of(question(101, QuestionType.SINGLE, "A", 10)));
        mockEnrolled();
        when(attemptRepository.countByUserIdAndQuizId(USER_ID, QUIZ_ID)).thenReturn(0L);
        when(mapper.toOptionVO(any())).thenReturn(null);

        QuizDtos.QuizResultVO result = quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(answer(101, "  ")), 30));

        assertThat(result.score()).isZero();
        assertThat(result.passed()).isFalse();
    }

    // ------------------------------------------------------------ guards

    @Test
    @DisplayName("a draft quiz cannot be submitted")
    void draftQuizIsRejected() {
        Quiz quiz = publishedQuiz(0);
        quiz.setStatus(CourseService.STATUS_DRAFT);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(), 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("not open");
    }

    @Test
    @DisplayName("a student who is not enrolled is forbidden to submit")
    void notEnrolledIsForbidden() {
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(publishedQuiz(0)));
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(), 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("enrol");
    }

    @Test
    @DisplayName("a dropped enrolment also blocks the submission")
    void droppedEnrolmentBlocksSubmission() {
        Quiz quiz = publishedQuiz(0);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        Enrollment dropped = new Enrollment();
        dropped.setUserId(USER_ID);
        dropped.setCourseId(COURSE_ID);
        dropped.setStatus(EnrollmentStatus.DROPPED);
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(dropped));

        assertThatThrownBy(() -> quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(), 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("enrol");
    }

    @Test
    @DisplayName("the attempt limit of a quiz is enforced")
    void maxAttemptsIsEnforced() {
        Quiz quiz = publishedQuiz(1);
        mockHappyPathRepos(quiz, List.of());
        mockEnrolled();
        when(attemptRepository.countByUserIdAndQuizId(USER_ID, QUIZ_ID)).thenReturn(1L);

        assertThatThrownBy(() -> quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(), 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("attempts");
    }

    @Test
    @DisplayName("a quiz without questions cannot be submitted")
    void emptyQuizIsRejected() {
        Quiz quiz = publishedQuiz(0);
        mockHappyPathRepos(quiz, List.of());
        mockEnrolled();
        when(attemptRepository.countByUserIdAndQuizId(USER_ID, QUIZ_ID)).thenReturn(0L);

        assertThatThrownBy(() -> quizService.submit(USER_ID, QUIZ_ID,
                new QuizDtos.QuizSubmitRequest(List.of(), 10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("question");
    }

    // ------------------------------------------------------------ admin rules

    @Test
    @DisplayName("TRUE_FALSE correct answers are normalised to T / F")
    void trueFalseAnswerNormalisation() {
        Quiz quiz = publishedQuiz(0);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(new Course()));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.saveQuestion(QUIZ_ID, null, new QuizDtos.QuestionSaveRequest(
                "Java is platform independent?", QuestionType.TRUE_FALSE, "true", 10,
                null, null, List.of()));

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrectAnswer()).isEqualTo("T");
    }
}
