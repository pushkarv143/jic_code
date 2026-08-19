package com.school.sms.service.impl;

import com.school.sms.dto.request.SchoolClassRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.dto.response.SchoolClassDto;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.ClassOfficial;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.StudentStatus;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.SchoolClassMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.service.SchoolClassService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final ClassOfficialRepository classOfficialRepository;
    private final SchoolClassMapper schoolClassMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SchoolClassDto> getAll(Long academicYearId, int page, int size,
                                                String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<SchoolClass> spec = new SpecificationBuilder<SchoolClass>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(academicYearId != null, "academicYear.id", SearchOperation.EQUALS, academicYearId)
                .build();

        Page<SchoolClass> pageResult = schoolClassRepository.findAll(spec, pageable);
        List<Long> classIds = pageResult.getContent().stream().map(SchoolClass::getId).toList();

        /*
         * Everything the list renders is gathered here in a fixed number of grouped
         * or IN queries, rather than per row.
         *
         * Two of these columns were previously always blank: the grid has always had
         * "Sections" and "Students" columns, but getAll set neither, so every class
         * read 0 and "-". Only getById filled them in. Sections and officials are
         * loaded as well because the list offers a class-teacher dropdown per section
         * and a dropdown per class post, and a request per row would make a ten-row
         * page cost forty round trips.
         */
        Map<Long, Integer> studentCounts = toCountMap(studentRepository.countActiveGroupByClassId());
        Map<Long, Integer> subjectCounts = toCountMap(subjectRepository.countActiveGroupByClassId());
        Map<Long, Integer> sectionStudentCounts = toCountMap(studentRepository.countActiveGroupBySectionId());

        Map<Long, List<Section>> sectionsByClass = new HashMap<>();
        Map<Long, List<ClassOfficial>> officialsByClass = new HashMap<>();
        if (!classIds.isEmpty()) {
            sectionRepository.findAllWithClassTeacherBySchoolClassIdIn(classIds)
                    .forEach(section -> sectionsByClass
                            .computeIfAbsent(section.getSchoolClass().getId(), k -> new ArrayList<>())
                            .add(section));
            classOfficialRepository.findCurrentWithStudentBySchoolClassIdIn(classIds)
                    .forEach(official -> officialsByClass
                            .computeIfAbsent(official.getSchoolClass().getId(), k -> new ArrayList<>())
                            .add(official));
        }

        Page<SchoolClassDto> dtoPage = pageResult.map(entity -> {
            SchoolClassDto dto = schoolClassMapper.toDto(entity);
            List<Section> sections = sectionsByClass.getOrDefault(entity.getId(), List.of());

            dto.setStudentCount(studentCounts.getOrDefault(entity.getId(), 0));
            dto.setSubjectCount(subjectCounts.getOrDefault(entity.getId(), 0));
            dto.setSectionCount(sections.size());
            dto.setSections(sections.stream()
                    .map(section -> toSectionDto(section, sectionStudentCounts))
                    .toList());
            dto.setOfficials(officialsByClass.getOrDefault(entity.getId(), List.of()).stream()
                    .map(this::toOfficialDto)
                    .toList());
            return dto;
        });
        return PageResponse.from(dtoPage);
    }

    private SectionDto toSectionDto(Section section, Map<Long, Integer> sectionStudentCounts) {
        Teacher teacher = section.getClassTeacher();
        return SectionDto.builder()
                .id(section.getId())
                .sectionName(section.getSectionName())
                .classId(section.getSchoolClass() != null ? section.getSchoolClass().getId() : null)
                .classTeacherId(teacher != null ? teacher.getId() : null)
                .classTeacherName(teacher != null && teacher.getUser() != null
                        ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                        : null)
                .roomNumber(section.getRoomNumber())
                .capacity(section.getCapacity())
                .studentCount(sectionStudentCounts.getOrDefault(section.getId(), 0))
                .build();
    }

    private ClassOfficialDto toOfficialDto(ClassOfficial official) {
        Student student = official.getStudent();
        // Identity from the student rather than the optional login account, matching
        // StudentMapper and the attendance responses.
        String name = student == null ? null
                : StringUtils.hasText(student.getFirstName())
                        ? NameUtil.fullName(student.getFirstName(), student.getLastName())
                        : student.getUser() != null
                                ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                                : null;

        return ClassOfficialDto.builder()
                .id(official.getId())
                .classId(official.getSchoolClass() != null ? official.getSchoolClass().getId() : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(name)
                .rollNumber(student != null ? student.getRollNumber() : null)
                .role(official.getRole() != null ? official.getRole().name() : null)
                .fromDate(official.getFromDate())
                .toDate(official.getToDate())
                .current(official.isCurrent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDto getById(Long id) {
        SchoolClass entity = findEntity(id);
        SchoolClassDto dto = schoolClassMapper.toDto(entity);
        dto.setSectionCount(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(id).size());
        dto.setSubjectCount(subjectRepository.findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(id).size());
        dto.setStudentCount((int) studentRepository
                .countBySchoolClassIdAndDeletedFalseAndStatus(id, StudentStatus.ACTIVE));
        return dto;
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

    @Override
    @Transactional
    public SchoolClassDto create(SchoolClassRequest request) {
        if (schoolClassRepository.existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(
                request.getClassName(), request.getAcademicYearId())) {
            throw new DuplicateResourceException("Class", "className", request.getClassName());
        }
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        SchoolClass entity = schoolClassMapper.toEntity(request);
        entity.setAcademicYear(academicYear);
        entity.setDeleted(false);

        SchoolClass saved = schoolClassRepository.save(entity);

        // The class's one section is created with it. Every record that hangs off a
        // class — a student, a subject-teacher mapping, a period, an attendance mark —
        // needs a section to point at, and since the school runs a single section
        // there is nothing for an administrator to decide here. Leaving it to a
        // second call is what used to produce a class nobody could enrol into.
        sectionRepository.save(Section.builder()
                .sectionName(AppConstants.SINGLE_SECTION_NAME)
                .schoolClass(saved)
                .build());

        return schoolClassMapper.toDto(saved);
    }

    @Override
    @Transactional
    public SchoolClassDto update(Long id, SchoolClassRequest request) {
        SchoolClass entity = findEntity(id);

        boolean nameOrYearChanged = !entity.getClassName().equalsIgnoreCase(request.getClassName())
                || !entity.getAcademicYear().getId().equals(request.getAcademicYearId());
        if (nameOrYearChanged && schoolClassRepository.existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(
                request.getClassName(), request.getAcademicYearId())) {
            throw new DuplicateResourceException("Class", "className", request.getClassName());
        }

        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());
        schoolClassMapper.updateEntityFromRequest(request, entity);
        entity.setAcademicYear(academicYear);

        return schoolClassMapper.toDto(schoolClassRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SchoolClass entity = findEntity(id);
        entity.setDeleted(true);
        schoolClassRepository.save(entity);
    }

    private SchoolClass findEntity(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }
}
