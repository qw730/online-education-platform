package com.edu.platform.repository;

import com.edu.platform.domain.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderBySortOrderAscIdAsc(Long questionId);

    void deleteByQuestionId(Long questionId);

    @Modifying
    @Query("delete from QuestionOption o where o.questionId in "
            + "(select q.id from Question q where q.quizId in "
            + "(select qz.id from Quiz qz where qz.courseId = :courseId))")
    void deleteByCourseId(Long courseId);
}
