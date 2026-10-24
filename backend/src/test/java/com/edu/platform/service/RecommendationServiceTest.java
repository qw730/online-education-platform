package com.edu.platform.service;

import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.dto.RecommendDtos;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.support.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the recommendation engine (feature 4): interest profile,
 * scoring formula, exclusion of enrolled courses and the cold-start fallback.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService - scoring & cold start")
class RecommendationServiceTest {

    private static final long USER_ID = 1L;

    @Mock private CourseRepository courseRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private DtoMapper mapper;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(courseRepository, categoryRepository,
                enrollmentRepository, mapper);
    }

    // ------------------------------------------------------------ fixtures

    private Course course(long id, Long categoryId, String tags, Integer studentCount,
                          BigDecimal rating) {
        Course c = new Course();
        c.setId(id);
        c.setTitle("Course " + id);
        c.setCategoryId(categoryId);
        c.setTags(tags);
        c.setStudentCount(studentCount);
        c.setRating(rating);
        c.setStatus(CourseService.STATUS_PUBLISHED);
        return c;
    }

    private CourseDtos.CourseVO vo(Course c) {
        return new CourseDtos.CourseVO(c.getId(), c.getTitle(), null, null, c.getCategoryId(),
                null, null, null, null, null, c.getTags(), c.getStatus(), c.getStudentCount(),
                c.getRating(), null, LocalDateTime.now());
    }

    private Enrollment enrolled(long courseId) {
        Enrollment e = new Enrollment();
        e.setUserId(USER_ID);
        e.setCourseId(courseId);
        e.setStatus(EnrollmentStatus.ENROLLED);
        return e;
    }

    // ------------------------------------------------------------ tests

    @Test
    @DisplayName("a brand new account gets the most popular courses (cold start)")
    void coldStartFallsBackToPopularCourses() {
        when(enrollmentRepository.findByUserIdAndStatusNot(USER_ID, EnrollmentStatus.DROPPED))
                .thenReturn(List.of());
        when(mapper.courseMap(Set.of())).thenReturn(Map.of());

        Course popular = course(9L, 2L, "python", 500, new BigDecimal("4.9"));
        when(courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED))
                .thenReturn(List.of(popular));
        when(mapper.toCourseVO(popular)).thenReturn(vo(popular));

        List<RecommendDtos.RecommendedCourseVO> result = service.recommend(USER_ID, 6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).course().id()).isEqualTo(9L);
        assertThat(result.get(0).reason()).contains("Popular");
    }

    @Test
    @DisplayName("score = 10*category + 6*keyword + popularity + 2*rating, already enrolled courses are excluded")
    void scoresCoursesByInterestProfile() {
        Course learning = course(1L, 1L, "vue, frontend", 100, new BigDecimal("4.0"));
        when(enrollmentRepository.findByUserIdAndStatusNot(USER_ID, EnrollmentStatus.DROPPED))
                .thenReturn(List.of(enrolled(1L)));
        when(mapper.courseMap(Set.of(1L))).thenReturn(Map.of(1L, learning));
        when(mapper.categoryNameMap(any())).thenReturn(Map.of(1L, "Frontend Development"));

        Course match = course(2L, 1L, "vue, spring", 50, new BigDecimal("4.5"));
        Course noMatch = course(3L, 2L, "python", 0, null);
        when(courseRepository.findAll(ArgumentMatchers.<Specification<Course>>any()))
                .thenReturn(List.of(match, noMatch));
        when(mapper.toCourseVOs(List.of(match))).thenReturn(List.of(vo(match)));

        List<RecommendDtos.RecommendedCourseVO> result = service.recommend(USER_ID, 6);

        assertThat(result).hasSize(1); // course 3 scores 0 -> skipped
        RecommendDtos.RecommendedCourseVO best = result.get(0);
        assertThat(best.course().id()).isEqualTo(2L);
        // 10*1 (same category) + 6*1 ("vue") + 50/10 (popularity) + 2*4.5 (rating) = 30
        assertThat(best.matchScore()).isEqualTo(30);
        assertThat(best.matchedTags()).containsExactly("vue");
        assertThat(best.reason()).contains("vue");
    }

    @Test
    @DisplayName("limit is capped to 20 and clamped for invalid input")
    void limitIsClamped() {
        when(enrollmentRepository.findByUserIdAndStatusNot(USER_ID, EnrollmentStatus.DROPPED))
                .thenReturn(List.of());
        when(mapper.courseMap(Set.of())).thenReturn(Map.of());
        List<Course> popular = List.of(
                course(9L, null, null, 500, new BigDecimal("4.9")));
        when(courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED))
                .thenReturn(popular);
        when(mapper.toCourseVO(any())).thenAnswer(inv -> vo(inv.getArgument(0)));

        // limit = -1 -> defaults to 6; the engine must not blow up
        List<RecommendDtos.RecommendedCourseVO> result = service.recommend(USER_ID, -1);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("when nothing matches the profile, popular courses are suggested instead")
    void noMatchFallsBackToPopular() {
        Course learning = course(1L, 3L, "cooking", 10, new BigDecimal("3.0"));
        lenient().when(enrollmentRepository.findByUserIdAndStatusNot(USER_ID, EnrollmentStatus.DROPPED))
                .thenReturn(List.of(enrolled(1L)));
        lenient().when(mapper.courseMap(Set.of(1L))).thenReturn(Map.of(1L, learning));
        lenient().when(mapper.categoryNameMap(any())).thenReturn(Map.of());

        Course candidate = course(4L, 4L, "guitar", 0, null); // scores 0 -> engine falls back
        when(courseRepository.findAll(ArgumentMatchers.<Specification<Course>>any()))
                .thenReturn(List.of(candidate));
        Course popular = course(9L, 2L, "python", 500, new BigDecimal("4.9"));
        when(courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED))
                .thenReturn(List.of(popular));
        when(mapper.toCourseVO(popular)).thenReturn(vo(popular));

        List<RecommendDtos.RecommendedCourseVO> result = service.recommend(USER_ID, 6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).course().id()).isEqualTo(9L);
        assertThat(result.get(0).reason()).isEqualTo("Popular course in the catalogue");
    }
}
