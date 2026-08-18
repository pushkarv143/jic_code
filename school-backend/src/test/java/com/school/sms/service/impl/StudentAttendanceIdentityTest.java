package com.school.sms.service.impl;

import com.school.sms.dto.response.MonthlyAttendanceRowDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentAttendanceRecordDto;
import com.school.sms.dto.response.StudentAttendanceRowDto;
import com.school.sms.entity.AttendanceStatus;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentAttendance;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.User;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * A student's name is an attribute of the student, not of the login account they
 * may never have been given (students.user_id is nullable). Every attendance
 * response that carries a name has to read it from the student for that reason —
 * sourcing it from the joined User returns null for anyone admitted without a
 * login, which the web client renders as "Unnamed Student" and the Android client
 * as a bare roll number.
 *
 * <p>Covers all three name-carrying responses in the module: the marking grid,
 * the monthly register and the paginated report.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAttendanceIdentityTest {

    private static final long CLASS_ID = 11L;
    private static final long SECTION_ID = 110L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 18);

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
        schoolClass.setClassName("Class 11 Science");

        section = new Section();
        section.setId(SECTION_ID);
        section.setSectionName("A");
    }

    /** A student admitted without a login: identity lives only on the student row. */
    private Student studentWithoutLogin() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Aarav");
        student.setLastName("Verma");
        student.setRollNumber(123);
        student.setAdmissionNumber("ADM-001");
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    /**
     * A student whose login pre-dates the identity migration: the student row was
     * never populated, so the account is the only place a name survives.
     */
    private Student studentWithLoginOnly() {
        Student student = new Student();
        student.setId(2L);
        student.setRollNumber(124);
        student.setAdmissionNumber("ADM-002");
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setStatus(StudentStatus.ACTIVE);

        User user = new User();
        user.setId(900L);
        user.setFirstName("Legacy");
        user.setLastName("Account");
        student.setUser(user);
        return student;
    }

    private void stubRoster(Student... students) {
        when(studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        CLASS_ID, SECTION_ID, StudentStatus.ACTIVE))
                .thenReturn(List.of(students));
    }

    /* ---- marking grid ---------------------------------------------------- */

    @Test
    void gridNamesAStudentWhoHasNoLoginAccount() {
        stubRoster(studentWithoutLogin());
        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDate(anyList(), eq(DATE)))
                .thenReturn(List.of());

        List<StudentAttendanceRowDto> rows = service.getGrid(CLASS_ID, SECTION_ID, DATE);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getFirstName()).isEqualTo("Aarav");
            assertThat(row.getLastName()).isEqualTo("Verma");
            assertThat(row.getRollNumber()).isEqualTo(123);
            // Not yet marked for this date.
            assertThat(row.getStatus()).isNull();
        });
    }

    @Test
    void gridFallsBackToTheLinkedAccountWhenTheStudentRowHasNoName() {
        stubRoster(studentWithLoginOnly());
        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDate(anyList(), eq(DATE)))
                .thenReturn(List.of());

        List<StudentAttendanceRowDto> rows = service.getGrid(CLASS_ID, SECTION_ID, DATE);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getFirstName()).isEqualTo("Legacy");
            assertThat(row.getLastName()).isEqualTo("Account");
        });
    }

    @Test
    void gridCarriesAnAlreadyMarkedStatusAndRemarks() {
        Student student = studentWithoutLogin();
        stubRoster(student);

        StudentAttendance record = StudentAttendance.builder()
                .student(student)
                .schoolClass(schoolClass)
                .section(section)
                .attendanceDate(DATE)
                .status(AttendanceStatus.LATE)
                .remarks("Bus delayed")
                .build();
        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDate(anyList(), eq(DATE)))
                .thenReturn(List.of(record));

        List<StudentAttendanceRowDto> rows = service.getGrid(CLASS_ID, SECTION_ID, DATE);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getFirstName()).isEqualTo("Aarav");
            assertThat(row.getStatus()).isEqualTo("LATE");
            assertThat(row.getRemarks()).isEqualTo("Bus delayed");
        });
    }

    /* ---- monthly register ------------------------------------------------ */

    @Test
    void monthlyRegisterNamesAStudentWhoHasNoLoginAccount() {
        Student student = studentWithoutLogin();
        stubRoster(student);
        // A staff caller, so the register is not narrowed to a student scope —
        // StudentAttendanceMonthlyScopeTest covers the narrowing itself.
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(true);

        StudentAttendance record = StudentAttendance.builder()
                .student(student)
                .schoolClass(schoolClass)
                .section(section)
                .attendanceDate(LocalDate.of(2026, 8, 3))
                .status(AttendanceStatus.PRESENT)
                .build();
        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDateBetween(
                anyList(), eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 31))))
                .thenReturn(List.of(record));

        List<MonthlyAttendanceRowDto> rows = service.getMonthly(CLASS_ID, SECTION_ID, 2026, 8);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getFirstName()).isEqualTo("Aarav");
            assertThat(row.getLastName()).isEqualTo("Verma");
            assertThat(row.getDays()).containsEntry("3", "PRESENT");
        });
    }

    /* ---- paginated report ------------------------------------------------ */

    @Test
    void reportNamesAStudentWhoHasNoLoginAccount() {
        Student student = studentWithoutLogin();

        StudentAttendance record = StudentAttendance.builder()
                .student(student)
                .schoolClass(schoolClass)
                .section(section)
                .attendanceDate(DATE)
                .status(AttendanceStatus.PRESENT)
                .build();

        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(null);
        when(studentAttendanceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        PageResponse<StudentAttendanceRecordDto> page =
                service.getReport(null, CLASS_ID, SECTION_ID, DATE, DATE, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement().satisfies(row -> {
            assertThat(row.getFirstName()).isEqualTo("Aarav");
            assertThat(row.getLastName()).isEqualTo("Verma");
            assertThat(row.getAdmissionNumber()).isEqualTo("ADM-001");
            assertThat(row.getClassName()).isEqualTo("Class 11 Science");
            assertThat(row.getSectionName()).isEqualTo("A");
            assertThat(row.getStatus()).isEqualTo("PRESENT");
        });
    }
}
