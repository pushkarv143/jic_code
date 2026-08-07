package com.school.sms.security;

import com.school.sms.repository.StudentRepository;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces the "own record only" rule that a plain {@code @PreAuthorize} SpEL
 * expression can't express: a STUDENT may only view their own attendance/fee
 * records, a PARENT only their own children's. Every other role is left to
 * the controller's {@code @PreAuthorize} (this guard is a no-op for them).
 * Reused by StudentAttendanceServiceImpl, StudentFeeServiceImpl and
 * FeePaymentServiceImpl rather than duplicating the lookup three times.
 */
@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

    private static final Set<String> SCOPED_ROLES = Set.of(
            AppConstants.JWT_ROLE_PREFIX + AppConstants.ROLE_STUDENT,
            AppConstants.JWT_ROLE_PREFIX + AppConstants.ROLE_PARENT
    );

    private final StudentRepository studentRepository;

    /**
     * @param requestedStudentId the studentId the caller is trying to view; may be null
     *                           (e.g. an unfiltered list endpoint) in which case a
     *                           scoped caller is restricted to {@link #ownStudentIds()}.
     */
    public void verifyCanView(Long requestedStudentId) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        boolean scoped = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SCOPED_ROLES::contains);
        if (!scoped) {
            return;
        }

        if (requestedStudentId == null) {
            throw new AccessDeniedException("A studentId is required for your role");
        }

        List<Long> ownIds = ownStudentIds(principal.getId());
        if (!ownIds.contains(requestedStudentId)) {
            throw new AccessDeniedException("You may only view your own records");
        }
    }

    /**
     * Returns null when the caller is not scoped (i.e. a staff/admin role that
     * can see everyone), or the list of student ids the caller may see
     * otherwise (their own record for STUDENT, their children's for PARENT).
     */
    public List<Long> resolveViewableStudentIds() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        boolean scoped = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SCOPED_ROLES::contains);
        return scoped ? ownStudentIds(principal.getId()) : null;
    }

    private List<Long> ownStudentIds(Long userId) {
        return studentRepository.findByUserId(userId)
                .map(student -> List.of(student.getId()))
                .orElseGet(() -> studentRepository.findAllByParentUserId(userId).stream()
                        .map(com.school.sms.entity.Student::getId)
                        .collect(Collectors.toList()));
    }
}
