package com.school.sms.security;

import com.school.sms.dto.response.TeacherDto;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decides how much of a teacher's record the caller may see.
 *
 * <p>Teacher access is a different shape of problem from student access. A teacher
 * has a legitimate need to see the staff directory — who teaches what, how to
 * contact a colleague — so narrowing the <em>rows</em> the way
 * {@link StudentAccessGuard} does would break a feature people actually use. What
 * must not leak is the sensitive half of the record: salary, date of birth, home
 * address, emergency contact and blood group. So the control here is field-level:
 * everyone entitled to the directory sees every teacher, but only these callers
 * see the sensitive fields:
 *
 * <ul>
 *   <li>management (SUPER_ADMIN / PRINCIPAL / VICE_PRINCIPAL) — HR responsibility;</li>
 *   <li>ACCOUNTANT — payroll is computed from salary, so withholding it would
 *       break the payroll module rather than protect anything;</li>
 *   <li>any teacher looking at their own record.</li>
 * </ul>
 *
 * <p>Redaction happens on the DTO on its way out, after mapping, so no caller can
 * reach the fields by asking for a different projection.
 */
@Component
@RequiredArgsConstructor
public class TeacherAccessGuard {

    private final TeacherRepository teacherRepository;

    /**
     * @param teacherId the teacher whose record is being read; null when the caller
     *                  is reading a list, in which case only role decides.
     */
    public boolean canViewSensitiveFields(Long teacherId) {
        UserPrincipal principal = requirePrincipal();

        if (AppConstants.MANAGEMENT_ROLES.contains(principal.getRoleName())
                || AppConstants.ROLE_ACCOUNTANT.equals(principal.getRoleName())) {
            return true;
        }
        return teacherId != null && teacherId.equals(ownTeacherId(principal.getId()).orElse(null));
    }

    /**
     * Blanks the sensitive fields unless the caller is entitled to them, and returns
     * the same instance for chaining. Null-safe so callers can pipe a mapper result
     * straight through.
     */
    public TeacherDto redactUnlessPermitted(TeacherDto dto) {
        if (dto == null || canViewSensitiveFields(dto.getId())) {
            return dto;
        }
        dto.setSalary(null);
        dto.setDateOfBirth(null);
        dto.setAddress(null);
        dto.setCity(null);
        dto.setState(null);
        dto.setPincode(null);
        dto.setBloodGroup(null);
        dto.setEmergencyContact(null);
        return dto;
    }

    /**
     * The caller's own teacher id.
     *
     * @throws AccessDeniedException when the caller has no teacher record — a
     *         TEACHER login without one is a data problem, and the self-service
     *         endpoints have nothing to resolve for them.
     */
    public Long requireOwnTeacherId() {
        UserPrincipal principal = requirePrincipal();
        return ownTeacherId(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Your login is not linked to a teacher record"));
    }

    /**
     * The caller's own teacher id if they have one, empty otherwise.
     *
     * <p>Unlike {@link #requireOwnTeacherId()} this does not throw: it exists for
     * the endpoints that serve several kinds of caller from one path (a "my
     * timetable" that answers a teacher with their teaching week and a student
     * with their class's week), where having no teacher record is an ordinary
     * branch rather than a failure.
     */
    public Optional<Long> findOwnTeacherId() {
        return SecurityUtils.getCurrentUserPrincipal().flatMap(p -> ownTeacherId(p.getId()));
    }

    private Optional<Long> ownTeacherId(Long userId) {
        return teacherRepository.findByUserId(userId).map(com.school.sms.entity.Teacher::getId);
    }

    private UserPrincipal requirePrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
    }
}
