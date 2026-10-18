package com.edu.platform.repository;

import com.edu.platform.domain.entity.StudyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudyPlanItemRepository extends JpaRepository<StudyPlanItem, Long> {

    List<StudyPlanItem> findByPlanIdOrderByPlannedDateAscIdAsc(Long planId);

    void deleteByPlanId(Long planId);

    @Modifying
    @Query("delete from StudyPlanItem i where i.planId in (select p.id from StudyPlan p where p.courseId = :courseId)")
    void deleteByCourseId(Long courseId);
}
