package com.school.sms.security;

import com.school.sms.entity.Role;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.SectionRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Section scoping: may this caller act on this class/section at all? Used by
 * attendance to gate the marking grid and the batch write before any row is
 * touched.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SectionAccessGuardTest {

    private static final long TEACHER_ID = 70L;
    private static final long CLASS_ID = 5L;
    private static final long TAUGHT_SECTION = 50L;
    private static final long OTHER_SECTION = 51L;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ClassSubjectTeacherRepository classSubjectTeacherRepository;

    @InjectMocks
    private SectionAccessGuard guard;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managementMayActOnAnySection() {
        signIn(1L, "PRINCIPAL");

        assertThat(guard.canAccessSection(CLASS_ID, OTHER_SECTION)).isTrue();
        assertThatCode(() -> guard.verifyCanAccessSection(CLASS_ID, OTHER_SECTION))
                .doesNotThrowAnyException();
    }

    @Test
    void officeRolesMayActOnAnySection() {
        signIn(2L, "RECEPTIONIST");

        assertThat(guard.canAccessSection(CLASS_ID, OTHER_SECTION)).isTrue();
    }

    /** Subject mapping is one of the two ways a teacher reaches a section. */
    @Test
    void subjectTeacherMayActOnASectionTheyTeach() {
        signInTeacher();
        when(classSubjectTeacherRepository
                .existsByTeacherIdAndSchoolClassIdAndSectionId(TEACHER_ID, CLASS_ID, TAUGHT_SECTION))
                .thenReturn(true);

        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isTrue();
    }

    /**
     * Homeroom is the other, and it is independent: a class teacher owns their
     * section even with no subject mapping in it.
     */
    @Test
    void homeroomTeacherMayActOnTheirSectionWithoutASubjectMapping() {
        signInTeacher();
        when(sectionRepository.existsByIdAndSchoolClassIdAndClassTeacherId(TAUGHT_SECTION, CLASS_ID, TEACHER_ID))
                .thenReturn(true);
        when(classSubjectTeacherRepository
                .existsByTeacherIdAndSchoolClassIdAndSectionId(TEACHER_ID, CLASS_ID, TAUGHT_SECTION))
                .thenReturn(false);

        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isTrue();
    }

    @Test
    void teacherIsRefusedASectionTheyNeitherTeachNorOwn() {
        signInTeacher();
        when(sectionRepository.existsByIdAndSchoolClassIdAndClassTeacherId(OTHER_SECTION, CLASS_ID, TEACHER_ID))
                .thenReturn(false);
        when(classSubjectTeacherRepository
                .existsByTeacherIdAndSchoolClassIdAndSectionId(TEACHER_ID, CLASS_ID, OTHER_SECTION))
                .thenReturn(false);

        assertThat(guard.canAccessSection(CLASS_ID, OTHER_SECTION)).isFalse();
        assertThatThrownBy(() -> guard.verifyCanAccessSection(CLASS_ID, OTHER_SECTION))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Both ids are matched, so a section id belonging to a different class cannot be
     * paired with a class the teacher does teach to slip past the check.
     */
    @Test
    void teacherIsRefusedWhenTheSectionBelongsToADifferentClass() {
        signInTeacher();
        long otherClassId = 6L;
        when(sectionRepository.existsByIdAndSchoolClassIdAndClassTeacherId(TAUGHT_SECTION, otherClassId, TEACHER_ID))
                .thenReturn(false);
        when(classSubjectTeacherRepository
                .existsByTeacherIdAndSchoolClassIdAndSectionId(TEACHER_ID, otherClassId, TAUGHT_SECTION))
                .thenReturn(false);

        assertThat(guard.canAccessSection(otherClassId, TAUGHT_SECTION)).isFalse();
    }

    @Test
    void teacherWithNoTeacherRecordIsRefusedRatherThanWavedThrough() {
        signIn(9L, "TEACHER");
        when(teacherRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isFalse();
    }

    @Test
    void teacherIsRefusedWhenClassOrSectionIsMissing() {
        signInTeacher();

        assertThat(guard.canAccessSection(null, TAUGHT_SECTION)).isFalse();
        assertThat(guard.canAccessSection(CLASS_ID, null)).isFalse();
    }

    @Test
    void studentsAndParentsMayNotAddressASectionDirectly() {
        signIn(5L, "STUDENT");
        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isFalse();

        SecurityContextHolder.clearContext();
        signIn(6L, "PARENT");
        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isFalse();
    }

    @Test
    void unrelatedStaffRolesAreRefused() {
        signIn(4L, "LIBRARIAN");
        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isFalse();

        SecurityContextHolder.clearContext();
        signIn(8L, "SECURITY_GUARD");
        assertThat(guard.canAccessSection(CLASS_ID, TAUGHT_SECTION)).isFalse();
    }

    @Test
    void unauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guard.canAccessSection(CLASS_ID, TAUGHT_SECTION))
                .isInstanceOf(AccessDeniedException.class);
    }

    /* --------------------------------------------------------------- */

    private void signInTeacher() {
        signIn(7L, "TEACHER");
        Teacher teacher = new Teacher();
        teacher.setId(TEACHER_ID);
        when(teacherRepository.findByUserId(7L)).thenReturn(Optional.of(teacher));
    }

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
}
