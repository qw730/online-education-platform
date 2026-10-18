package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 5: administrator console - create / edit / delete courses, lessons
 * and categories. Every method re-checks the ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    // ------------------------------------------------------------------ courses

    @GetMapping("/courses")
    public Result<PageResult<CourseDtos.CourseVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserContext.requireAdmin();
        return Result.success(courseService.listCourses(keyword, categoryId, level, sortBy, page, size, true));
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseDtos.CourseDetailVO> detail(@PathVariable Long courseId) {
        UserContext.requireAdmin();
        return Result.success(courseService.getDetail(courseId, null));
    }

    @PostMapping("/courses")
    public Result<CourseDtos.CourseVO> create(@Valid @RequestBody CourseDtos.CourseSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Course created", courseService.createCourse(request));
    }

    @PutMapping("/courses/{courseId}")
    public Result<CourseDtos.CourseVO> update(@PathVariable Long courseId,
                                              @Valid @RequestBody CourseDtos.CourseSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Course updated", courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/courses/{courseId}")
    public Result<Void> delete(@PathVariable Long courseId) {
        UserContext.requireAdmin();
        courseService.deleteCourse(courseId);
        return Result.success("Course deleted", null);
    }

    // ------------------------------------------------------------------ lessons

    @GetMapping("/courses/{courseId}/lessons")
    public Result<List<CourseDtos.LessonVO>> lessons(@PathVariable Long courseId) {
        UserContext.requireAdmin();
        return Result.success(courseService.listLessons(courseId));
    }

    @PostMapping("/courses/{courseId}/lessons")
    public Result<CourseDtos.LessonVO> createLesson(@PathVariable Long courseId,
                                                    @Valid @RequestBody CourseDtos.LessonSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Lesson created", courseService.createLesson(courseId, request));
    }

    @PutMapping("/lessons/{lessonId}")
    public Result<CourseDtos.LessonVO> updateLesson(@PathVariable Long lessonId,
                                                    @Valid @RequestBody CourseDtos.LessonSaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Lesson updated", courseService.updateLesson(lessonId, request));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public Result<Void> deleteLesson(@PathVariable Long lessonId) {
        UserContext.requireAdmin();
        courseService.deleteLesson(lessonId);
        return Result.success("Lesson deleted", null);
    }

    // ----------------------------------------------------------------- categories

    @PostMapping("/categories")
    public Result<CourseDtos.CategoryVO> createCategory(@Valid @RequestBody CourseDtos.CategorySaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Category created", courseService.saveCategory(null, request));
    }

    @PutMapping("/categories/{categoryId}")
    public Result<CourseDtos.CategoryVO> updateCategory(@PathVariable Long categoryId,
                                                        @Valid @RequestBody CourseDtos.CategorySaveRequest request) {
        UserContext.requireAdmin();
        return Result.success("Category updated", courseService.saveCategory(categoryId, request));
    }
}
