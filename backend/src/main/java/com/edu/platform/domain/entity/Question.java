package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

/**
 * One question of a quiz.
 */
@Getter
@Setter
@Entity
@Table(name = "question", indexes = {
        @Index(name = "idx_question_quiz", columnList = "quiz_id")
})
public class Question extends BaseEntity {

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type = QuestionType.SINGLE;

    /** For TRUE_FALSE questions: "T" or "F". */
    @Column(name = "correct_answer", nullable = false, length = 50)
    private String correctAnswer;

    @Column(name = "score", nullable = false)
    private Integer score = 10;

    @Column(name = "analysis", length = 500)
    private String analysis;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
