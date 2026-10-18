package com.edu.platform.dto;

import com.edu.platform.domain.enums.CourseLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payloads of the course &amp; category endpoints.
 */
public final class CourseDtos {

    private CourseDtos() {
    }

    public record CategoryVO(Long id, String name, String code, String description, String icon,
                             Integer sortOrder, long courseCount) {
    }

    /** Compact course card used in lists and in the recommendation carousel. */
    public record CourseVO(
            Long id,
            String title,
            String subtitle,
            String coverUrl,
            Long categoryId,
            String categoryName,
            String teacherName,
            CourseLevel level,
            BigDecimal price,
            Integer durationHours,
            String tags,
            Integer status,
            Integer studentCount,
            BigDecimal rating,
            Integer lessonCount,
            LocalDateTime createdAt) {
    }

    public record LessonVO(
            Long id,
            Long courseId,
            String title,
            String summary,
            String content,
            String videoUrl,
            Integer durationMinutes,
            Integer sortOrder) {
    }

    public record CourseDetailVO(
            CourseVO course,
            List<LessonVO> lessons,
            List<QuizDtos.QuizVO> quizzes,
            boolean enrolled,
            Integer progressPercent,
            Integer completedLessons,
            Integer totalLessons) {
    }

    public record CourseSaveRequest(
            @NotBlank(message = "Course title is required")
            @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 255) String subtitle,
            String description,
            @Size(max = 500) String coverUrl,
            @NotNull(message = "Category is required") Long categoryId,
            @Size(max = 50) String teacherName,
            CourseLevel level,
            @Min(value = 0, message = "Price cannot be negative") BigDecimal price,
            @Min(value = 0, message = "Duration cannot be negative") Integer durationHours,
            @Size(max = 255) String tags,
            Integer status) {
    }

    public record LessonSaveRequest(
            @NotBlank(message = "Lesson title is required")
            @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 500) String summary,
            String content,
            @Size(max = 500) String videoUrl,
            @Min(value = 0) Integer durationMinutes,
            Integer sortOrder) {
    }

    public record CategorySaveRequest(
            @NotBlank(message = "Category name is required") @Size(max = 50) String name,
            @NotBlank(message = "Category code is required") @Size(max = 50) String code,
            @Size(max = 255) String description,
            @Size(max = 50) String icon,
            Integer sortOrder) {
    }
}
