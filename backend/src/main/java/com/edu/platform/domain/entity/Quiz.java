package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

/**
 * A small quiz attached to a course. Students submit it and get the score
 * automatically.
 */
@Getter
@Setter
@Entity
@Table(name = "quiz", indexes = {
        @Index(name = "idx_quiz_course", columnList = "course_id")
})
public class Quiz extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    /** Time limit in minutes, 0 means no limit. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 0;

    /** Score needed to pass, in percent. */
    @Column(name = "pass_score")
    private Integer passScore = 60;

    /** Number of attempts allowed per student, 0 means unlimited. */
    @Column(name = "max_attempts")
    private Integer maxAttempts = 0;

    /** 1 = published, 0 = draft. */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
