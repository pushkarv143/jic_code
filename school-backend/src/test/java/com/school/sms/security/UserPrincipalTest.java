package com.school.sms.security;

import com.school.sms.entity.Role;
import com.school.sms.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void grantsTheRoleWithSpringsRolePrefix() {
        UserPrincipal principal = UserPrincipal.create(user("TEACHER"), List.of());

        assertThat(authorities(principal)).containsExactly("ROLE_TEACHER");
        assertThat(principal.getRoleName()).isEqualTo("TEACHER");
        assertThat(principal.hasRole("TEACHER")).isTrue();
        assertThat(principal.hasRole("STUDENT", "PARENT")).isFalse();
    }

    @Test
    void grantsPermissionsUnderTheirOwnPrefix() {
        UserPrincipal principal = UserPrincipal.create(user("TEACHER"),
                List.of("STUDENT_VIEW", "ATTENDANCE_MARK"));

        assertThat(authorities(principal))
                .containsExactlyInAnyOrder("ROLE_TEACHER", "PERM_STUDENT_VIEW", "PERM_ATTENDANCE_MARK");
        assertThat(principal.getPermissions()).containsExactlyInAnyOrder("STUDENT_VIEW", "ATTENDANCE_MARK");
    }

    /**
     * The reason permissions get their own prefix: ROLE_VIEW and ROLE_MANAGE are real
     * permission names in the seed data. Granted verbatim they would read as Spring
     * Security roles, making hasRole('VIEW') true for anyone who can view roles.
     */
    @Test
    void permissionsNamedLikeRolesDoNotBecomeRoles() {
        UserPrincipal principal = UserPrincipal.create(user("PRINCIPAL"), List.of("ROLE_VIEW"));

        assertThat(authorities(principal)).containsExactlyInAnyOrder("ROLE_PRINCIPAL", "PERM_ROLE_VIEW");
        assertThat(authorities(principal)).doesNotContain("ROLE_VIEW");
    }

    @Test
    void deduplicatesRepeatedPermissionGrants() {
        UserPrincipal principal = UserPrincipal.create(user("STUDENT"),
                List.of("STUDENT_VIEW", "STUDENT_VIEW"));

        assertThat(authorities(principal)).containsExactlyInAnyOrder("ROLE_STUDENT", "PERM_STUDENT_VIEW");
    }

    @Test
    void singleArgFactoryCarriesNoPermissions() {
        UserPrincipal principal = UserPrincipal.create(user("SUPER_ADMIN"));

        assertThat(principal.getPermissions()).isEmpty();
        assertThat(authorities(principal)).containsExactly("ROLE_SUPER_ADMIN");
    }

    private List<String> authorities(UserPrincipal principal) {
        return principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private User user(String roleName) {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setEmail("demo@school.edu");
        user.setPassword("hash");
        user.setActive(true);
        user.setRole(Role.builder().id(1L).name(roleName).build());
        return user;
    }
}
