package com.school.sms.security;

import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Answers "may this caller act on this class/section?".
 *
 * <p>Distinct from {@link StudentAccessGuard}, which scopes by student id. Some
 * operations are addressed by section rather than by student — marking a whole
 * section's attendance, pulling a section's roster or monthly grid — and for those
 * the student-id scope has nothing to check against. Checking the section up front
 * is also stricter than checking each student afterwards: it rejects the request
 * before any row is written, rather than letting a partially-authorized batch through.
 *
 * <p>A teacher reaches a section two ways, and both count:
 * <ul>
 *   <li><b>homeroom</b> — {@code sections.class_teacher_id} points at them;</li>
 *   <li><b>subject</b> — they have a {@code class_subject_teacher} row for it.</li>
 * </ul>
 * These are separate grants: a class teacher can own a section without teaching a
 * subject in it, and a subject teacher can teach a section they are not homeroom for.
 *
 * <p>Written for attendance, but deliberately free of attendance concepts so exams
 * and marks can reuse it.
 */
@Component
@RequiredArgsConstructor
public class SectionAccessGuard {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;

    /**
     * @throws AccessDeniedException if the caller may not act on this class/section
     */
    public void verifyCanAccessSection(Long classId, Long sectionId) {
        if (!canAccessSection(classId, sectionId)) {
            throw new AccessDeniedException("You are not assigned to this class/section");
        }
    }

    /**
     * The narrower check: homeroom teacher of <em>this</em> section, or management.
     *
     * <p>Distinct from {@link #verifyCanAccessSection} in dropping the subject-teacher
     * grant. Both are legitimate rules for different acts — a subject teacher marking
     * their own lesson's marks is normal, a subject teacher marking the class register
     * is not. The register is the class teacher's responsibility and is taken once a
     * day in period 1, so it belongs to whoever holds
     * {@code sections.class_teacher_id} and to nobody else.
     *
     * <p>Management keeps its pass, unlike in {@link HomeroomGuard}: the office does
     * legitimately correct a register, and unlike "my class" there is a real section
     * here for them to act on.
     *
     * @throws AccessDeniedException if the caller is neither management nor this
     *                               section's class teacher
     */
    public void verifyIsHomeroomOrManagement(Long classId, Long sectionId) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        String roleName = principal.getRoleName();
        if (AppConstants.MANAGEMENT_ROLES.contains(roleName)) {
            return;
        }

        if (AppConstants.TEACHING_ROLES.contains(roleName) && classId != null && sectionId != null) {
            boolean homeroom = teacherRepository.findByUserId(principal.getId())
                    .map(teacher -> isHomeroomOf(teacher.getId(), classId, sectionId))
                    // A teaching login with no teacher record is a data problem, not a
                    // licence to mark a register: fail closed.
                    .orElse(false);
            if (homeroom) {
                return;
            }
        }

        throw new AccessDeniedException("Only this class's class teacher can take its attendance");
    }

    public boolean canAccessSection(Long classId, Long sectionId) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        String roleName = principal.getRoleName();

        // Management and office roles supervise every section.
        if (AppConstants.MANAGEMENT_ROLES.contains(roleName)
                || AppConstants.ROLE_RECEPTIONIST.equals(roleName)
                || AppConstants.ROLE_ACCOUNTANT.equals(roleName)) {
            return true;
        }

        if (AppConstants.TEACHING_ROLES.contains(roleName)) {
            if (classId == null || sectionId == null) {
                return false;
            }
            return teacherRepository.findByUserId(principal.getId())
                    .map(teacher -> isHomeroomOf(teacher.getId(), classId, sectionId)
                            || teachesIn(teacher.getId(), classId, sectionId))
                    // A teaching login with no teacher record is a data problem, not a
                    // licence to mark any section's attendance: fail closed.
                    .orElse(false);
        }

        // Everyone else — students, parents, librarians, guards — has no business
        // addressing a section directly. Fail closed rather than defaulting open.
        return false;
    }

    private boolean isHomeroomOf(Long teacherId, Long classId, Long sectionId) {
        return sectionRepository.existsByIdAndSchoolClassIdAndClassTeacherId(sectionId, classId, teacherId);
    }

    private boolean teachesIn(Long teacherId, Long classId, Long sectionId) {
        return classSubjectTeacherRepository
                .existsByTeacherIdAndSchoolClassIdAndSectionId(teacherId, classId, sectionId);
    }
}
