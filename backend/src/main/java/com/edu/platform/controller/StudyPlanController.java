package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature 2: personal study plans.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping
    public Result<List<LearningDtos.StudyPlanVO>> myPlans() {
        return Result.success(studyPlanService.myPlans(UserContext.requireUserId()));
    }

    @GetMapping("/{planId}")
    public Result<LearningDtos.StudyPlanVO> detail(@PathVariable Long planId) {
        return Result.success(studyPlanService.getPlan(UserContext.requireUserId(), planId));
    }

    @PostMapping
    public Result<LearningDtos.StudyPlanVO> create(@Valid @RequestBody LearningDtos.StudyPlanSaveRequest request) {
        return Result.success("Study plan created",
                studyPlanService.createPlan(UserContext.requireUserId(), request));
    }

    @PutMapping("/{planId}")
    public Result<LearningDtos.StudyPlanVO> update(@PathVariable Long planId,
                                                   @Valid @RequestBody LearningDtos.StudyPlanSaveRequest request) {
        return Result.success("Study plan updated",
                studyPlanService.updatePlan(UserContext.requireUserId(), planId, request));
    }

    @DeleteMapping("/{planId}")
    public Result<Void> delete(@PathVariable Long planId) {
        studyPlanService.deletePlan(UserContext.requireUserId(), planId);
        return Result.success("Study plan deleted", null);
    }

    @PostMapping("/{planId}/items")
    public Result<LearningDtos.StudyPlanVO> addItem(@PathVariable Long planId,
                                                    @RequestBody LearningDtos.StudyPlanItemSaveRequest request) {
        return Result.success("Task added",
                studyPlanService.addItem(UserContext.requireUserId(), planId, request));
    }

    @PatchMapping("/{planId}/items/{itemId}")
    public Result<LearningDtos.StudyPlanVO> toggleItem(@PathVariable Long planId, @PathVariable Long itemId) {
        return Result.success("Task updated",
                studyPlanService.toggleItem(UserContext.requireUserId(), planId,
                        new LearningDtos.StudyPlanItemSaveRequest(null, null, null), itemId));
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    public Result<LearningDtos.StudyPlanVO> deleteItem(@PathVariable Long planId, @PathVariable Long itemId) {
        return Result.success("Task removed",
                studyPlanService.deleteItem(UserContext.requireUserId(), planId, itemId));
    }
}
