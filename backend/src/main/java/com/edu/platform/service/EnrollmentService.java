package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.LessonProgress;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonProgressRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.support.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 2 of the specification: pick a course, keep the learning progress up
 * to date and show how far the student has got.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final DtoMapper mapper;

    // ================================================================== enrolment

    @Transactional
    public LearningDtos.EnrollmentVO enroll(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> BizException.notFound("Course not found"));
        if (course.getStatus() == null || course.getStatus() != CourseService.STATUS_PUBLISHED) {
            throw new BizException("This course is not open for enrolment yet");
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        boolean isNew = enrollment == null;
        if (isNew) {
            enrollment = new Enrollment();
            enrollment.setUserId(userId);
            enrollment.setCourseId(courseId);
            enrollment.setEnrolledAt(LocalDateTime.now());
        } else if (enrollment.getStatus() != EnrollmentStatus.DROPPED
                && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new BizException("You have already enrolled in this course");
        } else {
            // Re-activate a dropped / finished enrolment.
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
        }

        int totalLessons = (int) lessonRepository.countByCourseId(courseId);
        enrollment.setTotalLessons(totalLessons);
        enrollment.setLastStudyAt(LocalDateTime.now());
        if (enrollment.getCompletedLessons() == null) {
            enrollment.setCompletedLessons(0);
        }
        if (totalLessons > 0 && enrollment.getProgressPercent() == null) {
            enrollment.setProgressPercent(0);
        }
        Enrollment saved = enrollmentRepository.save(enrollment);

        // Keep the denormalised counter exactly in sync with the real number of enrolments.
        course.setStudentCount((int) enrollmentRepository.countByCourseIdAndStatusNot(courseId,
                EnrollmentStatus.DROPPED));
        courseRepository.save(course);
        return toVO(saved);
    }

    @Transactional
    public void drop(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> BizException.notFound("You are not enrolled in this course"));
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course != null) {
            course.setStudentCount((int) enrollmentRepository.countByCourseIdAndStatusNot(courseId,
                    EnrollmentStatus.DROPPED));
            courseRepository.save(course);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<LearningDtos.EnrollmentVO> myCourses(Long userId, int page, int size) {
        Page<Enrollment> result = enrollmentRepository.findByUserIdAndStatusNot(userId,
                EnrollmentStatus.DROPPED,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "lastStudyAt")));
        Map<Long, Course> courseMap = mapper.courseMap(
                result.getContent().stream().map(Enrollment::getCourseId).toList());
        Map<Long, String> categoryNames = mapper.categoryNameMap(
                courseMap.values().stream().map(Course::getCategoryId).toList());
        return PageResult.of(result, e -> mapper.toEnrollmentVO(e, courseMap, categoryNames));
    }

    @Transactional(readOnly = true)
    public List<LearningDtos.EnrollmentVO> myActiveCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdAndStatusNot(userId, EnrollmentStatus.DROPPED);
        Map<Long, Course> courseMap = mapper.courseMap(enrollments.stream().map(Enrollment::getCourseId).toList());
        Map<Long, String> categoryNames = mapper.categoryNameMap(
                courseMap.values().stream().map(Course::getCategoryId).toList());
        return enrollments.stream().map(e -> mapper.toEnrollmentVO(e, courseMap, categoryNames)).toList();
    }

    // ================================================================== progress

    /** Full progress breakdown of one course, lesson by lesson. */
    @Transactional(readOnly = true)
    public LearningDtos.CourseProgressVO courseProgress(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> BizException.notFound("Course not found"));
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> BizException.forbidden("Please enrol in this course first"));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
        Map<Long, LessonProgress> progressMap = new HashMap<>();
        for (LessonProgress p : lessonProgressRepository.findByUserIdAndCourseId(userId, courseId)) {
            progressMap.put(p.getLessonId(), p);
        }

        List<LearningDtos.LessonProgressVO> lessonVOs = lessons.stream().map(lesson -> {
            LessonProgress p = progressMap.get(lesson.getId());
            return new LearningDtos.LessonProgressVO(
                    lesson.getId(), lesson.getTitle(), lesson.getSummary(), lesson.getVideoUrl(),
                    lesson.getDurationMinutes(), lesson.getSortOrder(),
                    p != null && Boolean.TRUE.equals(p.getCompleted()),
                    p == null || p.getWatchedMinutes() == null ? 0 : p.getWatchedMinutes(),
                    p == null ? null : p.getLastViewedAt());
        }).toList();

        int totalMinutes = lessons.stream()
                .mapToInt(l -> l.getDurationMinutes() == null ? 0 : l.getDurationMinutes()).sum();
        int studiedMinutes = lessonVOs.stream()
                .filter(LearningDtos.LessonProgressVO::completed)
                .mapToInt(v -> v.durationMinutes() == null ? 0 : v.durationMinutes())
                .sum();

        return new LearningDtos.CourseProgressVO(courseId, course.getTitle(),
                enrollment.getProgressPercent(), enrollment.getCompletedLessons(),
                lessons.size(), totalMinutes, studiedMinutes, enrollment.getStatus(), lessonVOs);
    }

    /**
     * Marks a lesson as finished / unfinished and recomputes the enrolment
     * progress of the whole course.
     */
    @Transactional
    public LearningDtos.CourseProgressVO toggleLessonProgress(Long userId, Long lessonId,
                                                             LearningDtos.ProgressToggleRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> BizException.notFound("Lesson not found"));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, lesson.getCourseId())
                .orElseThrow(() -> BizException.forbidden("Please enrol in this course first"));
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw BizException.forbidden("This course has been dropped, please enrol again");
        }

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress p = new LessonProgress();
                    p.setUserId(userId);
                    p.setCourseId(lesson.getCourseId());
                    p.setLessonId(lessonId);
                    return p;
                });
        boolean completed = request.completed() == null
                ? !Boolean.TRUE.equals(progress.getCompleted())
                : request.completed();
        progress.setCompleted(completed);
        if (request.watchedMinutes() != null) {
            progress.setWatchedMinutes(request.watchedMinutes());
        } else if (completed && lesson.getDurationMinutes() != null) {
            progress.setWatchedMinutes(lesson.getDurationMinutes());
        }
        progress.setLastViewedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);

        recalculate(userId, lesson.getCourseId());
        return courseProgress(userId, lesson.getCourseId());
    }

    /** Recomputes and persists the enrolment counters of one course. */
    @Transactional
    public Enrollment recalculate(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> BizException.forbidden("Please enrol in this course first"));

        long total = lessonRepository.countByCourseId(courseId);
        long done = lessonProgressRepository.countByUserIdAndCourseIdAndCompleted(userId, courseId, true);
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);

        enrollment.setTotalLessons((int) total);
        enrollment.setCompletedLessons((int) done);
        enrollment.setProgressPercent(percent);
        enrollment.setLastStudyAt(LocalDateTime.now());
        if (percent >= 100 && total > 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            // A lesson was un-ticked again.
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setCompletedAt(null);
        }
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .orElse(false);
    }

    private LearningDtos.EnrollmentVO toVO(Enrollment enrollment) {
        Map<Long, Course> courseMap = mapper.courseMap(List.of(enrollment.getCourseId()));
        Map<Long, String> categoryNames = mapper.categoryNameMap(
                courseMap.values().stream().map(Course::getCategoryId).toList());
        return mapper.toEnrollmentVO(enrollment, courseMap, categoryNames);
    }
}
