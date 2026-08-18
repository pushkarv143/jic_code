package com.school.sms.service.impl;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.request.TimetableSlotRequest;
import com.school.sms.dto.response.TimetableSlotDto;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TimetableSlot;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.TimetableSlotRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.TimetableService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableSlotRepository timetableSlotRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AuditLogService auditLogService;

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
    @Transactional
    public List<TimetableSlotDto> saveForSection(Long classId, Long sectionId, SaveTimetableRequest request) {
        SchoolClass schoolClass = findClass(classId);
        Section section = verifySectionBelongsToClass(classId, sectionId);

        List<TimetableSlotRequest> slots = request.getSlots() == null ? List.of() : request.getSlots();
        verifyNoDuplicateSlots(slots);

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
