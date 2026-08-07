package com.school.sms.service.impl;

import com.school.sms.dto.request.SubjectRequest;
import com.school.sms.dto.response.SubjectDto;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Subject;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.SubjectMapper;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectMapper subjectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectDto> getByClassId(Long classId) {
        findClass(classId);
        return subjectRepository.findAllBySchoolClassIdAndDeletedFalseOrderBySubjectNameAsc(classId).stream()
                .map(subjectMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SubjectDto create(Long classId, SubjectRequest request) {
        SchoolClass schoolClass = findClass(classId);
        if (subjectRepository.existsBySubjectCodeIgnoreCase(request.getSubjectCode())) {
            throw new DuplicateResourceException("Subject", "subjectCode", request.getSubjectCode());
        }

        Subject entity = subjectMapper.toEntity(request);
        entity.setSchoolClass(schoolClass);
        entity.setDeleted(false);

        return subjectMapper.toDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectDto update(Long id, SubjectRequest request) {
        Subject entity = findEntity(id);

        if (!entity.getSubjectCode().equalsIgnoreCase(request.getSubjectCode())
                && subjectRepository.existsBySubjectCodeIgnoreCase(request.getSubjectCode())) {
            throw new DuplicateResourceException("Subject", "subjectCode", request.getSubjectCode());
        }

        subjectMapper.updateEntityFromRequest(request, entity);
        return subjectMapper.toDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject entity = findEntity(id);
        entity.setDeleted(true);
        subjectRepository.save(entity);
    }

    private Subject findEntity(Long id) {
        return subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
    }

    private SchoolClass findClass(Long classId) {
        return schoolClassRepository.findById(classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
    }
}
