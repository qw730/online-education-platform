package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 5: administrator management of quizzes and their question bank.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizService quizService;

    @GetMapping("/quizzes/course/{courseId}")
    public Result<List<QuizDtos.QuizVO>> byCourse(@PathVariable Long courseId) {
        UserContext.requireAdmin();
        return Result.success(quizService.listByCourse(courseId, null, true));
    }

    @PostMapping("/quizzes")
    public Result<QuizDtos.QuizVO> create(@Valid @RequestBody QuizDtos.QuizSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Quiz created", quizService.saveQuiz(null, request));
    }

    @PutMapping("/quizzes/{quizId}")
    public Result<QuizDtos.QuizVO> update(@PathVariable Long quizId,
                                          @Valid @RequestBody QuizDtos.QuizSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Quiz updated", quizService.saveQuiz(quizId, request));
    }

    @DeleteMapping("/quizzes/{quizId}")
    public Result<Void> delete(@PathVariable Long quizId) {
        UserContext.requireAdmin();
        quizService.deleteQuiz(quizId);
        return Result.success("Quiz deleted", null);
    }

    @GetMapping("/quizzes/{quizId}/questions")
    public Result<List<QuizDtos.QuestionVO>> questions(@PathVariable Long quizId) {
        UserContext.requireAdmin();
        return Result.success(quizService.listQuestionsWithAnswers(quizId));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public Result<QuizDtos.QuestionVO> createQuestion(@PathVariable Long quizId,
                                                      @Valid @RequestBody QuizDtos.QuestionSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Question created", quizService.saveQuestion(quizId, null, request));
    }

    @PutMapping("/questions/{questionId}")
    public Result<QuizDtos.QuestionVO> updateQuestion(@PathVariable Long questionId,
                                                      @Valid @RequestBody QuizDtos.QuestionSaveRequest request) {
        UserContext.requireAdmin();
        // The quiz id is derived from the request body of the question form.
        return Result.success("Question updated",
                quizService.saveQuestion(findQuizIdOf(questionId), questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        UserContext.requireAdmin();
        quizService.deleteQuestion(questionId);
        return Result.success("Question deleted", null);
    }

    private Long findQuizIdOf(Long questionId) {
        return quizService.quizIdOfQuestion(questionId);
    }
}
