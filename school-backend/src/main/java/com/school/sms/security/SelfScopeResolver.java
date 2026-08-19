package com.school.sms.security;

import com.school.sms.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Works out which record a "my ..." request should be answered from.
 *
 * <p>Several endpoints take no id and serve whoever is signed in — a teacher asks
 * for their teaching week and a student for their class's week down the same
 * path. That branching, and the wording of the failures around it, was the same
 * in every one of them, so it lives here once: duplicating it invites the
 * endpoints to disagree about what a parent of three children, or a login with no
 * linked record, is told.
 *
 * <p>Deliberately resolves the teacher record first. The two are not meant to
 * coexist on one login, but if they ever do, the teaching relationship is the
 * more specific one and the question is being asked as staff.
 */
@Component
@RequiredArgsConstructor
public class SelfScopeResolver {

    private final TeacherAccessGuard teacherAccessGuard;
    private final StudentAccessGuard studentAccessGuard;

    /** Either a teacher id or a student id, never both, never neither. */
    public record SelfScope(Long teacherId, Long studentId) {

        public boolean isTeacher() {
            return teacherId != null;
        }
    }

    /**
     * @throws BadRequestException when the caller has no single record to answer
     *         from: an office or management login with no timetable of its own, a
     *         login linked to nothing, or a parent of more than one child, for whom
     *         picking a child would be a guess rather than an answer.
     */
    public SelfScope resolve() {
        Optional<Long> ownTeacherId = teacherAccessGuard.findOwnTeacherId();
        if (ownTeacherId.isPresent()) {
            return new SelfScope(ownTeacherId.get(), null);
        }

        // null (rather than empty) means the caller is not self-scoped at all.
        List<Long> ownStudentIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownStudentIds == null) {
            throw new BadRequestException(
                    "Your role has nothing of its own to show here. Ask for a class or a teacher instead.");
        }
        if (ownStudentIds.isEmpty()) {
            throw new BadRequestException("Your login is not linked to a student record");
        }
        if (ownStudentIds.size() > 1) {
            throw new BadRequestException("You are linked to " + ownStudentIds.size()
                    + " students. Ask for one of them by id instead.");
        }
        return new SelfScope(null, ownStudentIds.get(0));
    }
}
