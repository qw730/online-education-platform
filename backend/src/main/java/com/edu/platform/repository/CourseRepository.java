package com.edu.platform.repository;

import com.edu.platform.domain.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    long countByStatus(Integer status);

    long countByCategoryId(Long categoryId);

    List<Course> findTop6ByStatusOrderByStudentCountDesc(Integer status);

    @Query("select c from Course c where c.status = 1 and c.id <> :excludeId "
            + "and (:categoryId is null or c.categoryId = :categoryId) order by c.studentCount desc")
    List<Course> findRelated(Long excludeId, Long categoryId, Pageable pageable);
}
