package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A student's enrolment ("选课") in a course, together with the cached progress.
 */
@Getter
@Setter
@Entity
@Table(name = "enrollment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_user_course", columnNames = {"user_id", "course_id"})
})
public class Enrollment extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    /** 0 - 100, recomputed whenever a lesson is marked as finished. */
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Column(name = "completed_lessons", nullable = false)
    private Integer completedLessons = 0;

    @Column(name = "total_lessons", nullable = false)
    private Integer totalLessons = 0;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_study_at")
    private LocalDateTime lastStudyAt;
}
