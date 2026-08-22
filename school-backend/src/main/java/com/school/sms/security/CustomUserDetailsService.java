package com.school.sms.security;

import com.school.sms.entity.Permission;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.util.AppConstants;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final OrgModuleRepository orgModuleRepository;
    private final TeacherRepository teacherRepository;

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
     *
     * <p>The class-teacher flag is applied here for the third time on the same
     * reasoning. A teacher carrying {@code teachers.is_class_teacher} gets the
     * {@code class_teacher_permissions} set added to whatever the TEACHER role
     * holds; their role is unchanged, and clearing the flag withdraws the extra
     * capability from the API and the UI in one step. This is what replaced the
     * CLASS_TEACHER role.
     */
    private UserPrincipal toPrincipal(User user) {
        // One extra query per request against a table of ~20 rows, returning only
        // the keys that are switched off — normally none. Worth the cost to keep a
        // single definition of "this module is off"; if it ever shows up in a
        // profile, cache it rather than moving the filter somewhere less safe.
        Set<String> disabledModules = Set.copyOf(orgModuleRepository.findDisabledModuleKeys());

        // LinkedHashSet, not a List: a permission can now arrive from two places at
        // once — the role and the class-teacher set — and the same authority twice
        // is at best noise in the token and at worst a surprise to anything counting
        // them. Insertion order is kept so the role's own grants read first.
        Set<String> permissionNames = new LinkedHashSet<>();
        for (Permission permission : permissionRepository.findAllByRoleId(user.getRole().getId())) {
            if (!disabledModules.contains(permission.getModule())) {
                permissionNames.add(permission.getName());
            }
        }

        if (isClassTeacher(user)) {
            // Module-filtered on the same terms as the role's grants: switching
            // MY_CLASS off has to darken the homeroom capability for a class
            // teacher too, or the module would look off and behave on for exactly
            // the people it matters most to.
            for (Permission permission : permissionRepository.findClassTeacherPermissions()) {
                if (!disabledModules.contains(permission.getModule())) {
                    permissionNames.add(permission.getName());
                }
            }
        }

        return UserPrincipal.create(user, List.copyOf(permissionNames));
    }

    /**
     * Whether this user is a teacher carrying the class-teacher flag.
     *
     * <p>Costs one indexed lookup on {@code teachers.user_id}, and only for a user
     * whose role could hold the flag at all — every other role skips it, so the
     * common paths (a student, a parent, the administrator) pay nothing.
     */
    private boolean isClassTeacher(User user) {
        if (!AppConstants.ROLE_TEACHER.equals(user.getRole().getName())) {
            return false;
        }
        return teacherRepository.findByUserId(user.getId())
                .map(Teacher::isClassTeacher)
                .orElse(false);
    }
}
