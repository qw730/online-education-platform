package com.edu.platform.repository;

import com.edu.platform.domain.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderBySortOrderAscIdAsc(Long quizId);

    /** Bulk quiz stats: {@code [quizId, questionCount, totalScore]}. */
    @Query("select q.quizId, count(q), coalesce(sum(q.score), 0) from Question q "
            + "where q.quizId in :quizIds group by q.quizId")
    List<Object[]> statsByQuizIds(@Param("quizIds") Collection<Long> quizIds);

    void deleteByQuizId(Long quizId);

    @Modifying
    @Query("delete from Question q where q.quizId in (select qz.id from Quiz qz where qz.courseId = :courseId)")
    void deleteByCourseId(Long courseId);
}
