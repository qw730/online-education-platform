package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-lesson learning record of a student, used to render the learning progress.
 */
@Getter
@Setter
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_progress_user_lesson", columnNames = {"user_id", "lesson_id"})
})
public class LessonProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "watched_minutes", nullable = false)
    private Integer watchedMinutes = 0;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;
}
