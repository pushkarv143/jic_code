package com.school.sms.service.impl;

import com.school.sms.dto.request.MarkStudentAttendanceRequest;
import com.school.sms.dto.request.StudentAttendanceRecordItem;
import com.school.sms.entity.AttendanceStatus;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentAttendance;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentAttendanceRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SectionAccessGuard;
import com.school.sms.security.StudentAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Attendance marking is a batch write addressed by section, which makes it the
 * sharpest authorization surface in the module: the class/section written to each
 * row comes from the request rather than from the student, so both the caller's
 * right to the section and each student's membership of it have to be checked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAttendanceMarkingTest {

    private static final long CLASS_ID = 5L;
    private static final long SECTION_ID = 50L;
    private static final long OTHER_SECTION_ID = 51L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    @Mock
    private StudentAttendanceRepository studentAttendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentAccessGuard studentAccessGuard;

    @Mock
    private SectionAccessGuard sectionAccessGuard;

    @InjectMocks
    private StudentAttendanceServiceImpl service;

    private SchoolClass schoolClass;
    private Section section;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setDeleted(false);

        section = new Section();
        section.setId(SECTION_ID);

        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        when(studentAttendanceRepository.findByStudentIdAndAttendanceDate(anyLong(), any()))
                .thenReturn(Optional.empty());

        signIn();
    }

    @Test
    void marksEveryStudentInTheSection() {
        Student student = student(100L, CLASS_ID, SECTION_ID);
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));

        int marked = service.mark(request(100L));

        assertThat(marked).isEqualTo(1);
        verify(studentAttendanceRepository).save(any(StudentAttendance.class));
    }

    /**
     * The register is the class teacher's, so marking is gated on the homeroom
     * check rather than the wider "assigned to this section" one — a subject
     * teacher may read this section's attendance and enter its marks without being
     * able to mark it present.
     */
    @Test
    void checksTheSectionBeforeTouchingAnyRow() {
        doThrow(new AccessDeniedException("not yours"))
                .when(sectionAccessGuard).verifyIsHomeroomOrManagement(CLASS_ID, SECTION_ID);

        assertThatThrownBy(() -> service.mark(request(100L)))
                .isInstanceOf(AccessDeniedException.class);

        // Nothing read, nothing written: an unauthorised batch must not partially apply.
        verify(studentRepository, never()).findById(anyLong());
        verify(studentAttendanceRepository, never()).save(any());
    }

    /**
     * The regression this module exists for. A caller legitimately authorised for
     * section A could previously submit a student from section B, and the record
     * would be filed under A — writing attendance for a student they have no claim
     * to and corrupting that student's class history at the same time.
     */
    @Test
    void refusesAStudentFromAnotherSection() {
        Student outsider = student(200L, CLASS_ID, OTHER_SECTION_ID);
        when(studentRepository.findById(200L)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> service.mark(request(200L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not enrolled");

        verify(studentAttendanceRepository, never()).save(any());
    }

    @Test
    void refusesAStudentFromAnotherClass() {
        Student outsider = student(300L, 6L, SECTION_ID);
        when(studentRepository.findById(300L)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> service.mark(request(300L)))
                .isInstanceOf(BadRequestException.class);

        verify(studentAttendanceRepository, never()).save(any());
    }

    /**
     * The whole batch is rejected rather than the good rows being kept: a partially
     * applied register is harder to notice, and to undo, than a failed one.
     */
    @Test
    void rejectsTheWholeBatchWhenOneStudentDoesNotBelong() {
        Student insider = student(100L, CLASS_ID, SECTION_ID);
        Student outsider = student(200L, CLASS_ID, OTHER_SECTION_ID);
        when(studentRepository.findById(100L)).thenReturn(Optional.of(insider));
        when(studentRepository.findById(200L)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> service.mark(request(100L, 200L)))
                .isInstanceOf(BadRequestException.class);
    }

    /* --------------------------------------------------------------- */

    private void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new com.school.sms.security.UserPrincipal(
                                7L, "teacher", "teacher@school.edu", "hash", true,
                                "TEACHER", java.util.Set.of(),
                                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")), false),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
    }

    private Student student(Long id, Long classId, Long sectionId) {
        SchoolClass cls = new SchoolClass();
        cls.setId(classId);
        Section sec = new Section();
        sec.setId(sectionId);

        Student student = new Student();
        student.setId(id);
        student.setSchoolClass(cls);
        student.setSection(sec);
        return student;
    }

    private MarkStudentAttendanceRequest request(Long... studentIds) {
        List<StudentAttendanceRecordItem> items = java.util.Arrays.stream(studentIds)
                .map(id -> new StudentAttendanceRecordItem(id, AttendanceStatus.PRESENT, null))
                .toList();

        MarkStudentAttendanceRequest request = new MarkStudentAttendanceRequest();
        request.setClassId(CLASS_ID);
        request.setSectionId(SECTION_ID);
        request.setAttendanceDate(DATE);
        request.setRecords(items);
        return request;
    }
}
