package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

/**
 * One submission of a quiz. The score is calculated by the backend as soon as
 * the answers arrive.
 */
@Getter
@Setter
@Entity
@Table(name = "quiz_attempt", indexes = {
        @Index(name = "idx_attempt_user", columnList = "user_id"),
        @Index(name = "idx_attempt_quiz", columnList = "quiz_id")
})
public class QuizAttempt extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "passed", nullable = false)
    private Boolean passed = false;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    /** Raw answers as a JSON array, kept for review. */
    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;
}
