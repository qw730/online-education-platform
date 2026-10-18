package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.StudyPlan;
import com.edu.platform.domain.entity.StudyPlanItem;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.domain.enums.PlanItemStatus;
import com.edu.platform.domain.enums.PlanStatus;
import com.edu.platform.dto.LearningDtos;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.repository.StudyPlanItemRepository;
import com.edu.platform.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Feature 2 of the specification: a student can build a personal study plan per
 * enrolled course and tick the scheduled tasks off day by day.
 */
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository planRepository;
    private final StudyPlanItemRepository itemRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<LearningDtos.StudyPlanVO> myPlans(Long userId) {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningDtos.StudyPlanVO getPlan(Long userId, Long planId) {
        return toVO(requireOwnedPlan(userId, planId));
    }

    @Transactional
    public LearningDtos.StudyPlanVO createPlan(Long userId, LearningDtos.StudyPlanSaveRequest request) {
        validateDates(request.startDate(), request.endDate());
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> BizException.notFound("Course not found"));
        boolean enrolled = enrollmentRepository.findByUserIdAndCourseId(userId, request.courseId())
                .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .orElse(false);
        if (!enrolled) {
            throw BizException.forbidden("Please enrol in \"" + course.getTitle() + "\" before planning it");
        }

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setCourseId(request.courseId());
        plan.setTitle(request.title().trim());
        plan.setGoal(request.goal());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan.setDailyMinutes(request.dailyMinutes() == null ? 30 : request.dailyMinutes());
        plan.setRemark(request.remark());
        plan.setStatus(resolveStatus(request.status(), PlanStatus.PLANNED));
        StudyPlan saved = planRepository.save(plan);

        if (Boolean.TRUE.equals(request.autoGenerateItems())) {
            generateItemsFromLessons(saved);
        }
        return toVO(saved);
    }

    @Transactional
    public LearningDtos.StudyPlanVO updatePlan(Long userId, Long planId,
                                               LearningDtos.StudyPlanSaveRequest request) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        validateDates(request.startDate(), request.endDate());
        plan.setTitle(request.title().trim());
        plan.setGoal(request.goal());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        if (request.dailyMinutes() != null) {
            plan.setDailyMinutes(request.dailyMinutes());
        }
        plan.setRemark(request.remark());
        if (StringUtils.hasText(request.status())) {
            plan.setStatus(resolveStatus(request.status(), plan.getStatus()));
        }
        return toVO(planRepository.save(plan));
    }

    @Transactional
    public void deletePlan(Long userId, Long planId) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        itemRepository.deleteByPlanId(plan.getId());
        planRepository.delete(plan);
    }

    // ================================================================== plan items

    @Transactional
    public LearningDtos.StudyPlanVO addItem(Long userId, Long planId,
                                            LearningDtos.StudyPlanItemSaveRequest request) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        StudyPlanItem item = new StudyPlanItem();
        item.setPlanId(plan.getId());
        item.setLessonId(request.lessonId());
        item.setTitle(StringUtils.hasText(request.title()) ? request.title().trim() : resolveLessonTitle(request.lessonId()));
        item.setPlannedDate(request.plannedDate() == null ? LocalDate.now() : request.plannedDate());
        item.setStatus(PlanItemStatus.PENDING);
        itemRepository.save(item);
        refreshPlanStatus(plan);
        return toVO(plan);
    }

    @Transactional
    public LearningDtos.StudyPlanVO toggleItem(Long userId, Long planId,
                                               LearningDtos.StudyPlanItemSaveRequest request, Long itemId) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        StudyPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> BizException.notFound("Task not found"));
        if (!item.getPlanId().equals(plan.getId())) {
            throw BizException.forbidden("This task does not belong to the plan");
        }
        boolean done = item.getStatus() != PlanItemStatus.DONE;
        item.setStatus(done ? PlanItemStatus.DONE : PlanItemStatus.PENDING);
        item.setCompletedAt(done ? LocalDateTime.now() : null);
        itemRepository.save(item);
        refreshPlanStatus(plan);
        return toVO(plan);
    }

    @Transactional
    public LearningDtos.StudyPlanVO deleteItem(Long userId, Long planId, Long itemId) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        StudyPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> BizException.notFound("Task not found"));
        if (!item.getPlanId().equals(plan.getId())) {
            throw BizException.forbidden("This task does not belong to the plan");
        }
        itemRepository.delete(item);
        refreshPlanStatus(plan);
        return toVO(plan);
    }

    /** Spreads the course lessons evenly between the start and the end date. */
    private void generateItemsFromLessons(StudyPlan plan) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(plan.getCourseId());
        if (lessons.isEmpty()) {
            return;
        }
        long spanDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()));
        double step = (double) spanDays / lessons.size();
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            StudyPlanItem item = new StudyPlanItem();
            item.setPlanId(plan.getId());
            item.setLessonId(lesson.getId());
            item.setTitle(lesson.getTitle());
            item.setPlannedDate(plan.getStartDate().plusDays((long) Math.floor(i * step)));
            item.setStatus(PlanItemStatus.PENDING);
            itemRepository.save(item);
        }
    }

    private void refreshPlanStatus(StudyPlan plan) {
        List<StudyPlanItem> items = itemRepository.findByPlanIdOrderByPlannedDateAscIdAsc(plan.getId());
        long done = items.stream().filter(i -> i.getStatus() == PlanItemStatus.DONE).count();
        if (plan.getStatus() != PlanStatus.ABANDONED) {
            if (!items.isEmpty() && done == items.size()) {
                plan.setStatus(PlanStatus.COMPLETED);
            } else if (plan.getStartDate() != null && !plan.getStartDate().isAfter(LocalDate.now())) {
                plan.setStatus(PlanStatus.IN_PROGRESS);
            } else {
                plan.setStatus(PlanStatus.PLANNED);
            }
        }
        planRepository.save(plan);
    }

    private LearningDtos.StudyPlanVO toVO(StudyPlan plan) {
        List<StudyPlanItem> items = itemRepository.findByPlanIdOrderByPlannedDateAscIdAsc(plan.getId());
        Map<Long, String> courseTitles;
        Course course = courseRepository.findById(plan.getCourseId()).orElse(null);
        courseTitles = course == null ? Map.of() : Map.of(course.getId(), course.getTitle());

        List<LearningDtos.StudyPlanItemVO> itemVOs = items.stream()
                .map(i -> new LearningDtos.StudyPlanItemVO(i.getId(), i.getPlanId(), i.getLessonId(),
                        i.getTitle(), i.getPlannedDate(), i.getStatus().name(), i.getCompletedAt()))
                .toList();
        int done = (int) items.stream().filter(i -> i.getStatus() == PlanItemStatus.DONE).count();
        int percent = items.isEmpty() ? 0 : (int) Math.round(done * 100.0 / items.size());

        return new LearningDtos.StudyPlanVO(plan.getId(), plan.getCourseId(),
                courseTitles.get(plan.getCourseId()), plan.getTitle(), plan.getGoal(),
                plan.getStartDate(), plan.getEndDate(), plan.getDailyMinutes(), plan.getStatus().name(),
                plan.getRemark(), done, items.size(), percent, plan.getCreatedAt(), itemVOs);
    }

    private StudyPlan requireOwnedPlan(Long userId, Long planId) {
        StudyPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BizException.notFound("Study plan not found"));
        if (!plan.getUserId().equals(userId)) {
            throw BizException.forbidden("You can only manage your own study plans");
        }
        return plan;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BizException("Start date and end date are required");
        }
        if (end.isBefore(start)) {
            throw new BizException("The end date cannot be earlier than the start date");
        }
    }

    private PlanStatus resolveStatus(String status, PlanStatus fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        try {
            return PlanStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException("Unknown plan status: " + status);
        }
    }

    private String resolveLessonTitle(Long lessonId) {
        if (lessonId == null) {
            return "Study task";
        }
        return lessonRepository.findById(lessonId)
                .map(Lesson::getTitle)
                .orElseThrow(() -> BizException.notFound("Lesson not found"));
    }
}
