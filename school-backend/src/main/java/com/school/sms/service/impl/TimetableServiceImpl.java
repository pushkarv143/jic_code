package com.school.sms.service.impl;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.request.TimetableSlotRequest;
import com.school.sms.dto.response.TimetableSlotDto;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TimetableSlot;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.TimetableSlotRepository;
import com.school.sms.security.SelfScopeResolver;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.TimetableService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableSlotRepository timetableSlotRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;
    private final StudentAccessGuard studentAccessGuard;
    private final SelfScopeResolver selfScopeResolver;
    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;

    /** Period 1 — the register period, which belongs to the class teacher. */
    private static final Integer FIRST_PERIOD = 1;

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getForSection(Long classId, Long sectionId) {
        verifySectionBelongsToClass(classId, sectionId);
        return withClashWarnings(
                timetableSlotRepository.findAllBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(sectionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getForClass(Long classId) {
        findClass(classId);
        return withClashWarnings(
                timetableSlotRepository.findAllBySchoolClassIdOrderByDayOfWeekAscPeriodNumberAsc(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getForTeacher(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }
        return withClashWarnings(
                timetableSlotRepository.findAllByTeacherIdOrderByDayOfWeekAscPeriodNumberAsc(teacherId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getForCurrentUser() {
        SelfScopeResolver.SelfScope scope = selfScopeResolver.resolve();
        return scope.isTeacher()
                ? getForTeacher(scope.teacherId())
                : getForStudent(scope.studentId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableSlotDto> getForStudent(Long studentId) {
        // The full student-module check, not just the self-scope one: it holds a
        // student or parent to their own records *and* a teacher to students they
        // actually teach, so this path cannot be used to browse an arbitrary
        // student's class week by id. Management passes through.
        studentAccessGuard.verifyCanViewStudentRecord(studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Section section = student.getSection();
        if (section == null) {
            throw new BadRequestException("That student is not enrolled in a class yet");
        }
        return withClashWarnings(
                timetableSlotRepository.findAllBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(section.getId()));
    }

    @Override
    @Transactional
    public List<TimetableSlotDto> saveForSection(Long classId, Long sectionId, SaveTimetableRequest request) {
        SchoolClass schoolClass = findClass(classId);
        Section section = verifySectionBelongsToClass(classId, sectionId);

        List<TimetableSlotRequest> slots = request.getSlots() == null ? List.of() : request.getSlots();
        verifyNoDuplicateSlots(slots);
        verifyEveryTaughtPeriodHasATeacher(slots);
        verifyFirstPeriodBelongsToTheClassTeacher(section, slots);
        verifyNoTeacherIsInTwoPlacesAtOnce(sectionId, slots);

        // Delete-then-insert rather than a diff. A timetable is edited as a grid and
        // a single edit usually moves several periods at once; reconciling that
        // in-place would have to order the writes so no intermediate state violates
        // the (section, day, period) unique key. Replacing the week sidesteps the
        // ordering problem entirely, and the flush is what makes it safe — without
        // it Hibernate is free to run the inserts before the delete and trip the
        // very constraint this avoids.
        timetableSlotRepository.deleteAllBySectionId(sectionId);
        timetableSlotRepository.flush();

        List<TimetableSlot> saved = new ArrayList<>();
        for (TimetableSlotRequest item : slots) {
            if (!item.getEndTime().isAfter(item.getStartTime())) {
                throw new BadRequestException("Period " + item.getPeriodNumber() + " on "
                        + item.getDayOfWeek() + " ends before it starts");
            }

            Subject subject = null;
            if (item.getSubjectId() != null) {
                subject = subjectRepository.findById(item.getSubjectId())
                        .filter(s -> !s.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", item.getSubjectId()));
                // A subject belongs to exactly one class, so timetabling another
                // class's subject here is a mis-pick rather than a shared resource.
                if (subject.getSchoolClass() == null || !subject.getSchoolClass().getId().equals(classId)) {
                    throw new BadRequestException("Subject " + subject.getSubjectName()
                            + " does not belong to this class");
                }
            }

            Teacher teacher = null;
            if (item.getTeacherId() != null) {
                teacher = teacherRepository.findById(item.getTeacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", item.getTeacherId()));
            }

            saved.add(timetableSlotRepository.save(TimetableSlot.builder()
                    .schoolClass(schoolClass)
                    .section(section)
                    .dayOfWeek(item.getDayOfWeek())
                    .periodNumber(item.getPeriodNumber())
                    .startTime(item.getStartTime())
                    .endTime(item.getEndTime())
                    .subject(subject)
                    .teacher(teacher)
                    .roomNumber(StringUtils.hasText(item.getRoomNumber())
                            ? item.getRoomNumber()
                            : section.getRoomNumber())
                    .label(item.getLabel())
                    .build()));
        }

        auditLogService.record("SAVE_TIMETABLE", "Section", sectionId, null, null);
        return withClashWarnings(saved);
    }

    /**
     * The unique key catches a repeated (day, period) only once the rows reach the
     * database, and by then the message is a constraint name. Checking the batch
     * first turns it into something an administrator can act on.
     */
    private void verifyNoDuplicateSlots(List<TimetableSlotRequest> slots) {
        Set<String> seen = new HashSet<>();
        for (TimetableSlotRequest item : slots) {
            String key = item.getDayOfWeek() + "#" + item.getPeriodNumber();
            if (!seen.add(key)) {
                throw new BadRequestException("Period " + item.getPeriodNumber() + " on "
                        + item.getDayOfWeek() + " is listed twice");
            }
        }
    }

    /**
     * A subject cannot be timetabled before somebody is assigned to teach it.
     *
     * <p>Without this a period could be created for English with nobody in front of
     * the class, and the omission would only surface when the register was taken.
     * The Teachers &amp; Timetable screen assigns the teacher first and carries them
     * onto every period it creates, so reaching this message means the mapping is
     * genuinely missing.
     *
     * <p>Slots with no subject are exempt: assembly, games and free periods legitimately
     * occupy a period with neither a subject nor a teacher.
     */
    private void verifyEveryTaughtPeriodHasATeacher(List<TimetableSlotRequest> slots) {
        for (TimetableSlotRequest item : slots) {
            if (item.getSubjectId() != null && item.getTeacherId() == null) {
                Subject subject = subjectRepository.findById(item.getSubjectId()).orElse(null);
                String name = subject != null ? subject.getSubjectName() : "this subject";
                throw new BadRequestException("Assign a teacher to " + name
                        + " before adding it to the timetable");
            }
        }
    }

    /**
     * Period 1 belongs to the class teacher.
     *
     * <p>The register is taken in the first period and only the class teacher takes
     * it, so the person standing there at P1 has to be them. Two things follow, and
     * both are checked:
     *
     * <ul>
     *   <li>a P1 slot's teacher must be the section's class teacher;</li>
     *   <li>the subject in it must be one that teacher is actually mapped to teach —
     *       otherwise P1 could be filled with a subject nobody assigned them.</li>
     * </ul>
     *
     * <p>A P1 slot carrying neither subject nor teacher is left alone. Morning
     * assembly is exactly that, and it occupies the first period in most schools;
     * refusing it would enforce the rule past the point it means anything, since
     * there is no teaching to own.
     */
    private void verifyFirstPeriodBelongsToTheClassTeacher(Section section, List<TimetableSlotRequest> slots) {
        List<TimetableSlotRequest> firstPeriods = slots.stream()
                .filter(item -> FIRST_PERIOD.equals(item.getPeriodNumber()))
                .filter(item -> item.getSubjectId() != null || item.getTeacherId() != null)
                .toList();
        if (firstPeriods.isEmpty()) {
            return;
        }

        Teacher classTeacher = section.getClassTeacher();
        if (classTeacher == null) {
            throw new BadRequestException("This class has no class teacher yet. Assign one before "
                    + "timetabling period 1, which belongs to them.");
        }

        for (TimetableSlotRequest item : firstPeriods) {
            if (!classTeacher.getId().equals(item.getTeacherId())) {
                throw new BadRequestException("Period 1 on " + item.getDayOfWeek()
                        + " must be taught by the class teacher");
            }
            if (item.getSubjectId() != null
                    && !classSubjectTeacherRepository.existsByTeacherIdAndSectionIdAndSubjectId(
                            classTeacher.getId(), section.getId(), item.getSubjectId())) {
                Subject subject = subjectRepository.findById(item.getSubjectId()).orElse(null);
                String name = subject != null ? subject.getSubjectName() : "that subject";
                throw new BadRequestException("The class teacher is not assigned to teach " + name
                        + ", so it cannot go in period 1");
            }
        }
    }

    /**
     * A teacher cannot be in two classes at the same time.
     *
     * <p>This is a rejection, not a warning, and it is the one clash that has to be.
     * A double-booked room can be resolved by moving one of the two lessons and is
     * a scheduling inconvenience; a double-booked teacher is a timetable that cannot
     * be run at all. {@link #withClashWarnings} still reports both after the fact —
     * historical rows saved before this rule existed, and room conflicts, which stay
     * advisory.
     *
     * <p>Only conflicts with <em>other</em> sections need checking. Within the
     * submitted week the same teacher cannot occupy one period twice, because that
     * would be the same (day, period) twice and {@link #verifyNoDuplicateSlots}
     * has already rejected it — one section runs one lesson at a time.
     *
     * <p>The section being saved is excluded from the query for the opposite reason:
     * its rows are about to be deleted and rewritten by this same transaction, so
     * counting them would make every save collide with the week it is replacing.
     */
    private void verifyNoTeacherIsInTwoPlacesAtOnce(Long sectionId, List<TimetableSlotRequest> slots) {
        Map<String, TimetableSlotRequest> withinThisWeek = new HashMap<>();
        for (TimetableSlotRequest item : slots) {
            if (item.getTeacherId() != null) {
                withinThisWeek.put(
                        item.getTeacherId() + "#" + item.getDayOfWeek() + "#" + item.getPeriodNumber(),
                        item);
            }
        }
        if (withinThisWeek.isEmpty()) {
            return;
        }

        Set<Long> teacherIds = slots.stream()
                .map(TimetableSlotRequest::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (TimetableSlot booked : timetableSlotRepository
                .findBookingsForTeachersOutsideSection(teacherIds, sectionId)) {
            if (booked.getTeacher() == null) {
                continue;
            }
            String key = booked.getTeacher().getId() + "#" + booked.getDayOfWeek() + "#"
                    + booked.getPeriodNumber();
            if (withinThisWeek.containsKey(key)) {
                String where = booked.getSchoolClass() != null ? booked.getSchoolClass().getClassName() : "another class";
                throw new BadRequestException(teacherName(booked.getTeacher().getId())
                        + " already teaches " + where + " in period " + booked.getPeriodNumber()
                        + " on " + booked.getDayOfWeek() + ". A teacher cannot take two classes at once.");
            }
        }
    }

    /** Best-effort display name for a rejection message; falls back to the id. */
    private String teacherName(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .map(teacher -> {
                    var user = teacher.getUser();
                    if (user == null) {
                        return "Teacher #" + teacherId;
                    }
                    String last = user.getLastName() == null ? "" : " " + user.getLastName();
                    return (user.getFirstName() + last).trim();
                })
                .orElse("Teacher #" + teacherId);
    }

    /**
     * Annotates each slot that double-books a teacher or a room.
     *
     * <p>Clashes are reported rather than rejected. They span sections, so a
     * timetable being built one section at a time is legitimately in conflict for
     * as long as it takes to finish the others — refusing the save would make the
     * grid impossible to fill in any order. The warning is what makes the conflict
     * visible; resolving it stays the administrator's call.
     */
    private List<TimetableSlotDto> withClashWarnings(List<TimetableSlot> slots) {
        Set<String> teacherClashes = new HashSet<>();
        for (Object[] row : timetableSlotRepository.findTeacherClashes()) {
            teacherClashes.add(row[0] + "#" + row[1] + "#" + row[2]);
        }
        Set<String> roomClashes = new HashSet<>();
        for (Object[] row : timetableSlotRepository.findRoomClashes()) {
            roomClashes.add(row[0] + "#" + row[1] + "#" + row[2]);
        }

        List<TimetableSlotDto> result = new ArrayList<>();
        for (TimetableSlot slot : slots) {
            Teacher teacher = slot.getTeacher();
            Subject subject = slot.getSubject();
            Section section = slot.getSection();
            DayOfWeek day = slot.getDayOfWeek();

            List<String> warnings = new ArrayList<>();
            if (teacher != null
                    && teacherClashes.contains(teacher.getId() + "#" + day + "#" + slot.getPeriodNumber())) {
                warnings.add("This teacher is booked for another section in this period");
            }
            if (StringUtils.hasText(slot.getRoomNumber())
                    && roomClashes.contains(slot.getRoomNumber() + "#" + day + "#" + slot.getPeriodNumber())) {
                warnings.add("Room " + slot.getRoomNumber() + " is booked twice in this period");
            }

            result.add(TimetableSlotDto.builder()
                    .id(slot.getId())
                    .classId(slot.getSchoolClass() != null ? slot.getSchoolClass().getId() : null)
                    .className(slot.getSchoolClass() != null ? slot.getSchoolClass().getClassName() : null)
                    .sectionId(section != null ? section.getId() : null)
                    .sectionName(section != null ? section.getSectionName() : null)
                    .dayOfWeek(day != null ? day.name() : null)
                    .periodNumber(slot.getPeriodNumber())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .subjectId(subject != null ? subject.getId() : null)
                    .subjectName(subject != null ? subject.getSubjectName() : null)
                    .teacherId(teacher != null ? teacher.getId() : null)
                    .teacherName(teacher != null && teacher.getUser() != null
                            ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                            : null)
                    .roomNumber(slot.getRoomNumber())
                    .label(slot.getLabel())
                    .clashWarning(warnings.isEmpty() ? null : String.join("; ", warnings))
                    .build());
        }
        return result;
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private Section verifySectionBelongsToClass(Long classId, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));
        if (section.getSchoolClass() == null || !section.getSchoolClass().getId().equals(classId)) {
            throw new BadRequestException("That section does not belong to this class");
        }
        return section;
    }
}
