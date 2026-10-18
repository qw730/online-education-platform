package com.edu.platform.dto;

import java.util.List;

/**
 * Payload of the course recommendation endpoint.
 */
public final class RecommendDtos {

    private RecommendDtos() {
    }

    /**
     * @param course      the recommended course
     * @param matchScore  bigger means "more relevant to your learning history"
     * @param reason      a short human readable explanation shown on the card
     * @param matchedTags the keywords that produced the match
     */
    public record RecommendedCourseVO(
            CourseDtos.CourseVO course,
            int matchScore,
            String reason,
            List<String> matchedTags) {
    }
}
