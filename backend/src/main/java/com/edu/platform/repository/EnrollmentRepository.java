package com.edu.platform.repository;

import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    /** Real student count per course (dropped enrolments excluded), in one query. */
    @Query("select e.courseId, count(e) from Enrollment e " +
           "where e.courseId in :courseIds and e.status <> com.edu.platform.domain.enums.EnrollmentStatus.DROPPED " +
           "group by e.courseId")
    List<Object[]> countActiveGroupByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    List<Enrollment> findByUserIdAndStatusNot(Long userId, EnrollmentStatus status);

    Page<Enrollment> findByUserIdAndStatusNot(Long userId, EnrollmentStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, EnrollmentStatus status);

    long countByUserIdAndStatusNot(Long userId, EnrollmentStatus status);

    long countByUserId(Long userId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);

    List<Enrollment> findByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}
