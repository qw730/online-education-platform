package com.edu.platform.service;

import com.edu.platform.domain.entity.Category;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.dto.RecommendDtos;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.support.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feature 4 of the specification: recommend courses from the student's own
 * learning history.
 *
 * <p>The algorithm is intentionally simple and explainable, which makes it a
 * good fit for a course project:</p>
 * <ol>
 *   <li>Build an interest profile from every course the student has enrolled in
 *       (category weight + keyword weight).</li>
 *   <li>Score each published course that the student has <em>not</em> enrolled
 *       in yet:
 *       <pre>
 *       score = 10 * sameCategoryCount
 *             +  6 * sum(keywordWeight of the shared keywords)
 *             +  min(studentCount, 100) / 10.0
 *             +  2 * rating
 *       </pre>
 *   </li>
 *   <li>Return the top N together with a human readable reason, so the UI can
 *       explain <em>why</em> a course was suggested.</li>
 *   <li>For a brand new account with no history the engine falls back to the
 *       most popular published courses (cold start).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final double CATEGORY_WEIGHT = 10.0;
    private static final double TAG_WEIGHT = 6.0;
    private static final double RATING_WEIGHT = 2.0;
    private static final int MAX_POPULARITY_BONUS = 10;

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public List<RecommendDtos.RecommendedCourseVO> recommend(Long userId, int limit) {
        int max = limit <= 0 ? 6 : Math.min(limit, 20);

        List<Enrollment> enrollments = enrollmentRepository.findByUserIdAndStatusNot(userId,
                EnrollmentStatus.DROPPED);
        Set<Long> enrolledCourseIds = new LinkedHashSet<>();
        enrollments.forEach(e -> enrolledCourseIds.add(e.getCourseId()));
        Map<Long, Course> enrolledCourses = mapper.courseMap(enrolledCourseIds);

        // ---------------------------------------------------------- cold start
        if (enrolledCourses.isEmpty()) {
            return courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED)
                    .stream()
                    .limit(max)
                    .map(c -> new RecommendDtos.RecommendedCourseVO(mapper.toCourseVO(c), 1,
                            "Popular with other learners - enrol in a course to get personalised suggestions",
                            List.of()))
                    .toList();
        }

        // ------------------------------------------------- interest profile
        Map<Long, Integer> categoryWeights = new HashMap<>();
        Map<String, Integer> tagWeights = new HashMap<>();
        for (Course course : enrolledCourses.values()) {
            if (course.getCategoryId() != null) {
                categoryWeights.merge(course.getCategoryId(), 1, Integer::sum);
            }
            for (String tag : splitTags(course.getTags())) {
                tagWeights.merge(tag, 1, Integer::sum);
            }
        }
        Map<Long, String> categoryNames = mapper.categoryNameMap(categoryWeights.keySet());

        // ------------------------------------------------------ scoring pass
        List<Course> candidates = courseRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), CourseService.STATUS_PUBLISHED));

        List<ScoredCourse> scored = new ArrayList<>();
        for (Course candidate : candidates) {
            if (enrolledCourseIds.contains(candidate.getId())) {
                continue;
            }
            int categoryHits = candidate.getCategoryId() == null
                    ? 0 : categoryWeights.getOrDefault(candidate.getCategoryId(), 0);

            List<String> matchedTags = new ArrayList<>();
            double tagScore = 0;
            for (String tag : splitTags(candidate.getTags())) {
                Integer weight = tagWeights.get(tag);
                if (weight != null) {
                    tagScore += weight * TAG_WEIGHT;
                    matchedTags.add(tag);
                }
            }

            double popularity = Math.min(
                    candidate.getStudentCount() == null ? 0 : candidate.getStudentCount(),
                    MAX_POPULARITY_BONUS * 10) / 10.0;
            double rating = candidate.getRating() == null
                    ? 0 : candidate.getRating().multiply(BigDecimal.valueOf(RATING_WEIGHT)).doubleValue();

            double score = categoryHits * CATEGORY_WEIGHT + tagScore + popularity + rating;
            if (score <= 0) {
                continue;
            }
            scored.add(new ScoredCourse(candidate, score,
                    buildReason(categoryHits, matchedTags, categoryNames.get(candidate.getCategoryId())),
                    matchedTags));
        }

        // Fall back to popular courses when nothing matched the profile.
        if (scored.isEmpty()) {
            return courseRepository.findTop6ByStatusOrderByStudentCountDesc(CourseService.STATUS_PUBLISHED)
                    .stream()
                    .filter(c -> !enrolledCourseIds.contains(c.getId()))
                    .limit(max)
                    .map(c -> new RecommendDtos.RecommendedCourseVO(mapper.toCourseVO(c), 1,
                            "Popular course in the catalogue", List.of()))
                    .toList();
        }

        scored.sort(Comparator.comparingDouble(ScoredCourse::score).reversed()
                .thenComparing(s -> s.course().getStudentCount() == null ? 0 : s.course().getStudentCount(),
                        Comparator.reverseOrder()));

        List<Course> top = scored.stream().limit(max).map(ScoredCourse::course).toList();
        List<com.edu.platform.dto.CourseDtos.CourseVO> courseVOs = mapper.toCourseVOs(top);

        List<RecommendDtos.RecommendedCourseVO> result = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            ScoredCourse s = scored.get(i);
            result.add(new RecommendDtos.RecommendedCourseVO(courseVOs.get(i),
                    (int) Math.round(s.score()), s.reason(), s.matchedTags()));
        }
        return result;
    }

    private String buildReason(int categoryHits, List<String> matchedTags, String categoryName) {
        if (!matchedTags.isEmpty()) {
            List<String> shown = matchedTags.stream().limit(3).toList();
            return "Matches the topics you are learning: " + String.join(", ", shown);
        }
        if (categoryHits > 0 && StringUtils.hasText(categoryName)) {
            return "Because you are studying " + categoryName + " courses";
        }
        return "Learners who took your courses also liked this one";
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(t -> t.trim().toLowerCase())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private record ScoredCourse(Course course, double score, String reason, List<String> matchedTags) {
    }
}
