package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Category;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.dto.CourseDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.dto.QuizDtos;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonProgressRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.repository.QuestionOptionRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.repository.StudyPlanItemRepository;
import com.edu.platform.repository.StudyPlanRepository;
import com.edu.platform.support.DtoMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Course catalogue: browsing, detail pages and administrator CRUD.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanItemRepository studyPlanItemRepository;
    private final DtoMapper mapper;

    // ================================================================== categories

    @Transactional(readOnly = true)
    public List<CourseDtos.CategoryVO> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(c -> mapper.toCategoryVO(c, courseRepository.countByCategoryId(c.getId())))
                .toList();
    }

    @Transactional
    public CourseDtos.CategoryVO saveCategory(Long id, CourseDtos.CategorySaveRequest request) {
        Category category = id == null
                ? new Category()
                : categoryRepository.findById(id).orElseThrow(() -> BizException.notFound("Category not found"));
        category.setName(request.name().trim());
        category.setCode(request.code().trim().toUpperCase());
        category.setDescription(request.description());
        category.setIcon(request.icon());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        Category saved = categoryRepository.save(category);
        return mapper.toCategoryVO(saved, courseRepository.countByCategoryId(saved.getId()));
    }

    // ================================================================== courses

    @Transactional(readOnly = true)
    public PageResult<CourseDtos.CourseVO> listCourses(String keyword, Long categoryId, String level,
                                                       String sortBy, int page, int size, boolean includeDrafts) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, resolveSort(sortBy));
        Specification<Course> spec = buildSpecification(keyword, categoryId, level, includeDrafts);
        Page<Course> result = courseRepository.findAll(spec, pageable);
        return PageResult.of(result, mapper::toCourseVO);
    }

    private Specification<Course> buildSpecification(String keyword, Long categoryId, String level,
                                                     boolean includeDrafts) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeDrafts) {
                predicates.add(cb.equal(root.get("status"), STATUS_PUBLISHED));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("subtitle"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("tags"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("teacherName"), "")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (StringUtils.hasText(level)) {
                predicates.add(cb.equal(root.get("level"), parseLevel(level)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private com.edu.platform.domain.enums.CourseLevel parseLevel(String level) {
        try {
            return com.edu.platform.domain.enums.CourseLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException("Unknown course level: " + level);
        }
    }

    private Sort resolveSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        return switch (sortBy) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "studentCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "rating");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "id");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    @Transactional(readOnly = true)
    public Course getEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> BizException.notFound("Course not found"));
    }

    /** Detail page: course info + lessons + quizzes + the caller's progress. */
    @Transactional(readOnly = true)
    public CourseDtos.CourseDetailVO getDetail(Long courseId, Long userId) {
        Course course = getEntity(courseId);
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
        List<Quiz> quizzes = quizRepository.findByCourseIdAndStatus(courseId, STATUS_PUBLISHED);

        boolean enrolled = false;
        Integer progressPercent = null;
        Integer completedLessons = null;
        Integer totalLessons = lessons.size();
        if (userId != null) {
            var enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
            if (enrollment != null) {
                enrolled = true;
                progressPercent = enrollment.getProgressPercent();
                completedLessons = enrollment.getCompletedLessons();
                totalLessons = enrollment.getTotalLessons();
            }
        }
        Map<Long, String> titles = Map.of(courseId, course.getTitle());
        List<QuizDtos.QuizVO> quizVOs = mapper.toQuizVOs(quizzes, titles, userId);
        return new CourseDtos.CourseDetailVO(mapper.toCourseVO(course),
                lessons.stream().map(mapper::toLessonVO).toList(), quizVOs,
                enrolled, progressPercent, completedLessons, totalLessons);
    }

    @Transactional
    public CourseDtos.CourseVO createCourse(CourseDtos.CourseSaveRequest request) {
        Course course = new Course();
        apply(course, request);
        course.setStudentCount(0);
        course.setRating(BigDecimal.ZERO);
        return mapper.toCourseVO(courseRepository.save(course));
    }

    @Transactional
    public CourseDtos.CourseVO updateCourse(Long courseId, CourseDtos.CourseSaveRequest request) {
        Course course = getEntity(courseId);
        apply(course, request);
        return mapper.toCourseVO(courseRepository.save(course));
    }

    private void apply(Course course, CourseDtos.CourseSaveRequest request) {
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new BizException("The selected category does not exist");
        }
        course.setTitle(request.title().trim());
        course.setSubtitle(request.subtitle());
        course.setDescription(request.description());
        course.setCoverUrl(request.coverUrl());
        course.setCategoryId(request.categoryId());
        course.setTeacherName(request.teacherName());
        if (request.level() != null) {
            course.setLevel(request.level());
        }
        if (request.price() != null) {
            course.setPrice(request.price());
        }
        if (request.durationHours() != null) {
            course.setDurationHours(request.durationHours());
        }
        course.setTags(normaliseTags(request.tags()));
        if (request.status() != null) {
            course.setStatus(request.status());
        }
    }

    /** Lower-cases and trims the comma separated keyword list. */
    private String normaliseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return null;
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(","));
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = getEntity(courseId);
        // Remove every dependent row so the catalogue stays consistent.
        quizAttemptRepository.deleteByCourseId(courseId);
        questionOptionRepository.deleteByCourseId(courseId);
        questionRepository.deleteByCourseId(courseId);
        quizRepository.deleteByCourseId(courseId);
        lessonProgressRepository.deleteByCourseId(courseId);
        studyPlanItemRepository.deleteByCourseId(courseId);
        studyPlanRepository.deleteByCourseId(courseId);
        enrollmentRepository.deleteByCourseId(courseId);
        lessonRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    // ================================================================== lessons

    @Transactional(readOnly = true)
    public List<CourseDtos.LessonVO> listLessons(Long courseId) {
        getEntity(courseId);
        return lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId).stream()
                .map(mapper::toLessonVO)
                .toList();
    }

    @Transactional
    public CourseDtos.LessonVO createLesson(Long courseId, CourseDtos.LessonSaveRequest request) {
        getEntity(courseId);
        Lesson lesson = new Lesson();
        lesson.setCourseId(courseId);
        applyLesson(lesson, request, (int) lessonRepository.countByCourseId(courseId) + 1);
        return mapper.toLessonVO(lessonRepository.save(lesson));
    }

    @Transactional
    public CourseDtos.LessonVO updateLesson(Long lessonId, CourseDtos.LessonSaveRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> BizException.notFound("Lesson not found"));
        applyLesson(lesson, request, lesson.getSortOrder());
        return mapper.toLessonVO(lessonRepository.save(lesson));
    }

    private void applyLesson(Lesson lesson, CourseDtos.LessonSaveRequest request, int defaultOrder) {
        lesson.setTitle(request.title().trim());
        lesson.setSummary(request.summary());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes() == null ? 0 : request.durationMinutes());
        lesson.setSortOrder(request.sortOrder() == null ? defaultOrder : request.sortOrder());
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> BizException.notFound("Lesson not found"));
        lessonRepository.delete(lesson);
    }

    /** Used by the admin list to know how many lessons/quizzes a row has. */
    @Transactional(readOnly = true)
    public Map<Long, Long> lessonCounts(List<Long> courseIds) {
        return courseIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(id -> id, lessonRepository::countByCourseId, (a, b) -> a));
    }
}
