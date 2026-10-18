package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.CourseLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A course published on the platform.
 */
@Getter
@Setter
@Entity
@Table(name = "course")
public class Course extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "teacher_name", length = 50)
    private String teacherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private CourseLevel level = CourseLevel.BEGINNER;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "duration_hours")
    private Integer durationHours = 0;

    /** Comma separated keywords, used by the recommendation engine. */
    @Column(name = "tags", length = 255)
    private String tags;

    /** 1 = published, 0 = draft (only visible to admins). */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** Denormalised counter kept in sync by the enrolment service. */
    @Column(name = "student_count", nullable = false)
    private Integer studentCount = 0;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
}
