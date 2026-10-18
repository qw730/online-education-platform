package com.edu.platform.repository;

import com.edu.platform.domain.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    /** Bulk lesson counter for course lists: {@code [courseId, lessonCount]}. */
    @Query("select l.courseId, count(l) from Lesson l where l.courseId in :courseIds group by l.courseId")
    List<Object[]> countGroupByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}
