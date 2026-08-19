package com.school.sms.service.impl;

import com.school.sms.entity.LeaveApplicantType;
import com.school.sms.entity.LeaveApplication;
import com.school.sms.entity.LeaveStatus;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.Role;
import com.school.sms.entity.User;
import com.school.sms.repository.LeaveApplicationRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.util.AppConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who may decide a leave application.
 *
 * <p>The role alone used to be the whole check, which meant any class teacher in
 * the school could approve any student's leave — including students they had never
 * taught. The rule now is management, or the class teacher of the homeroom that
 * student actually sits in, so the "wrong class" and "not a student at all" cases
 * are the ones worth pinning down.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveDecisionScopeTest {

    private static final long APPROVER_USER_ID = 49L;
    private static final long APPROVER_TEACHER_ID = 42L;
    private static final long OWN_SECTION_ID = 3L;
    private static final long OTHER_SECTION_ID = 6L;

    private static final long OWN_STUDENT_USER_ID = 89L;
    private static final long OTHER_STUDENT_USER_ID = 92L;
    private static final long A_TEACHERS_USER_ID = 27L;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private LeaveApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        Teacher approver = new Teacher();
        approver.setId(APPROVER_TEACHER_ID);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);

        Section homeroom = new Section();
        homeroom.setId(OWN_SECTION_ID);
        homeroom.setSectionName("A");
        homeroom.setSchoolClass(schoolClass);

        Section otherSection = new Section();
        otherSection.setId(OTHER_SECTION_ID);
        otherSection.setSectionName("A");

        Student ownStudent = new Student();
        ownStudent.setId(1L);
        ownStudent.setSection(homeroom);

        Student otherStudent = new Student();
        otherStudent.setId(4L);
        otherStudent.setSection(otherSection);

        when(teacherRepository.findByUserId(APPROVER_USER_ID)).thenReturn(Optional.of(approver));
        when(sectionRepository.findByClassTeacherId(APPROVER_TEACHER_ID)).thenReturn(Optional.of(homeroom));
        when(studentRepository.findByUserId(OWN_STUDENT_USER_ID)).thenReturn(Optional.of(ownStudent));
        when(studentRepository.findByUserId(OTHER_STUDENT_USER_ID)).thenReturn(Optional.of(otherStudent));
        // A teacher's login has no student record — the "not a student" case.
        when(studentRepository.findByUserId(A_TEACHERS_USER_ID)).thenReturn(Optional.empty());

        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(any())).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** Mirrors StudentAccessGuardTest's helper so both build a principal the same way. */
    private void signInAs(Long userId, String roleName) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setEmail("user" + userId + "@school.edu");
        user.setPassword("hash");
        user.setActive(true);
        user.setRole(Role.builder().id(1L).name(roleName).build());

        UserPrincipal principal = UserPrincipal.create(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private LeaveApplication pending(Long applicantUserId, LeaveApplicantType type) {
        LeaveApplication application = LeaveApplication.builder()
                .applicantId(applicantUserId)
                .applicantType(type)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .status(LeaveStatus.PENDING)
                .build();
        application.setId(100L);
        when(leaveApplicationRepository.findById(100L)).thenReturn(Optional.of(application));
        return application;
    }

    @Test
    @DisplayName("a class teacher may approve their own homeroom student's leave")
    void approvesOwnHomeroomStudent() {
        pending(OWN_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        service.approve(100L);

        verify(leaveApplicationRepository).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("a class teacher may NOT approve a student from another class")
    void refusesStudentOfAnotherClass() {
        pending(OTHER_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        assertThatThrownBy(() -> service.approve(100L)).isInstanceOf(AccessDeniedException.class);
        verify(leaveApplicationRepository, never()).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("a class teacher may NOT approve a colleague's leave, even in their own class")
    void refusesTeacherLeave() {
        pending(A_TEACHERS_USER_ID, LeaveApplicantType.TEACHER);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        assertThatThrownBy(() -> service.approve(100L)).isInstanceOf(AccessDeniedException.class);
        verify(leaveApplicationRepository, never()).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("rejecting is held to the same rule as approving")
    void rejectUsesTheSameCheck() {
        pending(OTHER_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        assertThatThrownBy(() -> service.reject(100L)).isInstanceOf(AccessDeniedException.class);
        verify(leaveApplicationRepository, never()).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("a class teacher with no homeroom decides nothing — fails closed")
    void refusesWhenApproverHasNoHomeroom() {
        pending(OWN_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        when(sectionRepository.findByClassTeacherId(APPROVER_TEACHER_ID)).thenReturn(Optional.empty());
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        assertThatThrownBy(() -> service.approve(100L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("a plain TEACHER decides nothing, homeroom or not")
    void refusesPlainTeacher() {
        pending(OWN_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_TEACHER);

        assertThatThrownBy(() -> service.approve(100L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("management approves anything, including a teacher's leave")
    void managementApprovesAnything() {
        pending(A_TEACHERS_USER_ID, LeaveApplicantType.TEACHER);
        signInAs(1L, AppConstants.ROLE_SUPER_ADMIN);

        service.approve(100L);

        verify(leaveApplicationRepository).save(any(LeaveApplication.class));
    }

    @Test
    @DisplayName("the approved application records who decided it")
    void recordsApprover() {
        LeaveApplication application = pending(OWN_STUDENT_USER_ID, LeaveApplicantType.STUDENT);
        signInAs(APPROVER_USER_ID, AppConstants.ROLE_CLASS_TEACHER);

        service.approve(100L);

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(application.getApprovedBy()).isEqualTo(APPROVER_USER_ID);
    }
}
