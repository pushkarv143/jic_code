package com.school.sms.service.impl;

import com.school.sms.dto.request.OnlineClassRequest;
import com.school.sms.dto.response.OnlineClassDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.OnlineClass;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.OnlineClassRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.OnlineClassService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnlineClassServiceImpl implements OnlineClassService {

    private static final String ROLE_PREFIX = AppConstants.JWT_ROLE_PREFIX;

    private final OnlineClassRepository onlineClassRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OnlineClassDto> getAll(Long classId, Long sectionId, Long subjectId, Long teacherId,
                                                Boolean upcoming, Pageable pageable) {
        Long effectiveClassId = classId;
        Long effectiveSectionId = sectionId;

        if (classId == null && sectionId == null) {
            Optional<Student> ownStudent = currentStudentIfRole(AppConstants.ROLE_STUDENT);
            if (ownStudent.isPresent()) {
                effectiveClassId = ownStudent.get().getSchoolClass().getId();
                effectiveSectionId = ownStudent.get().getSection().getId();
            }
        }

        Specification<OnlineClass> spec = new SpecificationBuilder<OnlineClass>()
                .with(effectiveClassId != null, "schoolClass.id", SearchOperation.EQUALS, effectiveClassId)
                .with(effectiveSectionId != null, "section.id", SearchOperation.EQUALS, effectiveSectionId)
                .with(subjectId != null, "subject.id", SearchOperation.EQUALS, subjectId)
                .with(teacherId != null, "teacher.id", SearchOperation.EQUALS, teacherId)
                .with(Boolean.TRUE.equals(upcoming), "scheduledAt", SearchOperation.GREATER_THAN_EQUAL, LocalDateTime.now())
                .build();

        Page<OnlineClass> page = onlineClassRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public OnlineClassDto create(OnlineClassRequest request) {
        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        Subject subject = findSubject(request.getSubjectId());
        Teacher teacher = resolveTeacherForWrite(request.getTeacherId());

        OnlineClass onlineClass = OnlineClass.builder()
                .schoolClass(schoolClass)
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .title(request.getTitle())
                .meetingLink(request.getMeetingLink())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .build();

        return toDto(onlineClassRepository.save(onlineClass));
    }

    @Override
    @Transactional
    public OnlineClassDto update(Long id, OnlineClassRequest request) {
        OnlineClass onlineClass = findEntity(id);
        onlineClass.setSchoolClass(findClass(request.getClassId()));
        onlineClass.setSection(findSection(request.getSectionId()));
        onlineClass.setSubject(findSubject(request.getSubjectId()));
        onlineClass.setTeacher(resolveTeacherForWrite(request.getTeacherId()));
        onlineClass.setTitle(request.getTitle());
        onlineClass.setMeetingLink(request.getMeetingLink());
        onlineClass.setScheduledAt(request.getScheduledAt());
        onlineClass.setDurationMinutes(request.getDurationMinutes());

        return toDto(onlineClassRepository.save(onlineClass));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        onlineClassRepository.delete(findEntity(id));
    }

    /** See AssignmentServiceImpl.resolveTeacherForWrite — identical rule applied here. */
    private Teacher resolveTeacherForWrite(Long explicitTeacherId) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
        boolean isTeacherRole = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ROLE_PREFIX + AppConstants.ROLE_TEACHER) || a.equals(ROLE_PREFIX + AppConstants.ROLE_CLASS_TEACHER));

        if (isTeacherRole) {
            return teacherRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new BadRequestException("No teacher record found for the current user"));
        }
        if (explicitTeacherId != null) {
            return teacherRepository.findById(explicitTeacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", explicitTeacherId));
        }
        return null;
    }

    private Optional<Student> currentStudentIfRole(String roleName) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        if (principal == null) {
            return Optional.empty();
        }
        boolean matches = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ROLE_PREFIX + roleName));
        return matches ? studentRepository.findByUserId(principal.getId()) : Optional.empty();
    }

    private OnlineClassDto toDto(OnlineClass onlineClass) {
        Teacher teacher = onlineClass.getTeacher();
        return OnlineClassDto.builder()
                .id(onlineClass.getId())
                .classId(onlineClass.getSchoolClass().getId())
                .className(onlineClass.getSchoolClass().getClassName())
                .sectionId(onlineClass.getSection().getId())
                .sectionName(onlineClass.getSection().getSectionName())
                .subjectId(onlineClass.getSubject().getId())
                .subjectName(onlineClass.getSubject().getSubjectName())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null && teacher.getUser() != null
                        ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                        : null)
                .title(onlineClass.getTitle())
                .meetingLink(onlineClass.getMeetingLink())
                .scheduledAt(onlineClass.getScheduledAt())
                .durationMinutes(onlineClass.getDurationMinutes())
                .build();
    }

    private OnlineClass findEntity(Long id) {
        return onlineClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnlineClass", "id", id));
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", id));
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
    }
}
