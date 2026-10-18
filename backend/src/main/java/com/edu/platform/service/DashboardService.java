package com.edu.platform.service;

import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.domain.entity.QuizAttempt;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.domain.enums.PlanStatus;
import com.edu.platform.domain.enums.Role;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonProgressRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.repository.StudyPlanRepository;
import com.edu.platform.repository.UserRepository;
import com.edu.platform.support.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated numbers for the student dashboard and the administrator console.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public LearningDtos.StudentDashboardVO studentDashboard(Long userId) {
        List<Enrollment> active = enrollmentRepository.findByUserIdAndStatusNot(userId, EnrollmentStatus.DROPPED);
        Map<Long, Course> courseMap = mapper.courseMap(active.stream().map(Enrollment::getCourseId).toList());
        Map<Long, String> categoryNames = mapper.categoryNameMap(
                courseMap.values().stream().map(Course::getCategoryId).toList());

        List<LearningDtos.EnrollmentVO> recent = active.stream()
                .sorted(java.util.Comparator.comparing(Enrollment::getLastStudyAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(4)
                .map(e -> mapper.toEnrollmentVO(e, courseMap, categoryNames))
                .toList();

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        List<QuizDtos.AttemptVO> attemptVOs = mapAttempts(attempts);

        Double average = quizAttemptRepository.averageScorePercent(userId);
        Long minutes = lessonProgressRepository.sumWatchedMinutes(userId);

        return new LearningDtos.StudentDashboardVO(
                active.size(),
                enrollmentRepository.countByUserIdAndStatus(userId, EnrollmentStatus.COMPLETED),
                enrollmentRepository.countByUserIdAndStatus(userId, EnrollmentStatus.ENROLLED),
                lessonProgressRepository.countByUserIdAndCompleted(userId, true),
                minutes == null ? 0 : minutes,
                studyPlanRepository.countByUserIdAndStatusIn(userId,
                        List.of(PlanStatus.PLANNED, PlanStatus.IN_PROGRESS)),
                quizAttemptRepository.countByUserId(userId),
                average == null ? 0 : Math.round(average * 10) / 10.0,
                recent,
                attemptVOs);
    }

    @Transactional(readOnly = true)
    public LearningDtos.AdminStatsVO adminStats() {
        long totalCourses = courseRepository.count();
        long published = courseRepository.countByStatus(CourseService.STATUS_PUBLISHED);
        List<CourseDtos.CourseVO> top = mapper.toCourseVOs(
                courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED));

        return new LearningDtos.AdminStatsVO(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.ADMIN),
                totalCourses,
                published,
                totalCourses - published,
                categoryRepository.count(),
                enrollmentRepository.count(),
                quizRepository.count(),
                questionRepository.count(),
                quizAttemptRepository.count(),
                top);
    }

    private List<QuizDtos.AttemptVO> mapAttempts(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) {
            return List.of();
        }
        Map<Long, Quiz> quizMap = new HashMap<>();
        quizRepository.findAllById(DtoMapper.quizIdsOf(attempts)).forEach(q -> quizMap.put(q.getId(), q));
        Map<Long, Course> courseMap = mapper.courseMap(DtoMapper.courseIdsOf(attempts));
        return mapper.toAttemptVOs(attempts, quizMap, courseMap);
    }
}
