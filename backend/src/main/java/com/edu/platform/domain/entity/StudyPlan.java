package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.PlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A personal study plan created by a student for one of the enrolled courses.
 */
@Getter
@Setter
@Entity
@Table(name = "study_plan", indexes = {
        @Index(name = "idx_plan_user", columnList = "user_id")
})
public class StudyPlan extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "goal", length = 500)
    private String goal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Intended study time per day, in minutes. */
    @Column(name = "daily_minutes")
    private Integer dailyMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanStatus status = PlanStatus.PLANNED;

    @Column(name = "remark", length = 500)
    private String remark;
}
