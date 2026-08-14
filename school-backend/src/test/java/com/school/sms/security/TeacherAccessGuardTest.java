package com.school.sms.security;

import com.school.sms.dto.response.TeacherDto;
import com.school.sms.entity.Role;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.repository.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Teacher access is field-level rather than row-level: everyone entitled to the
 * staff directory sees every teacher, but salary and the personal fields are
 * blanked for anyone who is not management, the accountant, or the teacher
 * themselves.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherAccessGuardTest {

    private static final long OWN_TEACHER_ID = 70L;
    private static final long COLLEAGUE_TEACHER_ID = 71L;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private TeacherAccessGuard guard;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /* --------------------------------------------------------------- */
    /* Who may see the sensitive fields.                                */
    /* --------------------------------------------------------------- */

    @Test
    void managementSeesSensitiveFieldsOnEveryone() {
        signIn(1L, "PRINCIPAL");

        assertThat(guard.canViewSensitiveFields(COLLEAGUE_TEACHER_ID)).isTrue();
    }

    /** Payroll is computed from salary, so withholding it would break payroll. */
    @Test
    void accountantSeesSalaryBecausePayrollDependsOnIt() {
        signIn(2L, "ACCOUNTANT");

        assertThat(guard.canViewSensitiveFields(COLLEAGUE_TEACHER_ID)).isTrue();
    }

    @Test
    void teacherSeesSensitiveFieldsOnTheirOwnRecord() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        assertThat(guard.canViewSensitiveFields(OWN_TEACHER_ID)).isTrue();
    }

    @Test
    void teacherDoesNotSeeSensitiveFieldsOnAColleague() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        assertThat(guard.canViewSensitiveFields(COLLEAGUE_TEACHER_ID)).isFalse();
    }

    @Test
    void receptionistNeverSeesSensitiveFields() {
        signIn(3L, "RECEPTIONIST");

        assertThat(guard.canViewSensitiveFields(COLLEAGUE_TEACHER_ID)).isFalse();
        assertThat(guard.canViewSensitiveFields(null)).isFalse();
    }

    /* --------------------------------------------------------------- */
    /* What redaction actually removes.                                 */
    /* --------------------------------------------------------------- */

    @Test
    void redactionBlanksSalaryAndPersonalFieldsForAColleague() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        TeacherDto dto = guard.redactUnlessPermitted(fullDto(COLLEAGUE_TEACHER_ID));

        assertThat(dto.getSalary()).isNull();
        assertThat(dto.getDateOfBirth()).isNull();
        assertThat(dto.getAddress()).isNull();
        assertThat(dto.getCity()).isNull();
        assertThat(dto.getState()).isNull();
        assertThat(dto.getPincode()).isNull();
        assertThat(dto.getBloodGroup()).isNull();
        assertThat(dto.getEmergencyContact()).isNull();
    }

    /** The directory has to stay useful: name, contact and role survive redaction. */
    @Test
    void redactionKeepsTheFieldsThatMakeTheDirectoryUseful() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        TeacherDto dto = guard.redactUnlessPermitted(fullDto(COLLEAGUE_TEACHER_ID));

        assertThat(dto.getFirstName()).isEqualTo("Vikram");
        assertThat(dto.getEmail()).isEqualTo("vikram@school.edu");
        assertThat(dto.getPhone()).isEqualTo("+91-9999900004");
        assertThat(dto.getEmployeeId()).isEqualTo("EMP-1");
        assertThat(dto.getDepartmentName()).isEqualTo("Academics");
        assertThat(dto.getDesignationName()).isEqualTo("TGT");
    }

    @Test
    void teacherOwnRecordComesBackComplete() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        TeacherDto dto = guard.redactUnlessPermitted(fullDto(OWN_TEACHER_ID));

        assertThat(dto.getSalary()).isEqualByComparingTo(new BigDecimal("55000"));
        assertThat(dto.getAddress()).isEqualTo("221 Green Park");
    }

    @Test
    void managementRecordComesBackComplete() {
        signIn(1L, "SUPER_ADMIN");

        TeacherDto dto = guard.redactUnlessPermitted(fullDto(COLLEAGUE_TEACHER_ID));

        assertThat(dto.getSalary()).isEqualByComparingTo(new BigDecimal("55000"));
        assertThat(dto.getDateOfBirth()).isNotNull();
    }

    @Test
    void redactionIsNullSafe() {
        signIn(1L, "SUPER_ADMIN");

        assertThat(guard.redactUnlessPermitted(null)).isNull();
    }

    /* --------------------------------------------------------------- */
    /* Self-service identity resolution.                                */
    /* --------------------------------------------------------------- */

    @Test
    void resolvesTheCallersOwnTeacherId() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(OWN_TEACHER_ID)));

        assertThat(guard.requireOwnTeacherId()).isEqualTo(OWN_TEACHER_ID);
    }

    @Test
    void teacherLoginWithNoTeacherRecordIsRefusedRatherThanDefaulted() {
        signIn(9L, "TEACHER");
        when(teacherRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireOwnTeacherId())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guard.canViewSensitiveFields(OWN_TEACHER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* --------------------------------------------------------------- */

    private void signIn(Long userId, String roleName) {
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

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        return teacher;
    }

    private TeacherDto fullDto(Long id) {
        return TeacherDto.builder()
                .id(id)
                .firstName("Vikram")
                .lastName("Mehta")
                .email("vikram@school.edu")
                .phone("+91-9999900004")
                .employeeId("EMP-1")
                .departmentName("Academics")
                .designationName("TGT")
                .salary(new BigDecimal("55000"))
                .dateOfBirth(LocalDate.of(1988, 3, 15))
                .address("221 Green Park")
                .city("New Delhi")
                .state("Delhi")
                .pincode("110016")
                .bloodGroup("O+")
                .emergencyContact("+91-9999911004")
                .build();
    }
}
