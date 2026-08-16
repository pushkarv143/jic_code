package com.school.sms.service.impl;

import com.school.sms.dto.request.AssignmentFormRequest;
import com.school.sms.dto.request.GradeSubmissionRequest;
import com.school.sms.dto.response.AssignmentDto;
import com.school.sms.dto.response.AssignmentSubmissionDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.Assignment;
import com.school.sms.entity.AssignmentSubmission;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.Subject;
import com.school.sms.entity.SubmissionStatus;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.AssignmentRepository;
import com.school.sms.repository.AssignmentSubmissionRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AssignmentService;
import com.school.sms.service.FileStorageService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private static final String ROLE_PREFIX = AppConstants.JWT_ROLE_PREFIX;

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignmentDto> getAll(Long classId, Long sectionId, Long subjectId, Long teacherId, Pageable pageable) {
        Long effectiveClassId = classId;
        Long effectiveSectionId = sectionId;

        if (classId == null && sectionId == null) {
            Optional<Student> ownStudent = currentStudentIfRole(AppConstants.ROLE_STUDENT);
            if (ownStudent.isPresent()) {
                effectiveClassId = ownStudent.get().getSchoolClass().getId();
                effectiveSectionId = ownStudent.get().getSection().getId();
            }
        }

        Specification<Assignment> spec = new SpecificationBuilder<Assignment>()
                .with(effectiveClassId != null, "schoolClass.id", SearchOperation.EQUALS, effectiveClassId)
                .with(effectiveSectionId != null, "section.id", SearchOperation.EQUALS, effectiveSectionId)
                .with(subjectId != null, "subject.id", SearchOperation.EQUALS, subjectId)
                .with(teacherId != null, "teacher.id", SearchOperation.EQUALS, teacherId)
                .build();

        Page<Assignment> page = assignmentRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Override
    @Transactional
    public AssignmentDto create(AssignmentFormRequest request, MultipartFile file) {
        validateDates(request);
        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        Subject subject = findSubject(request.getSubjectId());
        Teacher teacher = resolveTeacherForWrite(request.getTeacherId());

        String fileUrl = (file != null && !file.isEmpty()) ? fileStorageService.storeDocument(file) : null;

        Assignment assignment = Assignment.builder()
                .schoolClass(schoolClass)
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(fileUrl)
                .assignedDate(request.getAssignedDate())
                .dueDate(request.getDueDate())
                .build();

        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public AssignmentDto update(Long id, AssignmentFormRequest request, MultipartFile file) {
        validateDates(request);
        Assignment assignment = findEntity(id);

        assignment.setSchoolClass(findClass(request.getClassId()));
        assignment.setSection(findSection(request.getSectionId()));
        assignment.setSubject(findSubject(request.getSubjectId()));
        assignment.setTeacher(resolveTeacherForWrite(request.getTeacherId()));
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setAssignedDate(request.getAssignedDate());
        assignment.setDueDate(request.getDueDate());

        if (file != null && !file.isEmpty()) {
            String oldFileUrl = assignment.getFileUrl();
            assignment.setFileUrl(fileStorageService.storeDocument(file));
            if (StringUtils.hasText(oldFileUrl)) {
                fileStorageService.delete(oldFileUrl);
            }
        }

        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Assignment assignment = findEntity(id);
        if (StringUtils.hasText(assignment.getFileUrl())) {
            fileStorageService.delete(assignment.getFileUrl());
        }
        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDto> getSubmissions(Long assignmentId) {
        findEntity(assignmentId);
        return submissionRepository.findAllByAssignmentIdOrderByIdAsc(assignmentId).stream()
                .map(this::toSubmissionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmissionDto getMySubmission(Long assignmentId) {
        findEntity(assignmentId);
        Student student = currentStudent();
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .map(this::toSubmissionDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public AssignmentSubmissionDto submit(Long assignmentId, MultipartFile file) {
        Assignment assignment = findEntity(assignmentId);
        Student student = currentStudent();

        String fileUrl = fileStorageService.storeDocument(file);
        LocalDateTime now = LocalDateTime.now();
        boolean late = assignment.getDueDate() != null && now.isAfter(assignment.getDueDate().atTime(LocalTime.MAX));

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseGet(() -> AssignmentSubmission.builder().assignment(assignment).student(student).build());

        if (StringUtils.hasText(submission.getFileUrl()) && !submission.getFileUrl().equals(fileUrl)) {
            fileStorageService.delete(submission.getFileUrl());
        }
        submission.setFileUrl(fileUrl);
        submission.setSubmittedAt(now);
        submission.setStatus(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);

        return toSubmissionDto(submissionRepository.save(submission));
    }

    @Override
    @Transactional
    public AssignmentSubmissionDto gradeSubmission(Long submissionId, GradeSubmissionRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentSubmission", "id", submissionId));

        submission.setMarksObtained(request.getMarksObtained());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);

        return toSubmissionDto(submissionRepository.save(submission));
    }

    private void validateDates(AssignmentFormRequest request) {
        if (request.getDueDate() != null && request.getAssignedDate() != null
                && request.getDueDate().isBefore(request.getAssignedDate())) {
            throw new BadRequestException("Due date must not be before the assigned date");
        }
    }

    /** TEACHER/CLASS_TEACHER callers are always pinned to their own teacher record; only an admin
     *  caller's explicit teacherId (if any) is honored — see AssignmentFormRequest.teacherId. */
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

    private Student currentStudent() {
        Long userId = SecurityUtils.getCurrentUserId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No student record found for the current user"));
    }

    private AssignmentDto toDto(Assignment assignment) {
        Teacher teacher = assignment.getTeacher();
        return AssignmentDto.builder()
                .id(assignment.getId())
                .classId(assignment.getSchoolClass().getId())
                .className(assignment.getSchoolClass().getClassName())
                .sectionId(assignment.getSection().getId())
                .sectionName(assignment.getSection().getSectionName())
                .subjectId(assignment.getSubject().getId())
                .subjectName(assignment.getSubject().getSubjectName())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null && teacher.getUser() != null
                        ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                        : null)
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .fileUrl(assignment.getFileUrl())
                .assignedDate(assignment.getAssignedDate())
                .dueDate(assignment.getDueDate())
                .build();
    }

    private AssignmentSubmissionDto toSubmissionDto(AssignmentSubmission submission) {
        Student student = submission.getStudent();
        return AssignmentSubmissionDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(student.getId())
                .studentName(student.getUser() != null
                        ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                        : null)
                .admissionNumber(student.getAdmissionNumber())
                .fileUrl(submission.getFileUrl())
                .submittedAt(submission.getSubmittedAt())
                .marksObtained(submission.getMarksObtained())
                .feedback(submission.getFeedback())
                .status(submission.getStatus().name())
                .build();
    }

    private Assignment findEntity(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", id));
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
