package com.edu.platform.repository;

import com.edu.platform.domain.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    List<LessonProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    long countByUserIdAndCourseIdAndCompleted(Long userId, Long courseId, Boolean completed);

    long countByUserIdAndCompleted(Long userId, Boolean completed);

    /** Total minutes a student has spent on lessons. */
    @Query("select coalesce(sum(p.watchedMinutes), 0) from LessonProgress p where p.userId = :userId")
    Long sumWatchedMinutes(@Param("userId") Long userId);

    void deleteByCourseId(Long courseId);
}
