package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonProgressRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.support.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the enrolment counter logic: the denormalised
 * course.studentCount must always equal the number of non-dropped enrolments
 * (regression guard for the counting bug fixed in commit 395e3cc).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService - student counter stays in sync")
class EnrollmentServiceTest {

    private static final long USER_ID = 1L;
    private static final long COURSE_ID = 5L;

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private DtoMapper mapper;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(enrollmentRepository, lessonRepository,
                lessonProgressRepository, courseRepository, mapper);
    }

    private Course publishedCourse() {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTitle("Java Programming");
        course.setStatus(CourseService.STATUS_PUBLISHED);
        course.setStudentCount(0);
        return course;
    }

    @Test
    @DisplayName("a new enrolment sets studentCount to the real number of active enrolments")
    void newEnrolmentRecalculatesCounter() {
        Course course = publishedCourse();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(lessonRepository.countByCourseId(COURSE_ID)).thenReturn(3L);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // 3 other active students + this new one = 4
        when(enrollmentRepository.countByCourseIdAndStatusNot(COURSE_ID, EnrollmentStatus.DROPPED)).thenReturn(4L);
        when(mapper.courseMap(any())).thenReturn(java.util.Map.of());
        when(mapper.categoryNameMap(any())).thenReturn(java.util.Map.of());

        service.enroll(USER_ID, COURSE_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getStudentCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("re-enrolling a dropped course also recalculates the counter")
    void reEnrolmentRecalculatesCounter() {
        Course course = publishedCourse();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        Enrollment dropped = new Enrollment();
        dropped.setUserId(USER_ID);
        dropped.setCourseId(COURSE_ID);
        dropped.setStatus(EnrollmentStatus.DROPPED);
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(dropped));
        when(lessonRepository.countByCourseId(COURSE_ID)).thenReturn(3L);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.countByCourseIdAndStatusNot(COURSE_ID, EnrollmentStatus.DROPPED)).thenReturn(1L);
        when(mapper.courseMap(any())).thenReturn(java.util.Map.of());
        when(mapper.categoryNameMap(any())).thenReturn(java.util.Map.of());

        service.enroll(USER_ID, COURSE_ID);

        assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getStudentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("dropping a course recalculates the counter instead of blindly decrementing")
    void droppingRecalculatesCounter() {
        Course course = publishedCourse();
        course.setStudentCount(8);
        Enrollment active = new Enrollment();
        active.setUserId(USER_ID);
        active.setCourseId(COURSE_ID);
        active.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(active));
        lenient().when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.countByCourseIdAndStatusNot(COURSE_ID, EnrollmentStatus.DROPPED)).thenReturn(7L);

        service.drop(USER_ID, COURSE_ID);

        assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getStudentCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("a draft course cannot be enrolled")
    void draftCourseIsRejected() {
        Course draft = publishedCourse();
        draft.setStatus(CourseService.STATUS_DRAFT);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.enroll(USER_ID, COURSE_ID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("not open");
    }

    @Test
    @DisplayName("enrolling twice in a row is rejected")
    void doubleEnrolmentIsRejected() {
        Course course = publishedCourse();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        Enrollment active = new Enrollment();
        active.setUserId(USER_ID);
        active.setCourseId(COURSE_ID);
        active.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.enroll(USER_ID, COURSE_ID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("already enrolled");
    }
}
