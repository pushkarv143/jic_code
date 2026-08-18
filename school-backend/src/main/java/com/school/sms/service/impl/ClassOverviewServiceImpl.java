package com.school.sms.service.impl;

import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.dto.response.ClassOverviewDto;
import com.school.sms.dto.response.ClassStatsDto;
import com.school.sms.dto.response.ClassTeacherAvailabilityDto;
import com.school.sms.dto.response.ClassSubjectTeacherDto;
import com.school.sms.dto.response.ClassWarningDto;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.dto.response.TeacherWorkloadDto;
import com.school.sms.entity.ClassOfficialRole;
import com.school.sms.entity.ClassSubjectTeacher;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.MarkRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentAttendanceRepository;
import com.school.sms.repository.StudentFeeRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.TimetableSlotRepository;
import com.school.sms.service.ClassOfficialService;
import com.school.sms.service.ClassOverviewService;
import com.school.sms.util.NameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassOverviewServiceImpl implements ClassOverviewService {

    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;
    private final ClassOfficialRepository classOfficialRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final MarkRepository markRepository;
    private final ClassOfficialService classOfficialService;

    /** Posts every class is expected to fill; a gap in one is worth surfacing. */
    private static final List<ClassOfficialRole> EXPECTED_ROLES =
            List.of(ClassOfficialRole.HEAD_BOY, ClassOfficialRole.HEAD_GIRL);

    @Override
    @Transactional(readOnly = true)
    public ClassOverviewDto getOverview(Long classId, LocalDate startDate, LocalDate endDate) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        List<Section> sections = sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(classId);
        List<Subject> subjects = subjectRepository.findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(classId);
        List<ClassSubjectTeacher> mappings =
                classSubjectTeacherRepository.findAllBySchoolClassIdOrderBySectionIdAscSubjectIdAsc(classId);

        Map<Long, Integer> perSection = countStudentsBySection();
        int totalStudents = (int) studentRepository
                .countBySchoolClassIdAndDeletedFalseAndStatus(classId, StudentStatus.ACTIVE);

        // Only sections that actually declare a capacity contribute. Treating a null
        // as 0 would make a part-configured class look catastrophically over-subscribed.
        Integer totalCapacity = null;
        for (Section section : sections) {
            if (section.getCapacity() != null) {
                totalCapacity = (totalCapacity == null ? 0 : totalCapacity) + section.getCapacity();
            }
        }
        Double occupancy = (totalCapacity == null || totalCapacity == 0)
                ? null
                : round2(totalStudents * 100.0 / totalCapacity);

        return ClassOverviewDto.builder()
                .classId(schoolClass.getId())
                .className(schoolClass.getClassName())
                .academicYearId(schoolClass.getAcademicYear() != null ? schoolClass.getAcademicYear().getId() : null)
                .academicYearName(schoolClass.getAcademicYear() != null
                        ? schoolClass.getAcademicYear().getYearName() : null)
                .totalStudents(totalStudents)
                .totalSections(sections.size())
                .totalSubjects(subjects.size())
                .totalCapacity(totalCapacity)
                .occupancyPercentage(occupancy)
                .genderSplit(genderSplit(classId))
                .sections(sections.stream().map(s -> toSectionDto(s, perSection)).toList())
                .officials(classOfficialService.getCurrent(classId))
                .subjectTeachers(mappings.stream().map(this::toMappingDto).toList())
                .stats(buildStats(classId, from, to))
                .warnings(buildWarnings(classId, sections, subjects, mappings, perSection))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countStudentsByClass() {
        return toCountMap(studentRepository.countActiveGroupByClassId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countStudentsBySection() {
        return toCountMap(studentRepository.countActiveGroupBySectionId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassTeacherAvailabilityDto> getClassTeacherAvailability() {
        return sectionRepository.findAllAssignedHomerooms().stream()
                .map(section -> {
                    Teacher teacher = section.getClassTeacher();
                    return ClassTeacherAvailabilityDto.builder()
                            .teacherId(teacher.getId())
                            .teacherName(teacher.getUser() != null
                                    ? NameUtil.fullName(teacher.getUser().getFirstName(),
                                            teacher.getUser().getLastName())
                                    : null)
                            .sectionId(section.getId())
                            .sectionName(section.getSectionName())
                            .classId(section.getSchoolClass() != null ? section.getSchoolClass().getId() : null)
                            .className(section.getSchoolClass() != null
                                    ? section.getSchoolClass().getClassName() : null)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherWorkloadDto> getTeacherWorkload() {
        Map<Long, long[]> byTeacher = new HashMap<>();
        for (Object[] row : classSubjectTeacherRepository.aggregateWorkloadByTeacher()) {
            byTeacher.computeIfAbsent(((Number) row[0]).longValue(), k -> new long[4])[0] =
                    ((Number) row[1]).longValue();
            byTeacher.get(((Number) row[0]).longValue())[1] = ((Number) row[2]).longValue();
        }
        for (Object[] row : timetableSlotRepository.countPeriodsGroupByTeacherId()) {
            byTeacher.computeIfAbsent(((Number) row[0]).longValue(), k -> new long[4])[2] =
                    ((Number) row[1]).longValue();
        }

        List<TeacherWorkloadDto> result = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : byTeacher.entrySet()) {
            Teacher teacher = teacherRepository.findById(entry.getKey()).orElse(null);
            if (teacher == null) {
                continue;
            }
            long[] counts = entry.getValue();
            result.add(TeacherWorkloadDto.builder()
                    .teacherId(teacher.getId())
                    .teacherName(teacher.getUser() != null
                            ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                            : null)
                    .employeeId(teacher.getEmployeeId())
                    .subjectMappings(counts[0])
                    .sectionsTaught(counts[1])
                    .classTeacherOf(sectionRepository.findIdsByClassTeacherId(teacher.getId()).size())
                    .weeklyPeriods(counts[2])
                    .build());
        }
        // Heaviest first: the point of the view is to find who is over-allocated.
        result.sort(Comparator.comparingLong(TeacherWorkloadDto::getWeeklyPeriods)
                .thenComparingLong(TeacherWorkloadDto::getSubjectMappings).reversed());
        return result;
    }

    private ClassStatsDto buildStats(Long classId, LocalDate from, LocalDate to) {
        ClassStatsDto.ClassStatsDtoBuilder stats = ClassStatsDto.builder();

        List<Object[]> attendance = studentAttendanceRepository.aggregateForClassBetween(classId, from, to);
        if (!attendance.isEmpty() && attendance.get(0)[1] != null) {
            long marked = ((Number) attendance.get(0)[1]).longValue();
            stats.attendanceMarkedDays(marked);
            // Left null rather than 0 when nothing is marked: 0% reads as "everyone
            // was absent", which is a very different statement from "not taken yet".
            if (marked > 0) {
                double present = ((Number) attendance.get(0)[0]).doubleValue();
                stats.attendancePercentage(round2(present * 100.0 / marked));
            }
        }

        List<Object[]> fees = studentFeeRepository.aggregateDefaultersForClass(classId);
        if (!fees.isEmpty()) {
            stats.feeDefaulterCount(((Number) fees.get(0)[0]).longValue());
            Object outstanding = fees.get(0)[1];
            stats.feeOutstandingAmount(outstanding instanceof BigDecimal bd
                    ? round2(bd.doubleValue())
                    : outstanding != null ? round2(((Number) outstanding).doubleValue()) : 0.0);
        }

        List<Object[]> marks = markRepository.aggregateAverageForClass(classId);
        if (!marks.isEmpty() && marks.get(0)[0] != null) {
            stats.averageMarksPercentage(round2(((Number) marks.get(0)[0]).doubleValue()));
            stats.gradedStudentCount(((Number) marks.get(0)[1]).longValue());
        }

        return stats.build();
    }

    /**
     * The setup gaps an administrator can act on today. All of it is derived from
     * data already loaded for the overview, so the checks cost no extra queries.
     */
    private List<ClassWarningDto> buildWarnings(Long classId,
                                                List<Section> sections,
                                                List<Subject> subjects,
                                                List<ClassSubjectTeacher> mappings,
                                                Map<Long, Integer> perSection) {
        List<ClassWarningDto> warnings = new ArrayList<>();

        if (sections.isEmpty()) {
            warnings.add(ClassWarningDto.builder()
                    .code("CLASS_WITHOUT_SECTIONS").severity("WARNING")
                    .message("This class has no sections, so no student can be enrolled in it")
                    .build());
            return warnings;
        }

        // A mapping is keyed on (section, subject), so a subject taught in two of
        // three sections is a real gap in the third rather than "covered".
        Set<String> mapped = new HashSet<>();
        for (ClassSubjectTeacher mapping : mappings) {
            if (mapping.getSection() != null && mapping.getSubject() != null) {
                mapped.add(mapping.getSection().getId() + "#" + mapping.getSubject().getId());
            }
        }

        for (Section section : sections) {
            if (section.getClassTeacher() == null) {
                warnings.add(ClassWarningDto.builder()
                        .code("SECTION_WITHOUT_CLASS_TEACHER").severity("WARNING")
                        .message("Section " + section.getSectionName() + " has no class teacher")
                        .sectionId(section.getId()).sectionName(section.getSectionName())
                        .build());
            }

            int strength = perSection.getOrDefault(section.getId(), 0);
            if (section.getCapacity() != null && section.getCapacity() > 0 && strength > section.getCapacity()) {
                warnings.add(ClassWarningDto.builder()
                        .code("SECTION_OVER_CAPACITY").severity("WARNING")
                        .message("Section " + section.getSectionName() + " holds " + strength
                                + " students against a capacity of " + section.getCapacity())
                        .sectionId(section.getId()).sectionName(section.getSectionName())
                        .build());
            }

            for (Subject subject : subjects) {
                if (!mapped.contains(section.getId() + "#" + subject.getId())) {
                    warnings.add(ClassWarningDto.builder()
                            .code("SUBJECT_WITHOUT_TEACHER").severity("WARNING")
                            .message(subject.getSubjectName() + " has no teacher in section "
                                    + section.getSectionName())
                            .sectionId(section.getId()).sectionName(section.getSectionName())
                            .subjectId(subject.getId()).subjectName(subject.getSubjectName())
                            .build());
                }
            }
        }

        // Two sections sharing a home room is not always wrong — they may never meet
        // there at the same time — so this is INFO, and the timetable's own room
        // clash check is what proves an actual conflict.
        Map<String, List<String>> byRoom = new LinkedHashMap<>();
        for (Section section : sections) {
            if (StringUtils.hasText(section.getRoomNumber())) {
                byRoom.computeIfAbsent(section.getRoomNumber(), k -> new ArrayList<>())
                        .add(section.getSectionName());
            }
        }
        byRoom.forEach((room, names) -> {
            if (names.size() > 1) {
                warnings.add(ClassWarningDto.builder()
                        .code("ROOM_SHARED_BY_SECTIONS").severity("INFO")
                        .message("Room " + room + " is the home room for sections " + String.join(", ", names))
                        .build());
            }
        });

        Set<ClassOfficialRole> held = new HashSet<>();
        classOfficialRepository.findAllBySchoolClassIdAndToDateIsNullOrderByRoleAsc(classId)
                .forEach(official -> held.add(official.getRole()));
        for (ClassOfficialRole role : EXPECTED_ROLES) {
            if (!held.contains(role)) {
                warnings.add(ClassWarningDto.builder()
                        .code("POST_VACANT").severity("INFO")
                        .message("No current " + role.name().toLowerCase().replace('_', ' '))
                        .build());
            }
        }

        return warnings;
    }

    private Map<String, Long> genderSplit(Long classId) {
        Map<String, Long> split = new LinkedHashMap<>();
        for (Object[] row : studentRepository.countActiveByGenderForClass(classId)) {
            split.put(row[0] != null ? row[0].toString() : "UNSPECIFIED", ((Number) row[1]).longValue());
        }
        return split;
    }

    private SectionDto toSectionDto(Section section, Map<Long, Integer> perSection) {
        Teacher teacher = section.getClassTeacher();
        return SectionDto.builder()
                .id(section.getId())
                .sectionName(section.getSectionName())
                .classId(section.getSchoolClass() != null ? section.getSchoolClass().getId() : null)
                .className(section.getSchoolClass() != null ? section.getSchoolClass().getClassName() : null)
                .classTeacherId(teacher != null ? teacher.getId() : null)
                .classTeacherName(teacher != null && teacher.getUser() != null
                        ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                        : null)
                .roomNumber(section.getRoomNumber())
                .capacity(section.getCapacity())
                .studentCount(perSection.getOrDefault(section.getId(), 0))
                .build();
    }

    private ClassSubjectTeacherDto toMappingDto(ClassSubjectTeacher mapping) {
        Teacher teacher = mapping.getTeacher();
        return ClassSubjectTeacherDto.builder()
                .id(mapping.getId())
                .classId(mapping.getSchoolClass() != null ? mapping.getSchoolClass().getId() : null)
                .className(mapping.getSchoolClass() != null ? mapping.getSchoolClass().getClassName() : null)
                .sectionId(mapping.getSection() != null ? mapping.getSection().getId() : null)
                .sectionName(mapping.getSection() != null ? mapping.getSection().getSectionName() : null)
                .subjectId(mapping.getSubject() != null ? mapping.getSubject().getId() : null)
                .subjectName(mapping.getSubject() != null ? mapping.getSubject().getSubjectName() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null && teacher.getUser() != null
                        ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                        : null)
                .build();
    }

    private Map<Long, Integer> toCountMap(List<Object[]> rows) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                counts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return counts;
    }

    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
