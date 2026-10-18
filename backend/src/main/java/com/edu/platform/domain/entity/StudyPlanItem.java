package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.PlanItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A scheduled task inside a {@link StudyPlan}, normally bound to one lesson.
 */
@Getter
@Setter
@Entity
@Table(name = "study_plan_item", indexes = {
        @Index(name = "idx_plan_item_plan", columnList = "plan_id")
})
public class StudyPlanItem extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanItemStatus status = PlanItemStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
