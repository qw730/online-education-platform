package com.edu.platform.dto;

import com.edu.platform.domain.enums.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payloads of the quiz endpoints.
 */
public final class QuizDtos {

    private QuizDtos() {
    }

    public record QuizVO(
            Long id,
            Long courseId,
            String courseTitle,
            String title,
            String description,
            Integer durationMinutes,
            Integer passScore,
            Integer maxAttempts,
            Integer status,
            int questionCount,
            int totalScore,
            long attemptCount,
            Integer bestScore,
            Boolean passed) {
    }

    public record QuestionOptionVO(Long id, String optionKey, String content, Integer sortOrder) {
    }

    /**
     * {@code correctAnswer} and {@code analysis} stay {@code null} while a
     * student is taking the quiz and are only revealed in the result payload.
     */
    public record QuestionVO(
            Long id,
            Long quizId,
            String content,
            QuestionType type,
            Integer score,
            Integer sortOrder,
            String correctAnswer,
            String analysis,
            List<QuestionOptionVO> options) {
    }

    public record QuizDetailVO(QuizVO quiz, List<QuestionVO> questions) {
    }

    public record QuizSaveRequest(
            @NotNull(message = "Course id is required") Long courseId,
            @NotBlank(message = "Quiz title is required") @Size(max = 200) String title,
            @Size(max = 500) String description,
            @Min(0) Integer durationMinutes,
            @Min(0) Integer passScore,
            @Min(0) Integer maxAttempts,
            Integer status) {
    }

    public record QuestionOptionSaveRequest(String optionKey, String content, Integer sortOrder) {
    }

    public record QuestionSaveRequest(
            @NotBlank(message = "Question content is required") String content,
            @NotNull(message = "Question type is required") QuestionType type,
            @NotBlank(message = "Correct answer is required") String correctAnswer,
            @Min(value = 1, message = "Score must be at least 1") Integer score,
            @Size(max = 500) String analysis,
            Integer sortOrder,
            List<QuestionOptionSaveRequest> options) {
    }

    /** One answer submitted by a student. */
    public record AnswerItem(@NotNull Long questionId, String answer) {
    }

    public record QuizSubmitRequest(
            List<AnswerItem> answers,
            Integer durationSeconds) {
    }

    public record AnswerResultVO(
            Long questionId,
            String content,
            QuestionType type,
            String yourAnswer,
            String correctAnswer,
            boolean correct,
            Integer score,
            Integer gainedScore,
            String analysis,
            List<QuestionOptionVO> options) {
    }

    /** Returned right after submitting, so the score is visible immediately. */
    public record QuizResultVO(
            Long attemptId,
            Long quizId,
            String quizTitle,
            Long courseId,
            String courseTitle,
            Integer score,
            Integer totalScore,
            Integer correctCount,
            Integer totalCount,
            Boolean passed,
            Integer passScore,
            Integer durationSeconds,
            LocalDateTime submittedAt,
            List<AnswerResultVO> details) {
    }

    public record AttemptVO(
            Long id,
            Long quizId,
            String quizTitle,
            Long courseId,
            String courseTitle,
            Integer score,
            Integer totalScore,
            Integer correctCount,
            Integer totalCount,
            Boolean passed,
            Integer durationSeconds,
            LocalDateTime createdAt) {
    }
}
