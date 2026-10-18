package com.edu.platform.repository;

import com.edu.platform.domain.entity.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdAndQuizIdOrderByCreatedAtDesc(Long userId, Long quizId);

    Page<QuizAttempt> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndQuizId(Long userId, Long quizId);

    Optional<QuizAttempt> findFirstByUserIdAndQuizIdOrderByScoreDesc(Long userId, Long quizId);

    long countByQuizId(Long quizId);

    long countByUserId(Long userId);

    /** Average score in percent across all attempts of one student. */
    @org.springframework.data.jpa.repository.Query(
            "select coalesce(avg(case when a.totalScore = 0 then 0 else a.score * 100.0 / a.totalScore end), 0) "
                    + "from QuizAttempt a where a.userId = :userId")
    Double averageScorePercent(@org.springframework.data.repository.query.Param("userId") Long userId);

    void deleteByCourseId(Long courseId);

    void deleteAllByQuizId(Long quizId);

    @org.springframework.data.jpa.repository.Query(
            "select a.quizId, count(a), max(a.score) from QuizAttempt a "
                    + "where a.userId = :userId and a.quizId in :quizIds group by a.quizId")
    List<Object[]> statsByUserAndQuizIds(@org.springframework.data.repository.query.Param("userId") Long userId,
                                         @org.springframework.data.repository.query.Param("quizIds")
                                         List<Long> quizIds);
}
