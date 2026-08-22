package com.school.sms.service.impl;

import com.school.sms.dto.request.AssignClassTeacherRequest;
import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.entity.ClassOfficial;
import com.school.sms.entity.ClassOfficialRole;
import com.school.sms.entity.Gender;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.mapper.SectionMapper;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The two "one at a time" rules: a teacher heads at most one section, and a
 * student holds at most one post.
 *
 * <p>Both are backed by unique indexes added in 13_assignment_uniqueness.sql, so
 * these checks are not what makes them true — the database is. What the service
 * adds is a message naming the section or post already held, in place of a
 * duplicate-key error that names an index. These tests pin that behaviour, and
 * in particular the two cases where the rule must NOT fire: re-saving an
 * unchanged value, and clearing an assignment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentUniquenessTest {

    /* ================================================================== */
    /* Rule 1 — one homeroom per teacher                                  */
    /* ================================================================== */

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class OneHomeroomPerTeacher {

        @Mock private SectionRepository sectionRepository;
        @Mock private SchoolClassRepository schoolClassRepository;
        @Mock private TeacherRepository teacherRepository;
        @Mock private SectionMapper sectionMapper;
        @Mock private StudentRepository studentRepository;
        @Mock private UserService userService;

        @InjectMocks private SectionServiceImpl service;

        private Teacher teacher;
        private Section target;

        @BeforeEach
        void setUp() {
            User user = new User();
            user.setId(900L);
            user.setFirstName("Anika");
            user.setLastName("Agarwal");

            teacher = new Teacher();
            teacher.setId(7L);
            teacher.setUser(user);
            when(teacherRepository.findById(7L)).thenReturn(Optional.of(teacher));

            SchoolClass schoolClass = new SchoolClass();
            schoolClass.setId(1L);
            schoolClass.setClassName("Pre-Nursery");

            target = new Section();
            target.setId(3L);
            target.setSectionName("A");
            target.setSchoolClass(schoolClass);
            when(sectionRepository.findById(3L)).thenReturn(Optional.of(target));

            when(sectionRepository.save(any(Section.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(sectionMapper.toDto(any(Section.class)))
                    .thenAnswer(invocation -> new com.school.sms.dto.response.SectionDto());
        }

        private Section otherSectionHeldBy(Teacher held) {
            SchoolClass schoolClass = new SchoolClass();
            schoolClass.setId(1L);
            schoolClass.setClassName("Pre-Nursery");

            Section other = new Section();
            other.setId(2L);
            other.setSectionName("B");
            other.setSchoolClass(schoolClass);
            other.setClassTeacher(held);
            return other;
        }

        private AssignClassTeacherRequest request(Long teacherId) {
            AssignClassTeacherRequest request = new AssignClassTeacherRequest();
            request.setTeacherId(teacherId);
            return request;
        }

        @Test
        void rejectsATeacherWhoAlreadyHeadsAnotherSection() {
            when(sectionRepository.findByClassTeacherId(7L))
                    .thenReturn(Optional.of(otherSectionHeldBy(teacher)));

            assertThatThrownBy(() -> service.assignClassTeacher(3L, request(7L)))
                    .isInstanceOf(BadRequestException.class)
                    // Names the teacher and where they already are, not an index.
                    .hasMessageContaining("Anika Agarwal")
                    .hasMessageContaining("Pre-Nursery - B")
                    .hasMessageContaining("only one section");

            verify(sectionRepository, never()).save(any());
            // Nothing may touch the class-teacher flag when the assignment is
            // refused. This replaced a check on UserService.promoteToClassTeacher,
            // which used to swap the user's role to CLASS_TEACHER and no longer
            // exists — the duty is teachers.is_class_teacher now.
            verify(teacherRepository, never()).save(any());
        }

        @Test
        void allowsATeacherWithNoHomeroom() {
            when(sectionRepository.findByClassTeacherId(7L)).thenReturn(Optional.empty());

            assertThatCode(() -> service.assignClassTeacher(3L, request(7L))).doesNotThrowAnyException();
            verify(sectionRepository).save(any(Section.class));
        }

        /**
         * The grid re-sends a row's current value when a neighbouring cell changes,
         * so treating "already here" as a clash would make the screen unusable.
         */
        @Test
        void allowsReassigningATeacherToTheSectionTheyAlreadyHead() {
            target.setClassTeacher(teacher);
            when(sectionRepository.findByClassTeacherId(7L)).thenReturn(Optional.of(target));

            assertThatCode(() -> service.assignClassTeacher(3L, request(7L))).doesNotThrowAnyException();
            verify(sectionRepository).save(any(Section.class));
        }

        /** Clearing must never be blocked — it is how you free a teacher up. */
        @Test
        void allowsClearingAClassTeacher() {
            when(sectionRepository.findByClassTeacherId(any())).thenReturn(Optional.of(otherSectionHeldBy(teacher)));

            assertThatCode(() -> service.assignClassTeacher(3L, request(null))).doesNotThrowAnyException();
            verify(teacherRepository, never()).findById(any());
        }
    }

    /* ================================================================== */
    /* Rule 2 — one post per student                                      */
    /* ================================================================== */

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class OnePostPerStudent {

        private static final long CLASS_ID = 1L;

        @Mock private ClassOfficialRepository classOfficialRepository;
        @Mock private SchoolClassRepository schoolClassRepository;
        @Mock private SectionRepository sectionRepository;
        @Mock private StudentRepository studentRepository;
        @Mock private AuditLogService auditLogService;

        @InjectMocks private ClassOfficialServiceImpl service;

        private SchoolClass schoolClass;
        private Student student;

        @BeforeEach
        void setUp() {
            schoolClass = new SchoolClass();
            schoolClass.setId(CLASS_ID);
            schoolClass.setClassName("Pre-Nursery");
            schoolClass.setDeleted(false);
            when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

            student = new Student();
            student.setId(55L);
            student.setFirstName("Vikram");
            student.setLastName("Kapoor");
            student.setGender(Gender.MALE);
            student.setStatus(StudentStatus.ACTIVE);
            student.setDeleted(false);
            student.setSchoolClass(schoolClass);
            when(studentRepository.findById(55L)).thenReturn(Optional.of(student));

            when(classOfficialRepository.save(any(ClassOfficial.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(classOfficialRepository.saveAndFlush(any(ClassOfficial.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        private ClassOfficialRequest request(ClassOfficialRole role) {
            ClassOfficialRequest request = new ClassOfficialRequest();
            request.setStudentId(55L);
            request.setRole(role);
            return request;
        }

        private ClassOfficial held(ClassOfficialRole role) {
            return ClassOfficial.builder()
                    .schoolClass(schoolClass)
                    .student(student)
                    .role(role)
                    .fromDate(LocalDate.of(2026, 4, 1))
                    .build();
        }

        @Test
        void rejectsAStudentWhoAlreadyHoldsADifferentPost() {
            when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(55L))
                    .thenReturn(List.of(held(ClassOfficialRole.MONITOR)));

            assertThatThrownBy(() -> service.appoint(CLASS_ID, request(ClassOfficialRole.HEAD_BOY)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Vikram Kapoor")
                    .hasMessageContaining("already Monitor")
                    .hasMessageContaining("only one post");

            verify(classOfficialRepository, never()).save(any());
        }

        /**
         * Re-appointing to the post already held is a different mistake from a
         * clash, and gets its own message so the fix is obvious.
         */
        @Test
        void reportsReappointmentToTheSamePostDistinctly() {
            when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(55L))
                    .thenReturn(List.of(held(ClassOfficialRole.HEAD_BOY)));

            assertThatThrownBy(() -> service.appoint(CLASS_ID, request(ClassOfficialRole.HEAD_BOY)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("That student already holds this post");
        }

        @Test
        void allowsAStudentHoldingNoPost() {
            when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(55L)).thenReturn(List.of());

            assertThatCode(() -> service.appoint(CLASS_ID, request(ClassOfficialRole.HEAD_BOY)))
                    .doesNotThrowAnyException();
            verify(classOfficialRepository).save(any(ClassOfficial.class));
        }

        /**
         * A student whose only appointment has ended is free again — the finder is
         * scoped to live rows, so an empty result is the whole test.
         */
        @Test
        void allowsAStudentWhoseEarlierPostHasEnded() {
            when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(55L)).thenReturn(List.of());

            assertThatCode(() -> service.appoint(CLASS_ID, request(ClassOfficialRole.MONITOR)))
                    .doesNotThrowAnyException();
        }

        /** Succession still works: the rule is about the incoming student, not the outgoing one. */
        @Test
        void stillSucceedsASittingHolderWhenTheIncomingStudentIsFree() {
            Student outgoing = new Student();
            outgoing.setId(1L);
            outgoing.setFirstName("Vihaan");
            outgoing.setSchoolClass(schoolClass);

            when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(55L)).thenReturn(List.of());
            when(classOfficialRepository.findBySchoolClassIdAndRoleAndToDateIsNull(
                    CLASS_ID, ClassOfficialRole.HEAD_BOY))
                    .thenReturn(Optional.of(ClassOfficial.builder()
                            .schoolClass(schoolClass)
                            .student(outgoing)
                            .role(ClassOfficialRole.HEAD_BOY)
                            .fromDate(LocalDate.of(2026, 4, 1))
                            .build()));

            assertThatCode(() -> service.appoint(CLASS_ID, request(ClassOfficialRole.HEAD_BOY)))
                    .doesNotThrowAnyException();
            verify(classOfficialRepository).saveAndFlush(any(ClassOfficial.class));
            verify(classOfficialRepository).save(any(ClassOfficial.class));
        }
    }
}
