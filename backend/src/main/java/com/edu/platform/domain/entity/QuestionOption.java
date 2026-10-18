package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

/**
 * A selectable option of a {@link Question}.
 */
@Getter
@Setter
@Entity
@Table(name = "question_option", indexes = {
        @Index(name = "idx_option_question", columnList = "question_id")
})
public class QuestionOption extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** A, B, C, D ... */
    @Column(name = "option_key", nullable = false, length = 4)
    private String optionKey;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
