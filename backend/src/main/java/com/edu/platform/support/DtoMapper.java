package com.edu.platform.support;

import com.edu.platform.domain.entity.Category;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.Question;
import com.edu.platform.domain.entity.QuestionOption;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.domain.entity.QuizAttempt;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts entities into the view objects consumed by the Vue frontend.
 * Uses bulk lookups so that list pages do not trigger N+1 queries.
 */
@Component
@RequiredArgsConstructor
public class DtoMapper {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    // ------------------------------------------------------------------ categories

    public CourseDtos.CategoryVO toCategoryVO(Category category, long courseCount) {
        return new CourseDtos.CategoryVO(category.getId(), category.getName(), category.getCode(),
                category.getDescription(), category.getIcon(), category.getSortOrder(), courseCount);
    }

    // ------------------------------------------------------------------ courses

    public CourseDtos.CourseVO toCourseVO(Course course) {
        return toCourseVOs(List.of(course)).get(0);
    }

    public List<CourseDtos.CourseVO> toCourseVOs(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNames = categoryNameMap(
                courses.stream().map(Course::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, Long> lessonCounts = lessonCountMap(
                courses.stream().map(Course::getId).collect(Collectors.toList()));

        return courses.stream()
                .map(c -> new CourseDtos.CourseVO(
                        c.getId(), c.getTitle(), c.getSubtitle(), c.getCoverUrl(),
                        c.getCategoryId(), categoryNames.get(c.getCategoryId()),
                        c.getTeacherName(), c.getLevel(), c.getPrice(), c.getDurationHours(),
                        c.getTags(), c.getStatus(), c.getStudentCount(), c.getRating(),
                        lessonCounts.getOrDefault(c.getId(), 0L).intValue(), c.getCreatedAt()))
                .toList();
    }

    public CourseDtos.LessonVO toLessonVO(Lesson lesson) {
        return new CourseDtos.LessonVO(lesson.getId(), lesson.getCourseId(), lesson.getTitle(),
                lesson.getSummary(), lesson.getContent(), lesson.getVideoUrl(),
                lesson.getDurationMinutes(), lesson.getSortOrder());
    }

    public Map<Long, String> categoryNameMap(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, Long> lessonCountMap(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : lessonRepository.countGroupByCourseIds(courseIds)) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    // ------------------------------------------------------------------ enrolments

    public LearningDtos.EnrollmentVO toEnrollmentVO(Enrollment enrollment,
                                                    Map<Long, Course> courseMap,
                                                    Map<Long, String> categoryNames) {
        Course course = courseMap.get(enrollment.getCourseId());
        return new LearningDtos.EnrollmentVO(
                enrollment.getId(),
                enrollment.getCourseId(),
                course == null ? "(deleted course)" : course.getTitle(),
                course == null ? null : course.getCoverUrl(),
                course == null ? null : course.getTeacherName(),
                course == null ? null : categoryNames.get(course.getCategoryId()),
                enrollment.getStatus(),
                enrollment.getProgressPercent(),
                enrollment.getCompletedLessons(),
                enrollment.getTotalLessons(),
                enrollment.getEnrolledAt(),
                enrollment.getLastStudyAt());
    }

    /** Loads the referenced courses in one query and returns them keyed by id. */
    public Map<Long, Course> courseMap(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
    }

    // ------------------------------------------------------------------ quizzes

    public QuizDtos.QuizVO toQuizVO(Quiz quiz, String courseTitle, Long userId) {
        return toQuizVOs(List.of(quiz), Map.of(quiz.getCourseId(), courseTitle), userId).get(0);
    }

    public List<QuizDtos.QuizVO> toQuizVOs(List<Quiz> quizzes, Map<Long, String> courseTitles, Long userId) {
        if (quizzes.isEmpty()) {
            return List.of();
        }
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();

        Map<Long, int[]> stats = new HashMap<>();
        for (Object[] row : questionRepository.statsByQuizIds(quizIds)) {
            stats.put((Long) row[0], new int[]{((Number) row[1]).intValue(), ((Number) row[2]).intValue()});
        }

        Map<Long, long[]> attempts = new HashMap<>();
        Map<Long, Integer> bestScores = new HashMap<>();
        if (userId != null) {
            for (Object[] row : quizAttemptRepository.statsByUserAndQuizIds(userId, quizIds)) {
                Long quizId = (Long) row[0];
                attempts.put(quizId, new long[]{((Number) row[1]).longValue()});
                bestScores.put(quizId, row[2] == null ? null : ((Number) row[2]).intValue());
            }
        }

        return quizzes.stream().map(q -> {
            int[] stat = stats.getOrDefault(q.getId(), new int[]{0, 0});
            long attemptCount = userId == null
                    ? quizAttemptRepository.countByQuizId(q.getId())
                    : attempts.getOrDefault(q.getId(), new long[]{0})[0];
            Integer bestScore = bestScores.get(q.getId());
            Boolean passed = bestScore == null ? null
                    : (stat[1] == 0 || bestScore * 100.0 / stat[1] >= q.getPassScore());
            return new QuizDtos.QuizVO(q.getId(), q.getCourseId(), courseTitles.get(q.getCourseId()),
                    q.getTitle(), q.getDescription(), q.getDurationMinutes(), q.getPassScore(),
                    q.getMaxAttempts(), q.getStatus(), stat[0], stat[1], attemptCount, bestScore, passed);
        }).toList();
    }

    public QuizDtos.QuestionOptionVO toOptionVO(QuestionOption option) {
        return new QuizDtos.QuestionOptionVO(option.getId(), option.getOptionKey(),
                option.getContent(), option.getSortOrder());
    }

    /**
     * @param revealAnswer when false the correct answer and the analysis are
     *                     stripped, so a student cannot cheat from the network tab.
     */
    public QuizDtos.QuestionVO toQuestionVO(Question question, List<QuestionOption> options, boolean revealAnswer) {
        return new QuizDtos.QuestionVO(
                question.getId(), question.getQuizId(), question.getContent(), question.getType(),
                question.getScore(), question.getSortOrder(),
                revealAnswer ? question.getCorrectAnswer() : null,
                revealAnswer ? question.getAnalysis() : null,
                options.stream().map(this::toOptionVO).toList());
    }

    public QuizDtos.AttemptVO toAttemptVO(QuizAttempt attempt, Map<Long, Quiz> quizMap, Map<Long, Course> courseMap) {
        Quiz quiz = quizMap.get(attempt.getQuizId());
        Course course = courseMap.get(attempt.getCourseId());
        return new QuizDtos.AttemptVO(attempt.getId(), attempt.getQuizId(),
                quiz == null ? "(deleted quiz)" : quiz.getTitle(),
                attempt.getCourseId(),
                course == null ? "(deleted course)" : course.getTitle(),
                attempt.getScore(), attempt.getTotalScore(), attempt.getCorrectCount(),
                attempt.getTotalCount(), attempt.getPassed(), attempt.getDurationSeconds(),
                attempt.getCreatedAt());
    }

    public List<QuizDtos.AttemptVO> toAttemptVOs(List<QuizAttempt> attempts,
                                                 Map<Long, Quiz> quizMap,
                                                 Map<Long, Course> courseMap) {
        return attempts.stream().map(a -> toAttemptVO(a, quizMap, courseMap)).toList();
    }

    /** Ids of the quizzes referenced by the given attempts. */
    public static List<Long> quizIdsOf(List<QuizAttempt> attempts) {
        return attempts.stream().map(QuizAttempt::getQuizId).distinct().toList();
    }

    /** Ids of the courses referenced by the given attempts. */
    public static List<Long> courseIdsOf(List<QuizAttempt> attempts) {
        return attempts.stream().map(QuizAttempt::getCourseId).distinct().toList();
    }

    /** Convenience overload that resolves the course titles internally. */
    public List<QuizDtos.QuizVO> toQuizVOsWithCourseIds(List<Quiz> quizzes, Long userId) {
        if (quizzes.isEmpty()) {
            return List.of();
        }
        Map<Long, String> titles = courseRepository.findAllById(
                        quizzes.stream().map(Quiz::getCourseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));
        return toQuizVOs(quizzes, titles, userId);
    }
}
