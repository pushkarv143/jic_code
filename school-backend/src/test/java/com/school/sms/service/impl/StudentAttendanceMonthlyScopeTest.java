package com.school.sms.service.impl;

import com.school.sms.dto.response.MonthlyAttendanceRowDto;
import com.school.sms.entity.AttendanceStatus;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentAttendance;
import com.school.sms.entity.StudentStatus;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Who sees how much of the monthly register.
 *
 * <p>The register is addressed by section, but section rights are not the only way
 * to be entitled to part of one. A STUDENT/PARENT is failed closed by
 * {@link SectionAccessGuard} by design, so requiring section access outright made
 * the register a guaranteed 403 for exactly the people its self-view was written
 * for. Section access is now a widening grant: hold it and you get the whole
 * roster, otherwise you get the intersection with your own student scope.
 *
 * <p>The interesting case is therefore not "can a student load it" but "does a
 * student loading it get only their own row" — a section-wide response would hand
 * them every classmate's attendance.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAttendanceMonthlyScopeTest {

    private static final long CLASS_ID = 11L;
    private static final long SECTION_ID = 110L;
    private static final long OWN_STUDENT_ID = 1L;
    private static final long CLASSMATE_ID = 2L;
    private static final int YEAR = 2026;
    private static final int MONTH = 8;

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

        // A two-student section: the caller's own child and a classmate.
        when(studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        CLASS_ID, SECTION_ID, StudentStatus.ACTIVE))
                .thenReturn(List.of(student(OWN_STUDENT_ID, "Aarav", 1), student(CLASSMATE_ID, "Diya", 2)));

        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDateBetween(
                anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    private Student student(long id, String firstName, int rollNumber) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setRollNumber(rollNumber);
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    private List<MonthlyAttendanceRowDto> getMonthly() {
        return service.getMonthly(CLASS_ID, SECTION_ID, YEAR, MONTH);
    }

    /* ---- staff: section access is the whole roster ------------------------ */

    @Test
    void aCallerWithSectionAccessSeesEveryStudentInIt() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(true);

        assertThat(getMonthly())
                .extracting(MonthlyAttendanceRowDto::getStudentId)
                .containsExactly(OWN_STUDENT_ID, CLASSMATE_ID);
    }

    /**
     * A section with no active students is empty for staff, not forbidden — the
     * denial below is reserved for callers with no claim on the section at all.
     */
    @Test
    void anEmptySectionStaysEmptyRatherThanBecomingADenialForStaff() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(true);
        when(studentRepository
                .findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
                        CLASS_ID, SECTION_ID, StudentStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(getMonthly()).isEmpty();
    }

    /* ---- families: their own rows, and only their own --------------------- */

    @Test
    void aStudentSeesTheirOwnRowRatherThanBeingRefusedOutright() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of(OWN_STUDENT_ID));

        assertThat(getMonthly())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getStudentId()).isEqualTo(OWN_STUDENT_ID);
                    assertThat(row.getFirstName()).isEqualTo("Aarav");
                });
    }

    /** The point of the scoping: a student must not read the rest of their class. */
    @Test
    void aStudentDoesNotSeeAClassmatesRegister() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of(OWN_STUDENT_ID));

        assertThat(getMonthly())
                .extracting(MonthlyAttendanceRowDto::getStudentId)
                .doesNotContain(CLASSMATE_ID);
    }

    @Test
    void aParentWithTwoChildrenInOneSectionSeesBothOfThem() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope())
                .thenReturn(List.of(OWN_STUDENT_ID, CLASSMATE_ID));

        assertThat(getMonthly())
                .extracting(MonthlyAttendanceRowDto::getStudentId)
                .containsExactly(OWN_STUDENT_ID, CLASSMATE_ID);
    }

    @Test
    void theScopedRowStillCarriesItsMarkedDays() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of(OWN_STUDENT_ID));

        StudentAttendance record = StudentAttendance.builder()
                .student(student(OWN_STUDENT_ID, "Aarav", 1))
                .schoolClass(schoolClass)
                .section(section)
                .attendanceDate(LocalDate.of(YEAR, MONTH, 7))
                .status(AttendanceStatus.ABSENT)
                .build();
        when(studentAttendanceRepository.findAllByStudentIdInAndAttendanceDateBetween(
                anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(record));

        assertThat(getMonthly()).singleElement()
                .satisfies(row -> assertThat(row.getDays()).containsEntry("7", "ABSENT"));
    }

    /* ---- no claim on the section at all ----------------------------------- */

    @Test
    void aFamilyAskingForSomeoneElsesSectionIsDenied() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        // Their child is real, but is in some other section than the one requested.
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of(999L));

        assertThatThrownBy(this::getMonthly)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not assigned to this class/section");
    }

    /**
     * A teacher who teaches elsewhere is scoped to students who are not in this
     * section, so the intersection is empty and the denial still stands — the
     * behaviour they had before the register was opened to families.
     */
    @Test
    void aTeacherWhoDoesNotTeachTheSectionIsStillDenied() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of(41L, 42L));

        assertThatThrownBy(this::getMonthly).isInstanceOf(AccessDeniedException.class);
    }

    /**
     * A teacher with no class mappings yet legitimately sees nobody; an empty scope
     * must not degrade into "unrestricted".
     */
    @Test
    void anEmptyScopeIsADenialRatherThanTheWholeRoster() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(List.of());

        assertThatThrownBy(this::getMonthly).isInstanceOf(AccessDeniedException.class);
    }

    /**
     * An unscoped caller (null scope) who nonetheless failed the section check has
     * no claim on this section either — it must not fall through to the roster.
     */
    @Test
    void anUnscopedCallerWithoutSectionAccessIsDenied() {
        when(sectionAccessGuard.canAccessSection(CLASS_ID, SECTION_ID)).thenReturn(false);
        when(studentAccessGuard.resolveStudentDirectoryScope()).thenReturn(null);

        assertThatThrownBy(this::getMonthly).isInstanceOf(AccessDeniedException.class);
    }
}
