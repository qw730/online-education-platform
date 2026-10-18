package com.edu.platform.repository;

import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findByUserIdAndStatusNot(Long userId, EnrollmentStatus status);

    Page<Enrollment> findByUserIdAndStatusNot(Long userId, EnrollmentStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, EnrollmentStatus status);

    long countByUserIdAndStatusNot(Long userId, EnrollmentStatus status);

    long countByUserId(Long userId);

    long countByCourseId(Long courseId);

    List<Enrollment> findByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}
