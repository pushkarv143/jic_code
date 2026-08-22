package com.school.sms.security;

import com.school.sms.entity.Permission;
import com.school.sms.entity.User;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final OrgModuleRepository orgModuleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username or email: " + usernameOrEmail));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with id: " + id));
        return toPrincipal(user);
    }

    /**
     * JwtAuthenticationFilter re-resolves the principal on every request, so the
     * role_permissions lookup here is what keeps {@code hasAuthority('PERM_...')}
     * checks honest: revoking a permission takes effect on the caller's next
     * request rather than waiting for their access token to expire.
     *
     * <p>The disabled-module filter belongs here for the same reason, and this is
     * the only correct place for it. Filtering only where the UI reads its
     * permissions ({@code GET /api/v1/me/access}) would hide a switched-off
     * module's menu entries while every {@code @PreAuthorize} in the application
     * still let the requests through — the module would look off and behave on.
     * Applying it at the point the authorities are built makes one registry row
     * withdraw the capability from the API and the UI together.
     */
    private UserPrincipal toPrincipal(User user) {
        // One extra query per request against a table of ~20 rows, returning only
        // the keys that are switched off — normally none. Worth the cost to keep a
        // single definition of "this module is off"; if it ever shows up in a
        // profile, cache it rather than moving the filter somewhere less safe.
        Set<String> disabledModules = Set.copyOf(orgModuleRepository.findDisabledModuleKeys());

        List<String> permissionNames = permissionRepository.findAllByRoleId(user.getRole().getId()).stream()
                .filter(permission -> !disabledModules.contains(permission.getModule()))
                .map(Permission::getName)
                .toList();
        return UserPrincipal.create(user, permissionNames);
    }
}
