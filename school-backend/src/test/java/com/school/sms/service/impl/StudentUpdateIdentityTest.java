package com.school.sms.service.impl;

import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;

import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.mapper.GuardianMapper;
import com.school.sms.mapper.MedicalDetailsMapper;
import com.school.sms.mapper.StudentDocumentMapper;
import com.school.sms.mapper.StudentMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.GuardianRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentDocumentRepository;
import com.school.sms.repository.StudentMedicalDetailsRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Editing a student's name, email or phone.
 *
 * These three live on the linked {@code users} row, not on {@code students}, so
 * they cannot travel through the entity mapper. They were previously missing from
 * {@link StudentUpdateRequest} entirely while the web form sent them anyway — and
 * because {@code FAIL_ON_UNKNOWN_PROPERTIES} is disabled, Jackson dropped them
 * without a word. The edit returned 200 and changed nothing, which is why these
 * tests assert the write actually lands rather than just that the call succeeds.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentUpdateIdentityTest {

    private static final long STUDENT_ID = 100L;
    private static final long USER_ID = 500L;
    private static final long CLASS_ID = 5L;
    private static final long SECTION_ID = 50L;
    private static final long YEAR_ID = 1L;

    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.school.sms.repository.RoleRepository roleRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentMedicalDetailsRepository medicalDetailsRepository;
    @Mock private StudentDocumentRepository studentDocumentRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private com.school.sms.service.EmailService emailService;
    @Mock private com.school.sms.service.FileStorageService fileStorageService;
    @Mock private AuditLogService auditLogService;
    @Mock private StudentAccessGuard studentAccessGuard;
    @Mock private StudentMapper studentMapper;
    @Mock private GuardianMapper guardianMapper;
    @Mock private MedicalDetailsMapper medicalDetailsMapper;
    @Mock private StudentDocumentMapper studentDocumentMapper;

    @InjectMocks
    private StudentServiceImpl service;

    private Student student;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setUsername("aarav");
        user.setFirstName("Aarav");
        user.setLastName("Sharma");
        user.setEmail("aarav@school.edu");
        user.setPhone("+91-9999900010");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setDeleted(false);

        Section section = new Section();
        section.setId(SECTION_ID);

        AcademicYear year = new AcademicYear();
        year.setId(YEAR_ID);

        student = new Student();
        student.setId(STUDENT_ID);
        student.setUser(user);
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setDeleted(false);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        when(academicYearRepository.findById(YEAR_ID)).thenReturn(Optional.of(year));
        when(guardianRepository.findAllByStudentIdOrderByIdAsc(STUDENT_ID)).thenReturn(List.of());
        when(studentDocumentRepository.findAllByStudentIdOrderByIdDesc(STUDENT_ID)).thenReturn(List.of());
        when(medicalDetailsRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
        // The mapper is mocked, so the response DTO has to be stubbed or toFullDto
        // NPEs on the way out. These tests assert on the persisted entities rather
        // than on this object.
        when(studentMapper.toDto(any(Student.class))).thenReturn(new com.school.sms.dto.response.StudentDto());
    }

    @Test
    @DisplayName("editing the name actually writes to the linked user account")
    void updatesTheNameOnTheLinkedUser() {
        service.update(STUDENT_ID, request(r -> {
            r.setFirstName("Aarav Kumar");
            r.setLastName("Verma");
        }));

        assertThat(user.getFirstName()).isEqualTo("Aarav Kumar");
        assertThat(user.getLastName()).isEqualTo("Verma");
        verify(userRepository).save(user);
    }

    @Test
    void updatesEmailAndPhone() {
        when(userRepository.findByEmail("new@school.edu")).thenReturn(Optional.empty());

        service.update(STUDENT_ID, request(r -> {
            r.setEmail("new@school.edu");
            r.setPhone("+91-9000000001");
        }));

        assertThat(user.getEmail()).isEqualTo("new@school.edu");
        assertThat(user.getPhone()).isEqualTo("+91-9000000001");
    }

    /** Only the academic half changed, so the user row should not be touched at all. */
    @Test
    void leavesTheUserAloneWhenNoIdentityFieldIsSent() {
        service.update(STUDENT_ID, request(r -> { }));

        assertThat(user.getFirstName()).isEqualTo("Aarav");
        verify(userRepository, never()).save(any());
    }

    /** Re-sending the student's own email must not be mistaken for a collision. */
    @Test
    void acceptsTheUsersOwnEmailUnchanged() {
        when(userRepository.findByEmail("aarav@school.edu")).thenReturn(Optional.of(user));

        service.update(STUDENT_ID, request(r -> r.setEmail("aarav@school.edu")));

        assertThat(user.getEmail()).isEqualTo("aarav@school.edu");
    }

    /**
     * users.email is UNIQUE, so without this check the save fails as a constraint
     * violation — a 409 with no indication of which field is at fault.
     */
    @Test
    void rejectsAnEmailAlreadyUsedByAnotherAccount() {
        User someoneElse = new User();
        someoneElse.setId(999L);
        someoneElse.setEmail("taken@school.edu");
        when(userRepository.findByEmail("taken@school.edu")).thenReturn(Optional.of(someoneElse));

        assertThatThrownBy(() -> service.update(STUDENT_ID, request(r -> r.setEmail("taken@school.edu"))))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(user.getEmail()).isEqualTo("aarav@school.edu");
    }

    /**
     * students.user_id is nullable, so a student can exist with no login — four did
     * in the live database, and they had no name anywhere because the name was only
     * ever stored on the account.
     *
     * The name now lives on the student row, so this must succeed rather than be
     * rejected. An earlier version of this fix threw here, which made exactly those
     * records permanently uneditable.
     */
    @Test
    void namesAStudentWhoHasNoLoginAccount() {
        student.setUser(null);

        service.update(STUDENT_ID, request(r -> {
            r.setFirstName("No");
            r.setLastName("Login");
        }));

        assertThat(student.getFirstName()).isEqualTo("No");
        assertThat(student.getLastName()).isEqualTo("Login");
        verify(userRepository, never()).save(any());
    }

    /** With an account present, both rows are written so the two cannot drift apart. */
    @Test
    void mirrorsTheNameOntoTheLinkedAccountWhenOneExists() {
        service.update(STUDENT_ID, request(r -> r.setFirstName("Mirrored")));

        assertThat(student.getFirstName()).isEqualTo("Mirrored");
        assertThat(user.getFirstName()).isEqualTo("Mirrored");
    }

    /* --------------------------------------------------------------- */

    private StudentUpdateRequest request(java.util.function.Consumer<StudentUpdateRequest> customiser) {
        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setClassId(CLASS_ID);
        request.setSectionId(SECTION_ID);
        request.setAcademicYearId(YEAR_ID);
        customiser.accept(request);
        return request;
    }
}
