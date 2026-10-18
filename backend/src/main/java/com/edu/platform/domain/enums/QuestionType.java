package com.edu.platform.domain.enums;

/**
 * Question type of a quiz. The correct answer of a SINGLE question is one
 * option key such as {@code "A"}; for MULTIPLE it is comma separated keys such
 * as {@code "A,C"}; for TRUE_FALSE it is {@code "T"} or {@code "F"}.
 */
public enum QuestionType {
    SINGLE,
    MULTIPLE,
    TRUE_FALSE
}
