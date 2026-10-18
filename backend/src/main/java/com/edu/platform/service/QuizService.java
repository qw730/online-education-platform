package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Question;
import com.edu.platform.domain.entity.QuestionOption;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.domain.entity.QuizAttempt;
import com.edu.platform.domain.enums.QuestionType;
import com.edu.platform.common.PageResult;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.QuestionOptionRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.support.DtoMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Feature 3 of the specification: an online quiz that is graded automatically
 * the moment the student submits it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DtoMapper mapper;
    private final ObjectMapper objectMapper;

    // ================================================================== reading

    @Transactional(readOnly = true)
    public List<QuizDtos.QuizVO> listByCourse(Long courseId, Long userId, boolean includeDrafts) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> BizException.notFound("Course not found"));
        List<Quiz> quizzes = includeDrafts
                ? quizRepository.findByCourseIdOrderByIdAsc(courseId)
                : quizRepository.findByCourseIdAndStatus(courseId, CourseService.STATUS_PUBLISHED);
        return mapper.toQuizVOsWithCourseIds(quizzes, userId);
    }

    /** Quiz detail for a student: correct answers are hidden. */
    @Transactional(readOnly = true)
    public QuizDtos.QuizDetailVO getDetail(Long quizId, Long userId) {
        Quiz quiz = requireQuiz(quizId);
        List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quizId);
        List<QuizDtos.QuestionVO> questionVOs = questions.stream()
                .map(q -> mapper.toQuestionVO(q,
                        optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(q.getId()), false))
                .toList();
        String courseTitle = courseRepository.findById(quiz.getCourseId())
                .map(Course::getTitle).orElse(null);
        return new QuizDtos.QuizDetailVO(mapper.toQuizVO(quiz, courseTitle, userId), questionVOs);
    }

    /** Same as {@link #getDetail} but with the answers revealed (admin / result screen). */
    @Transactional(readOnly = true)
    public List<QuizDtos.QuestionVO> listQuestionsWithAnswers(Long quizId) {
        requireQuiz(quizId);
        return questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quizId).stream()
                .map(q -> mapper.toQuestionVO(q,
                        optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(q.getId()), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizDtos.AttemptVO> attemptsOfQuiz(Long userId, Long quizId) {
        List<QuizAttempt> attempts = attemptRepository.findByUserIdAndQuizIdOrderByCreatedAtDesc(userId, quizId);
        return mapAttempts(attempts);
    }

    @Transactional(readOnly = true)
    public PageResult<QuizDtos.AttemptVO> myAttempts(Long userId, int page, int size) {
        Page<QuizAttempt> result = attemptRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        List<QuizDtos.AttemptVO> records = mapAttempts(result.getContent());
        return new PageResult<>(records, result.getTotalElements(),
                result.getNumber() + 1, result.getSize(), result.getTotalPages());
    }

    // ================================================================== submitting

    /**
     * Grades the submission, stores the attempt and returns the score plus a
     * per-question review.
     */
    @Transactional
    public QuizDtos.QuizResultVO submit(Long userId, Long quizId, QuizDtos.QuizSubmitRequest request) {
        Quiz quiz = requireQuiz(quizId);
        if (quiz.getStatus() == null || quiz.getStatus() != CourseService.STATUS_PUBLISHED) {
            throw new BizException("This quiz is not open yet");
        }
        boolean enrolled = enrollmentRepository.findByUserIdAndCourseId(userId, quiz.getCourseId())
                .map(e -> e.getStatus() != com.edu.platform.domain.enums.EnrollmentStatus.DROPPED)
                .orElse(false);
        if (!enrolled) {
            throw BizException.forbidden("Please enrol in the course before taking its quiz");
        }
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0
                && attemptRepository.countByUserIdAndQuizId(userId, quizId) >= quiz.getMaxAttempts()) {
            throw new BizException("You have used all " + quiz.getMaxAttempts() + " attempts of this quiz");
        }

        Map<Long, String> submitted = new HashMap<>();
        if (request.answers() != null) {
            for (QuizDtos.AnswerItem item : request.answers()) {
                if (item != null && item.questionId() != null) {
                    submitted.put(item.questionId(), item.answer());
                }
            }
        }

        List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quizId);
        if (questions.isEmpty()) {
            throw new BizException("This quiz does not contain any question yet");
        }

        int totalScore = 0;
        int gainedScore = 0;
        int correctCount = 0;
        List<QuizDtos.AnswerResultVO> details = new ArrayList<>();
        List<Map<String, Object>> rawAnswers = new ArrayList<>();

        for (Question question : questions) {
            List<QuestionOption> options = optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(question.getId());
            String yourAnswer = submitted.get(question.getId());
            boolean correct = isCorrect(question, yourAnswer);
            int score = question.getScore() == null ? 0 : question.getScore();

            totalScore += score;
            if (correct) {
                gainedScore += score;
                correctCount++;
            }
            details.add(new QuizDtos.AnswerResultVO(
                    question.getId(), question.getContent(), question.getType(),
                    yourAnswer, question.getCorrectAnswer(), correct, score,
                    correct ? score : 0, question.getAnalysis(),
                    options.stream().map(mapper::toOptionVO).toList()));

            Map<String, Object> raw = new HashMap<>();
            raw.put("questionId", question.getId());
            raw.put("yourAnswer", yourAnswer);
            raw.put("correctAnswer", question.getCorrectAnswer());
            raw.put("correct", correct);
            raw.put("gainedScore", correct ? score : 0);
            rawAnswers.add(raw);
        }

        boolean passed = totalScore == 0 || gainedScore * 100.0 / totalScore >= quiz.getPassScore();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quizId);
        attempt.setCourseId(quiz.getCourseId());
        attempt.setScore(gainedScore);
        attempt.setTotalScore(totalScore);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalCount(questions.size());
        attempt.setPassed(passed);
        attempt.setDurationSeconds(request.durationSeconds() == null ? 0 : request.durationSeconds());
        attempt.setAnswersJson(writeJson(rawAnswers));
        QuizAttempt saved = attemptRepository.save(attempt);

        return buildResult(saved, quiz, details);
    }

    /** Replays a previous attempt. */
    @Transactional(readOnly = true)
    public QuizDtos.QuizResultVO getAttempt(Long userId, Long attemptId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> BizException.notFound("Attempt not found"));
        if (!attempt.getUserId().equals(userId)) {
            throw BizException.forbidden("You can only view your own attempts");
        }
        Quiz quiz = requireQuiz(attempt.getQuizId());
        List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quiz.getId());
        Map<Long, String> answers = readAnswers(attempt.getAnswersJson());

        List<QuizDtos.AnswerResultVO> details = new ArrayList<>();
        for (Question question : questions) {
            String yourAnswer = answers.get(question.getId());
            boolean correct = isCorrect(question, yourAnswer);
            int score = question.getScore() == null ? 0 : question.getScore();
            details.add(new QuizDtos.AnswerResultVO(question.getId(), question.getContent(), question.getType(),
                    yourAnswer, question.getCorrectAnswer(), correct, score, correct ? score : 0,
                    question.getAnalysis(),
                    optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(question.getId())
                            .stream().map(mapper::toOptionVO).toList()));
        }
        return buildResult(attempt, quiz, details);
    }

    private QuizDtos.QuizResultVO buildResult(QuizAttempt attempt, Quiz quiz,
                                              List<QuizDtos.AnswerResultVO> details) {
        String courseTitle = courseRepository.findById(quiz.getCourseId()).map(Course::getTitle).orElse(null);
        return new QuizDtos.QuizResultVO(attempt.getId(), quiz.getId(), quiz.getTitle(),
                quiz.getCourseId(), courseTitle, attempt.getScore(), attempt.getTotalScore(),
                attempt.getCorrectCount(), attempt.getTotalCount(), attempt.getPassed(),
                quiz.getPassScore(), attempt.getDurationSeconds(),
                attempt.getCreatedAt() == null ? java.time.LocalDateTime.now() : attempt.getCreatedAt(),
                details);
    }

    // ================================================================== grading

    /** Compares the submitted answer with the expected one, order independent. */
    private boolean isCorrect(Question question, String yourAnswer) {
        if (!StringUtils.hasText(yourAnswer) || !StringUtils.hasText(question.getCorrectAnswer())) {
            return false;
        }
        String expected = question.getCorrectAnswer().trim().toUpperCase();
        String actual = yourAnswer.trim().toUpperCase();
        if (question.getType() == QuestionType.MULTIPLE) {
            return normaliseSet(actual).equals(normaliseSet(expected));
        }
        return expected.equals(actual);
    }

    private Set<String> normaliseSet(String answer) {
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialise quiz answers", ex);
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> readAnswers(String json) {
        Map<Long, String> map = new HashMap<>();
        if (!StringUtils.hasText(json)) {
            return map;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, List.class);
            for (Map<String, Object> row : list) {
                Object qid = row.get("questionId");
                Object answer = row.get("yourAnswer");
                if (qid != null) {
                    map.put(Long.valueOf(String.valueOf(qid)), answer == null ? null : String.valueOf(answer));
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to read stored quiz answers", ex);
        }
        return map;
    }

    private List<QuizDtos.AttemptVO> mapAttempts(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) {
            return List.of();
        }
        Map<Long, com.edu.platform.domain.entity.Quiz> quizMap = new HashMap<>();
        quizRepository.findAllById(DtoMapper.quizIdsOf(attempts)).forEach(q -> quizMap.put(q.getId(), q));
        Map<Long, Course> courseMap = mapper.courseMap(DtoMapper.courseIdsOf(attempts));
        return mapper.toAttemptVOs(attempts, quizMap, courseMap);
    }

    private Quiz requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> BizException.notFound("Quiz not found"));
    }

    /** Resolves the quiz a question belongs to (used by the admin update endpoint). */
    @Transactional(readOnly = true)
    public Long quizIdOfQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> BizException.notFound("Question not found"))
                .getQuizId();
    }

    // ================================================================== admin CRUD

    @Transactional
    public QuizDtos.QuizVO saveQuiz(Long quizId, QuizDtos.QuizSaveRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> BizException.notFound("Course not found"));
        Quiz quiz = quizId == null ? new Quiz()
                : quizRepository.findById(quizId).orElseThrow(() -> BizException.notFound("Quiz not found"));
        quiz.setCourseId(request.courseId());
        quiz.setTitle(request.title().trim());
        quiz.setDescription(request.description());
        if (request.durationMinutes() != null) {
            quiz.setDurationMinutes(request.durationMinutes());
        }
        if (request.passScore() != null) {
            quiz.setPassScore(request.passScore());
        }
        if (request.maxAttempts() != null) {
            quiz.setMaxAttempts(request.maxAttempts());
        }
        if (request.status() != null) {
            quiz.setStatus(request.status());
        }
        Quiz saved = quizRepository.save(quiz);
        return mapper.toQuizVO(saved, course.getTitle(), null);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = requireQuiz(quizId);
        List<Question> questions = questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quizId);
        for (Question q : questions) {
            optionRepository.deleteByQuestionId(q.getId());
        }
        questionRepository.deleteByQuizId(quizId);
        attemptRepository.deleteAllByQuizId(quizId);
        quizRepository.delete(quiz);
    }

    @Transactional
    public QuizDtos.QuestionVO saveQuestion(Long quizId, Long questionId, QuizDtos.QuestionSaveRequest request) {
        requireQuiz(quizId);
        Question question = questionId == null ? new Question()
                : questionRepository.findById(questionId)
                .orElseThrow(() -> BizException.notFound("Question not found"));
        question.setQuizId(quizId);
        question.setContent(request.content().trim());
        question.setType(request.type());
        question.setCorrectAnswer(normaliseCorrectAnswer(request.type(), request.correctAnswer()));
        question.setScore(request.score() == null ? 10 : request.score());
        question.setAnalysis(request.analysis());
        if (request.sortOrder() != null) {
            question.setSortOrder(request.sortOrder());
        } else if (question.getId() == null) {
            question.setSortOrder(questionRepository.findByQuizIdOrderBySortOrderAscIdAsc(quizId).size() + 1);
        }
        Question saved = questionRepository.save(question);

        // Replace the option list (simple and safe for a small quiz).
        optionRepository.deleteByQuestionId(saved.getId());
        List<QuizDtos.QuestionOptionSaveRequest> options = request.options();
        if (options != null) {
            int index = 1;
            for (QuizDtos.QuestionOptionSaveRequest option : options) {
                if (option == null || !StringUtils.hasText(option.content())) {
                    continue;
                }
                QuestionOption entity = new QuestionOption();
                entity.setQuestionId(saved.getId());
                entity.setOptionKey(StringUtils.hasText(option.optionKey())
                        ? option.optionKey().trim().toUpperCase()
                        : String.valueOf((char) ('A' + index - 1)));
                entity.setContent(option.content().trim());
                entity.setSortOrder(option.sortOrder() == null ? index : option.sortOrder());
                optionRepository.save(entity);
                index++;
            }
        }
        return mapper.toQuestionVO(saved,
                optionRepository.findByQuestionIdOrderBySortOrderAscIdAsc(saved.getId()), true);
    }

    /** TRUE_FALSE questions always use T / F, everything else upper case keys. */
    private String normaliseCorrectAnswer(QuestionType type, String correctAnswer) {
        String value = correctAnswer.trim().toUpperCase();
        if (type == QuestionType.TRUE_FALSE) {
            if (value.startsWith("T") || "TRUE".equals(value)) {
                return "T";
            }
            if (value.startsWith("F") || "FALSE".equals(value)) {
                return "F";
            }
            throw new BizException("The correct answer of a true/false question must be T or F");
        }
        if (type == QuestionType.MULTIPLE) {
            Set<String> keys = normaliseSet(value);
            if (keys.isEmpty()) {
                throw new BizException("Please provide at least one correct option");
            }
            return String.join(",", keys);
        }
        return value;
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> BizException.notFound("Question not found"));
        optionRepository.deleteByQuestionId(questionId);
        questionRepository.delete(question);
    }
}
