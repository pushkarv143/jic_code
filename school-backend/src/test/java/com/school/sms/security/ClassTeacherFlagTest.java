package com.school.sms.security;

import com.school.sms.entity.Permission;
import com.school.sms.entity.Role;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The class-teacher flag, where it actually takes effect.
 *
 * <p>{@code teachers.is_class_teacher} replaced the CLASS_TEACHER role. The two
 * were never two kinds of person — the role held exactly TEACHER's permissions plus
 * six — and the identity did not match reality: 44 users carried it while only 17
 * appeared in {@code sections.class_teacher_id}, so 27 people held homeroom
 * privileges over a homeroom they did not have.
 *
 * <p>What matters is that the flag grants those six <em>at the point authorities
 * are built</em>, because that is the single place both {@code @PreAuthorize} and
 * the UI's permission list are fed from. A flag that only changed one of them would
 * leave the API and the interface disagreeing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassTeacherFlagTest {

    private static final long USER_ID = 49L;

    @Mock private UserRepository userRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private OrgModuleRepository orgModuleRepository;
    @Mock private TeacherRepository teacherRepository;

    @InjectMocks private CustomUserDetailsService service;

    private static Permission permission(String name, String module) {
        Permission p = new Permission();
        p.setName(name);
        p.setModule(module);
        return p;
    }

    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setId(4L);
        role.setName(roleName);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("teacher42");
        user.setPassword("hash");
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    private void given(String roleName, Boolean flag, List<String> disabledModules) {
        User user = userWithRole(roleName);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orgModuleRepository.findDisabledModuleKeys()).thenReturn(disabledModules);
        when(permissionRepository.findAllByRoleId(4L))
                .thenReturn(List.of(permission("STUDENT_VIEW", "STUDENT")));
        when(permissionRepository.findClassTeacherPermissions()).thenReturn(List.of(
                permission("MY_CLASS_VIEW", "MY_CLASS"),
                permission("MY_CLASS_ROSTER_MANAGE", "MY_CLASS"),
                permission("STUDENT_UPDATE", "STUDENT")));

        if (flag == null) {
            when(teacherRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        } else {
            Teacher teacher = new Teacher();
            teacher.setId(80L);
            teacher.setClassTeacher(flag);
            when(teacherRepository.findByUserId(USER_ID)).thenReturn(Optional.of(teacher));
        }
    }

    private List<String> authoritiesOf() {
        return service.loadUserById(USER_ID).getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
    }

    @Test
    @DisplayName("a teacher carrying the flag gets the extra permissions on top of their role")
    void flagGrantsTheExtraPermissions() {
        given("TEACHER", true, List.of());

        assertThat(authoritiesOf()).contains(
                "ROLE_TEACHER",
                "PERM_STUDENT_VIEW",
                "PERM_MY_CLASS_VIEW",
                "PERM_MY_CLASS_ROSTER_MANAGE",
                "PERM_STUDENT_UPDATE");
    }

    @Test
    @DisplayName("the same teacher without the flag has plain TEACHER permissions and nothing more")
    void withoutTheFlagNothingExtraIsGranted() {
        given("TEACHER", false, List.of());

        List<String> authorities = authoritiesOf();
        assertThat(authorities).contains("ROLE_TEACHER", "PERM_STUDENT_VIEW");
        assertThat(authorities).doesNotContain(
                "PERM_MY_CLASS_VIEW", "PERM_MY_CLASS_ROSTER_MANAGE", "PERM_STUDENT_UPDATE");
    }

    @Test
    @DisplayName("the base role is unchanged either way — the flag adds permissions, not an identity")
    void theRoleNeverChanges() {
        given("TEACHER", true, List.of());
        assertThat(service.loadUserById(USER_ID)).isInstanceOf(UserPrincipal.class)
                .satisfies(principal -> assertThat(((UserPrincipal) principal).getRoleName())
                        .isEqualTo("TEACHER"));
    }

    @Test
    @DisplayName("switching the module off withdraws the flag's grants too")
    void theModuleFilterAppliesToTheFlagsGrants() {
        // Otherwise MY_CLASS would look off and behave on for exactly the people it
        // matters most to — the ones who hold a homeroom.
        given("TEACHER", true, List.of("MY_CLASS"));

        List<String> authorities = authoritiesOf();
        assertThat(authorities).doesNotContain("PERM_MY_CLASS_VIEW", "PERM_MY_CLASS_ROSTER_MANAGE");
        // STUDENT_UPDATE is in the flag's set but belongs to a module still on.
        assertThat(authorities).contains("PERM_STUDENT_UPDATE");
    }

    @Test
    @DisplayName("a permission held by both the role and the flag is granted once")
    void noDuplicateAuthorities() {
        User user = userWithRole("TEACHER");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orgModuleRepository.findDisabledModuleKeys()).thenReturn(List.of());
        when(permissionRepository.findAllByRoleId(4L))
                .thenReturn(List.of(permission("STUDENT_UPDATE", "STUDENT")));
        when(permissionRepository.findClassTeacherPermissions())
                .thenReturn(List.of(permission("STUDENT_UPDATE", "STUDENT")));
        Teacher teacher = new Teacher();
        teacher.setClassTeacher(true);
        when(teacherRepository.findByUserId(USER_ID)).thenReturn(Optional.of(teacher));

        assertThat(authoritiesOf()).filteredOn("PERM_STUDENT_UPDATE"::equals).hasSize(1);
    }

    @Test
    @DisplayName("a role that cannot hold the flag is not even looked up")
    void otherRolesPayNothing() {
        given("STUDENT", null, List.of());

        assertThat(authoritiesOf()).doesNotContain("PERM_MY_CLASS_VIEW");
        // The common paths — a student, a parent, the administrator — must not pay
        // for a teacher lookup on every single request.
        verify(teacherRepository, never()).findByUserId(anyLong());
    }
}
