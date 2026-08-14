package com.school.sms.security;

import com.school.sms.entity.Permission;
import com.school.sms.entity.User;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

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
     */
    private UserPrincipal toPrincipal(User user) {
        List<String> permissionNames = permissionRepository.findAllByRoleId(user.getRole().getId()).stream()
                .map(Permission::getName)
                .toList();
        return UserPrincipal.create(user, permissionNames);
    }
}
