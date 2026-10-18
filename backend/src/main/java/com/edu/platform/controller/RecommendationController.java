package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.dto.RecommendDtos;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.DashboardService;
import com.edu.platform.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 4 (recommendations) and the dashboards.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final DashboardService dashboardService;

    @GetMapping("/recommendations")
    public Result<List<RecommendDtos.RecommendedCourseVO>> recommendations(
            @RequestParam(defaultValue = "6") int limit) {
        return Result.success(recommendationService.recommend(UserContext.requireUserId(), limit));
    }

    @GetMapping("/dashboard/student")
    public Result<LearningDtos.StudentDashboardVO> studentDashboard() {
        return Result.success(dashboardService.studentDashboard(UserContext.requireUserId()));
    }

    @GetMapping("/dashboard/admin")
    public Result<LearningDtos.AdminStatsVO> adminDashboard() {
        UserContext.requireAdmin();
        return Result.success(dashboardService.adminStats());
    }
}
