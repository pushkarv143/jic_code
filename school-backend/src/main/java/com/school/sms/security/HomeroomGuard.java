package com.school.sms.security;

import com.school.sms.entity.Section;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Answers "which section is this caller the homeroom teacher of?" — and nothing
 * wider.
 *
 * <p>This is the narrowest of the three section-scoped guards, and the difference
 * is the whole point:
 *
 * <ul>
 *   <li>{@link SectionAccessGuard} — homeroom <em>or</em> subject-taught, plus a
 *       blanket pass for management and office roles. Right for attendance and
 *       marks, where a subject teacher legitimately acts on a section.</li>
 *   <li>{@link StudentAccessGuard} — the union of every student a teacher reaches
 *       by either grant. Right for the student directory.</li>
 *   <li><b>this guard</b> — homeroom only, no role exemptions at all.</li>
 * </ul>
 *
 * <p><b>Why no exemption for management or SUPER_ADMIN.</b> Elsewhere an
 * administrator is deliberately never gated by a missing grant
 * ({@code AppConstants.ADMIN_OVERRIDE}), because a missing {@code role_permissions}
 * row is a data problem rather than a policy decision. Here there is nothing to
 * exempt them into: "my class" is a fact about {@code sections.class_teacher_id},
 * and a principal who is homeroom of no section has no such class. Returning some
 * arbitrary section for them would be a lie, and returning every section would
 * duplicate the Classes module they already have. So the guard reports honestly
 * that they have no homeroom, and the module simply does not appear for them.
 *
 * <p><b>Why the role is not consulted.</b> Holding the {@code CLASS_TEACHER} role
 * is neither necessary nor sufficient. On the seeded database 44 users hold that
 * role while only 17 are homeroom of anything — so the role would advertise the
 * module to 27 people with an empty class. Conversely a plain {@code TEACHER} who
 * is put in {@code sections.class_teacher_id} genuinely is a homeroom teacher and
 * should reach it. The assignment decides; the role only shapes coarse menus.
 *
 * <p>A teacher heads at most one section, enforced by {@code uq_sections_class_teacher}
 * in {@code 13_assignment_uniqueness.sql} — hence {@link Optional} rather than a list.
 */
@Component
@RequiredArgsConstructor
public class HomeroomGuard {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

    /**
     * The caller's homeroom section, or empty when they have none.
     *
     * <p>Empty is an ordinary answer, not an error: most logins have no homeroom.
     * Callers that need one should use {@link #requireHomeroomSection()}.
     */
    public Optional<Section> findHomeroomSection() {
        return SecurityUtils.getCurrentUserPrincipal()
                .flatMap(principal -> teacherRepository.findByUserId(principal.getId()))
                .flatMap(teacher -> sectionRepository.findByClassTeacherId(teacher.getId()));
    }

    /**
     * @return the caller's homeroom section
     * @throws AccessDeniedException if they are not homeroom teacher of any section
     */
    public Section requireHomeroomSection() {
        return findHomeroomSection().orElseThrow(() -> new AccessDeniedException(
                "You are not the class teacher of any section"));
    }

    /**
     * @throws AccessDeniedException if the caller is not homeroom teacher of this section
     */
    public void verifyIsHomeroomOf(Long sectionId) {
        if (sectionId == null || !requireHomeroomSection().getId().equals(sectionId)) {
            throw new AccessDeniedException("You are not the class teacher of this section");
        }
    }

    /** True when the caller is homeroom teacher of some section. */
    public boolean hasHomeroom() {
        return findHomeroomSection().isPresent();
    }
}
