package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 2: browse the course catalogue (public read endpoints).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/categories")
    public Result<List<CourseDtos.CategoryVO>> categories() {
        return Result.success(courseService.listCategories());
    }

    @GetMapping("/courses")
    public Result<PageResult<CourseDtos.CourseVO>> courses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size) {
        return Result.success(courseService.listCourses(keyword, categoryId, level, sortBy, page, size, false));
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseDtos.CourseDetailVO> detail(@PathVariable Long courseId) {
        var current = UserContext.get();
        return Result.success(courseService.getDetail(courseId,
                current == null || current.isAdmin() ? null : current.userId()));
    }
}
