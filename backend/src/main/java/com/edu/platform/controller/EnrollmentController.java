package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 2: enrolment ("选课") and learning progress.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enrollments")
    public Result<LearningDtos.EnrollmentVO> enroll(@Valid @RequestBody LearningDtos.EnrollRequest request) {
        return Result.success("Enrolled successfully",
                enrollmentService.enroll(UserContext.requireUserId(), request.courseId()));
    }

    @DeleteMapping("/enrollments/{courseId}")
    public Result<Void> drop(@PathVariable Long courseId) {
        enrollmentService.drop(UserContext.requireUserId(), courseId);
        return Result.success("Course dropped", null);
    }

    @GetMapping("/enrollments/my")
    public Result<PageResult<LearningDtos.EnrollmentVO>> myCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return Result.success(enrollmentService.myCourses(UserContext.requireUserId(), page, size));
    }

    @GetMapping("/enrollments/my/all")
    public Result<List<LearningDtos.EnrollmentVO>> myActiveCourses() {
        return Result.success(enrollmentService.myActiveCourses(UserContext.requireUserId()));
    }

    @GetMapping("/progress/course/{courseId}")
    public Result<LearningDtos.CourseProgressVO> courseProgress(@PathVariable Long courseId) {
        return Result.success(enrollmentService.courseProgress(UserContext.requireUserId(), courseId));
    }

    @PostMapping("/progress/lesson/{lessonId}")
    public Result<LearningDtos.CourseProgressVO> toggleLesson(@PathVariable Long lessonId,
                                                              @RequestBody(required = false)
                                                              LearningDtos.ProgressToggleRequest request) {
        LearningDtos.ProgressToggleRequest body = request == null
                ? new LearningDtos.ProgressToggleRequest(null, null) : request;
        return Result.success("Progress updated",
                enrollmentService.toggleLessonProgress(UserContext.requireUserId(), lessonId, body));
    }
}
