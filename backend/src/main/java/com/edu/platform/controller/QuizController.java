package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.common.PageResult;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 3: take a quiz and get the score back immediately.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/quizzes/course/{courseId}")
    public Result<List<QuizDtos.QuizVO>> byCourse(@PathVariable Long courseId) {
        var current = UserContext.require();
        return Result.success(quizService.listByCourse(courseId,
                current.isAdmin() ? null : current.userId(), current.isAdmin()));
    }

    @GetMapping("/quizzes/{quizId}")
    public Result<QuizDtos.QuizDetailVO> detail(@PathVariable Long quizId) {
        var current = UserContext.require();
        return Result.success(quizService.getDetail(quizId, current.isAdmin() ? null : current.userId()));
    }

    @PostMapping("/quizzes/{quizId}/submit")
    public Result<QuizDtos.QuizResultVO> submit(@PathVariable Long quizId,
                                                @Valid @RequestBody QuizDtos.QuizSubmitRequest request) {
        return Result.success("Quiz submitted",
                quizService.submit(UserContext.requireUserId(), quizId, request));
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    public Result<List<QuizDtos.AttemptVO>> attemptsOfQuiz(@PathVariable Long quizId) {
        return Result.success(quizService.attemptsOfQuiz(UserContext.requireUserId(), quizId));
    }

    @GetMapping("/attempts/my")
    public Result<PageResult<QuizDtos.AttemptVO>> myAttempts(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(quizService.myAttempts(UserContext.requireUserId(), page, size));
    }

    @GetMapping("/attempts/{attemptId}")
    public Result<QuizDtos.QuizResultVO> attemptDetail(@PathVariable Long attemptId) {
        return Result.success(quizService.getAttempt(UserContext.requireUserId(), attemptId));
    }
}
