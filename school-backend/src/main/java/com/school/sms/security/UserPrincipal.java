package com.school.sms.security;

import com.school.sms.entity.User;
import com.school.sms.util.AppConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts a {@link User} JPA entity to Spring Security's {@link UserDetails}
 * contract. The entity itself never implements UserDetails to keep the
 * persistence model free of framework concerns.
 *
 * <p>Authorities carry two kinds of grant, deliberately kept apart:
 * <ul>
 *   <li>{@code ROLE_<NAME>} — the single role the user holds, matched by
 *       {@code hasRole(...)} in the existing {@code @PreAuthorize} expressions.</li>
 *   <li>{@code PERM_<NAME>} — one per row of {@code role_permissions}, matched by
 *       {@code hasAuthority('PERM_STUDENT_VIEW')}. The {@code PERM_} prefix is not
 *       cosmetic: permission names like {@code ROLE_VIEW} and {@code ROLE_MANAGE}
 *       would otherwise collide with Spring's {@code ROLE_} convention and make
 *       {@code hasRole('VIEW')} spuriously true.</li>
 * </ul>
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final String roleName;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email, String password,
                          boolean active, String roleName, Set<String> permissions,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.active = active;
        this.roleName = roleName;
        this.permissions = permissions;
        this.authorities = authorities;
    }

    /**
     * Builds a principal carrying only the role grant. Used where permissions are
     * irrelevant — notably JWT issuance, which encodes the user id and re-resolves
     * the full principal (permissions included) on every request.
     */
    public static UserPrincipal create(User user) {
        return create(user, List.of());
    }

    public static UserPrincipal create(User user, Collection<String> permissionNames) {
        String roleName = user.getRole().getName();

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AppConstants.JWT_ROLE_PREFIX + roleName));

        Set<String> perms = new LinkedHashSet<>(permissionNames);
        perms.forEach(name -> authorities.add(
                new SimpleGrantedAuthority(AppConstants.PERMISSION_AUTHORITY_PREFIX + name)));

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                roleName,
                perms,
                List.copyOf(authorities)
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    /** The role name without the {@code ROLE_} prefix, e.g. {@code TEACHER}. */
    public String getRoleName() {
        return roleName;
    }

    /** Permission names without the {@code PERM_} prefix, e.g. {@code STUDENT_VIEW}. */
    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasRole(String... names) {
        for (String name : names) {
            if (name.equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
