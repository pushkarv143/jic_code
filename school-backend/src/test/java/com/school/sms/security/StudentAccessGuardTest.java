package com.school.sms.security;

import com.school.sms.entity.Role;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.repository.StudentRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The row-level half of RBAC: who may see which student, as opposed to who may
 * call the endpoint. Each test signs a role into the SecurityContext and asserts
 * the scope the guard resolves for it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentAccessGuardTest {

    private static final long OWN_STUDENT_ID = 10L;
    private static final long TAUGHT_STUDENT_ID = 11L;
    private static final long STRANGER_STUDENT_ID = 99L;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private StudentAccessGuard guard;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /* --------------------------------------------------------------- */
    /* Management: unrestricted.                                        */
    /* --------------------------------------------------------------- */

    @Test
    void managementSeesEveryStudent() {
        signIn(1L, "SUPER_ADMIN");

        assertThat(guard.resolveStudentDirectoryScope()).isNull();
        assertThatCode(() -> guard.verifyCanViewStudentRecord(STRANGER_STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void officeRolesAreNotScoped() {
        signIn(2L, "RECEPTIONIST");

        assertThat(guard.resolveStudentDirectoryScope()).isNull();
    }

    /* --------------------------------------------------------------- */
    /* Student: own record only.                                        */
    /* --------------------------------------------------------------- */

    @Test
    void studentSeesOnlyTheirOwnRecord() {
        signIn(5L, "STUDENT");
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.of(student(OWN_STUDENT_ID)));

        assertThat(guard.resolveStudentDirectoryScope()).containsExactly(OWN_STUDENT_ID);
        assertThatCode(() -> guard.verifyCanViewStudentRecord(OWN_STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void studentIsRefusedAnotherStudentsRecord() {
        signIn(5L, "STUDENT");
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.of(student(OWN_STUDENT_ID)));

        assertThatThrownBy(() -> guard.verifyCanViewStudentRecord(STRANGER_STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* --------------------------------------------------------------- */
    /* Parent: their children only.                                     */
    /* --------------------------------------------------------------- */

    @Test
    void parentSeesOnlyTheirOwnChildren() {
        signIn(6L, "PARENT");
        when(studentRepository.findByUserId(6L)).thenReturn(Optional.empty());
        when(studentRepository.findAllByParentUserId(6L)).thenReturn(List.of(student(OWN_STUDENT_ID)));

        assertThat(guard.resolveStudentDirectoryScope()).containsExactly(OWN_STUDENT_ID);
        assertThatThrownBy(() -> guard.verifyCanViewStudentRecord(STRANGER_STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* --------------------------------------------------------------- */
    /* Teacher: the students they teach.                                */
    /* --------------------------------------------------------------- */

    @Test
    void teacherSeesOnlyStudentsTheyTeach() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(70L)));
        when(studentRepository.findIdsTaughtByTeacherId(70L))
                .thenReturn(List.of(OWN_STUDENT_ID, TAUGHT_STUDENT_ID));

        assertThat(guard.resolveStudentDirectoryScope())
                .containsExactly(OWN_STUDENT_ID, TAUGHT_STUDENT_ID);
        assertThatCode(() -> guard.verifyCanViewStudentRecord(TAUGHT_STUDENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void teacherIsRefusedAStudentTheyDoNotTeach() {
        signIn(7L, "TEACHER");
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher(70L)));
        when(studentRepository.findIdsTaughtByTeacherId(70L)).thenReturn(List.of(TAUGHT_STUDENT_ID));

        assertThatThrownBy(() -> guard.verifyCanViewStudentRecord(STRANGER_STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void classTeacherIsScopedTheSameWayAsTeacher() {
        signIn(8L, "CLASS_TEACHER");
        when(teacherRepository.findByUserId(8L)).thenReturn(Optional.of(teacher(80L)));
        when(studentRepository.findIdsTaughtByTeacherId(80L)).thenReturn(List.of(TAUGHT_STUDENT_ID));

        assertThat(guard.resolveStudentDirectoryScope()).containsExactly(TAUGHT_STUDENT_ID);
    }

    /**
     * A TEACHER login with no teacher row is a data problem. It must fail closed —
     * returning null here would read as "unrestricted" and hand them the whole
     * directory, which is the exact inversion the guard exists to prevent.
     */
    @Test
    void teacherWithNoTeacherRecordSeesNobodyRatherThanEveryone() {
        signIn(9L, "TEACHER");
        when(teacherRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThat(guard.resolveStudentDirectoryScope()).isEmpty();
        assertThatThrownBy(() -> guard.verifyCanViewStudentRecord(TAUGHT_STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* --------------------------------------------------------------- */
    /* The pre-existing self-only contract used by attendance/fees.     */
    /* --------------------------------------------------------------- */

    @Test
    void verifyCanViewLeavesTeachersAloneSoAttendanceAndFeesAreUnchanged() {
        signIn(7L, "TEACHER");

        assertThat(guard.resolveViewableStudentIds()).isNull();
        assertThatCode(() -> guard.verifyCanView(STRANGER_STUDENT_ID)).doesNotThrowAnyException();
    }

    @Test
    void verifyCanViewStillScopesStudents() {
        signIn(5L, "STUDENT");
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.of(student(OWN_STUDENT_ID)));

        assertThatThrownBy(() -> guard.verifyCanView(STRANGER_STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** A scoped caller with no student id to check against is refused, not waved through. */
    @Test
    void scopedCallerWithoutAStudentIdIsRefused() {
        signIn(5L, "STUDENT");
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.of(student(OWN_STUDENT_ID)));

        assertThatThrownBy(() -> guard.verifyCanViewStudentRecord(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guard.resolveStudentDirectoryScope())
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

    private Student student(Long id) {
        Student student = new Student();
        student.setId(id);
        return student;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        return teacher;
    }
}
