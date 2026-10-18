package com.edu.platform.dto;

import com.edu.platform.domain.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payloads of enrolment, learning progress, study plan and dashboard endpoints.
 */
public final class LearningDtos {

    private LearningDtos() {
    }

    public record EnrollRequest(@NotNull(message = "Course id is required") Long courseId) {
    }

    /** A row of "My courses". */
    public record EnrollmentVO(
            Long id,
            Long courseId,
            String courseTitle,
            String coverUrl,
            String teacherName,
            String categoryName,
            EnrollmentStatus status,
            Integer progressPercent,
            Integer completedLessons,
            Integer totalLessons,
            LocalDateTime enrolledAt,
            LocalDateTime lastStudyAt) {
    }

    public record LessonProgressVO(
            Long lessonId,
            String title,
            String summary,
            String videoUrl,
            Integer durationMinutes,
            Integer sortOrder,
            boolean completed,
            Integer watchedMinutes,
            LocalDateTime lastViewedAt) {
    }

    public record CourseProgressVO(
            Long courseId,
            String courseTitle,
            Integer progressPercent,
            Integer completedLessons,
            Integer totalLessons,
            Integer totalMinutes,
            Integer studiedMinutes,
            EnrollmentStatus status,
            List<LessonProgressVO> lessons) {
    }

    public record ProgressToggleRequest(Boolean completed, Integer watchedMinutes) {
    }

    public record StudyPlanItemVO(
            Long id,
            Long planId,
            Long lessonId,
            String title,
            java.time.LocalDate plannedDate,
            String status,
            LocalDateTime completedAt) {
    }

    public record StudyPlanVO(
            Long id,
            Long courseId,
            String courseTitle,
            String title,
            String goal,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Integer dailyMinutes,
            String status,
            String remark,
            int doneItems,
            int totalItems,
            int progressPercent,
            LocalDateTime createdAt,
            List<StudyPlanItemVO> items) {
    }

    public record StudyPlanSaveRequest(
            @NotNull(message = "Course id is required") Long courseId,
            @NotNull(message = "Plan title is required") String title,
            String goal,
            @NotNull(message = "Start date is required") java.time.LocalDate startDate,
            @NotNull(message = "End date is required") java.time.LocalDate endDate,
            Integer dailyMinutes,
            String remark,
            String status,
            /** When true the backend generates one task per lesson automatically. */
            Boolean autoGenerateItems) {
    }

    public record StudyPlanItemSaveRequest(
            String title,
            Long lessonId,
            java.time.LocalDate plannedDate) {
    }

    /** Headline numbers shown on the student dashboard. */
    public record StudentDashboardVO(
            long enrolledCourses,
            long completedCourses,
            long inProgressCourses,
            long completedLessons,
            long studyMinutes,
            long activePlans,
            long quizAttempts,
            double averageQuizScore,
            List<EnrollmentVO> recentCourses,
            List<QuizDtos.AttemptVO> recentAttempts) {
    }

    /** Headline numbers shown on the admin dashboard. */
    public record AdminStatsVO(
            long totalUsers,
            long totalStudents,
            long totalAdmins,
            long totalCourses,
            long publishedCourses,
            long draftCourses,
            long totalCategories,
            long totalEnrollments,
            long totalQuizzes,
            long totalQuestions,
            long totalAttempts,
            List<CourseDtos.CourseVO> topCourses) {
    }
}
