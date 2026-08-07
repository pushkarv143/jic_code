package com.school.sms.service.impl;

import com.school.sms.dto.request.ClassSubjectTeacherRequest;
import com.school.sms.dto.response.ClassSubjectTeacherDto;
import com.school.sms.entity.ClassSubjectTeacher;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.ClassSubjectTeacherMapper;
import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.ClassSubjectTeacherService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSubjectTeacherServiceImpl implements ClassSubjectTeacherService {

    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSubjectTeacherMapper classSubjectTeacherMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectTeacherDto> getAll(Long sectionId, Long subjectId, Long teacherId) {
        Specification<ClassSubjectTeacher> spec = new SpecificationBuilder<ClassSubjectTeacher>()
                .with(sectionId != null, "section.id", SearchOperation.EQUALS, sectionId)
                .with(subjectId != null, "subject.id", SearchOperation.EQUALS, subjectId)
                .with(teacherId != null, "teacher.id", SearchOperation.EQUALS, teacherId)
                .build();

        List<ClassSubjectTeacher> results = spec == null
                ? classSubjectTeacherRepository.findAll()
                : classSubjectTeacherRepository.findAll(spec);

        return results.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ClassSubjectTeacherDto create(ClassSubjectTeacherRequest request) {
        if (classSubjectTeacherRepository.existsBySectionIdAndSubjectId(request.getSectionId(), request.getSubjectId())) {
            throw new DuplicateResourceException("ClassSubjectTeacher", "sectionId+subjectId",
                    request.getSectionId() + "+" + request.getSubjectId());
        }

        SchoolClass schoolClass = schoolClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.getClassId()));
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", request.getSectionId()));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));

        ClassSubjectTeacher entity = ClassSubjectTeacher.builder()
                .schoolClass(schoolClass)
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .build();

        return toDto(classSubjectTeacherRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ClassSubjectTeacher entity = classSubjectTeacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSubjectTeacher", "id", id));
        classSubjectTeacherRepository.delete(entity);
    }

    private ClassSubjectTeacherDto toDto(ClassSubjectTeacher entity) {
        ClassSubjectTeacherDto dto = classSubjectTeacherMapper.toDto(entity);
        dto.setTeacherName(NameUtil.fullName(
                entity.getTeacher().getUser().getFirstName(),
                entity.getTeacher().getUser().getLastName()));
        return dto;
    }
}
