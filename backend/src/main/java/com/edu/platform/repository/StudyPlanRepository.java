package com.edu.platform.repository;

import com.edu.platform.domain.entity.StudyPlan;
import com.edu.platform.domain.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudyPlan> findByUserIdAndStatus(Long userId, PlanStatus status);

    long countByUserIdAndStatus(Long userId, PlanStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<PlanStatus> statuses);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}
