package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

/**
 * One lesson (chapter) of a course. Lesson completion drives course progress.
 */
@Getter
@Setter
@Entity
@Table(name = "lesson", indexes = {
        @Index(name = "idx_lesson_course", columnList = "course_id")
})
public class Lesson extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 0;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
