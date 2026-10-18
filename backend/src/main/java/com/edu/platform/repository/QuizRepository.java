package com.edu.platform.repository;

import com.edu.platform.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseIdOrderByIdAsc(Long courseId);

    List<Quiz> findByCourseIdAndStatus(Long courseId, Integer status);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}
