package com.school.sms.security;

import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enforces the row-level rules that a plain {@code @PreAuthorize} SpEL expression
 * can't express — who may see <em>which</em> student, as opposed to who may call
 * the endpoint at all. Three scopes exist:
 *
 * <ul>
 *   <li><b>unrestricted</b> — management and office roles see every student.</li>
 *   <li><b>taught</b> — a TEACHER/CLASS_TEACHER sees only students in a section they
 *       are homeroom teacher of or teach a subject in.</li>
 *   <li><b>self</b> — a STUDENT sees only their own record, a PARENT only their
 *       children's.</li>
 * </ul>
 *
 * <p>Two families of methods exist on purpose. {@link #verifyCanView(Long)} and
 * {@link #resolveViewableStudentIds()} apply the <em>self</em> scope only and are
 * the long-standing contract used by the attendance and fee services; widening
 * them would silently change those modules. The
 * {@code ...StudentRecord}/{@code ...DirectoryScope} pair below adds the teacher
 * dimension and is what the student module itself uses.
 */
@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Self-scope check: a STUDENT/PARENT caller must be asking about their own
     * record. A no-op for every other role.
     *
     * @param requestedStudentId the studentId the caller is trying to view; may be null
     *                           (e.g. an unfiltered list endpoint) in which case a
     *                           scoped caller is rejected outright.
     */
    public void verifyCanView(Long requestedStudentId) {
        UserPrincipal principal = requirePrincipal();
        if (!isSelfScoped(principal)) {
            return;
        }
        requireMembership(requestedStudentId, ownStudentIds(principal.getId()));
    }

    /**
     * Returns null when the caller is not self-scoped (i.e. a staff/admin/teacher
     * role that this method treats as able to see everyone), or the list of
     * student ids a STUDENT/PARENT caller may see.
     */
    public List<Long> resolveViewableStudentIds() {
        UserPrincipal principal = requirePrincipal();
        return isSelfScoped(principal) ? ownStudentIds(principal.getId()) : null;
    }

    /**
     * Full student-module check: rejects a STUDENT/PARENT asking about someone
     * else's record <em>and</em> a teacher asking about a student they do not teach.
     * Management/office roles pass through.
     *
     * @throws AccessDeniedException if the caller may not see this student
     */
    public void verifyCanViewStudentRecord(Long requestedStudentId) {
        List<Long> allowed = resolveStudentDirectoryScope();
        if (allowed == null) {
            return;
        }
        requireMembership(requestedStudentId, allowed);
    }

    /**
     * Returns null when the caller may see every student, otherwise the exact set
     * of ids they may see. An empty list is meaningful — a teacher with no class
     * mappings yet sees nobody — and must not be treated as "unrestricted".
     */
    public List<Long> resolveStudentDirectoryScope() {
        UserPrincipal principal = requirePrincipal();

        if (isSelfScoped(principal)) {
            return ownStudentIds(principal.getId());
        }
        if (AppConstants.TEACHING_ROLES.contains(principal.getRoleName())) {
            return teacherRepository.findByUserId(principal.getId())
                    .map(teacher -> studentRepository.findIdsTaughtByTeacherId(teacher.getId()))
                    // A TEACHER login with no teacher record is a data problem, not a
                    // licence to read the whole directory: fail closed.
                    .orElseGet(List::of);
        }
        return null;
    }

    /** True when the caller's own view of the student directory is narrowed at all. */
    public boolean isScoped() {
        return resolveStudentDirectoryScope() != null;
    }

    private void requireMembership(Long requestedStudentId, List<Long> allowedIds) {
        if (requestedStudentId == null) {
            throw new AccessDeniedException("A studentId is required for your role");
        }
        if (!allowedIds.contains(requestedStudentId)) {
            throw new AccessDeniedException("You may only view your own records");
        }
    }

    private UserPrincipal requirePrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
    }

    private boolean isSelfScoped(UserPrincipal principal) {
        return AppConstants.SELF_SCOPED_ROLES.contains(principal.getRoleName());
    }

    private List<Long> ownStudentIds(Long userId) {
        return studentRepository.findByUserId(userId)
                .map(student -> List.of(student.getId()))
                .orElseGet(() -> studentRepository.findAllByParentUserId(userId).stream()
                        .map(com.school.sms.entity.Student::getId)
                        .collect(Collectors.toList()));
    }
}
