package com.school.sms.service.impl;

import com.school.sms.dto.request.LeaveApplicationRequest;
import com.school.sms.dto.response.LeaveApplicationDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.LeaveApplicantType;
import com.school.sms.entity.LeaveApplication;
import com.school.sms.entity.LeaveStatus;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.LeaveApplicationRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.LeaveApplicationService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    private static final String ROLE_PREFIX = AppConstants.JWT_ROLE_PREFIX;

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    // Needed only to answer "is this applicant one of my homeroom students?" when a
    // class teacher decides student leave.
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveApplicationDto> getAll(String applicantType, String status, Pageable pageable) {
        Specification<LeaveApplication> spec = new SpecificationBuilder<LeaveApplication>()
                .with(StringUtils.hasText(applicantType), "applicantType", SearchOperation.EQUALS,
                        StringUtils.hasText(applicantType) ? LeaveApplicantType.valueOf(applicantType.toUpperCase()) : null)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? LeaveStatus.valueOf(status.toUpperCase()) : null)
                .build();

        // Row-level scoping, for the same reason the decision check exists: a class
        // teacher who may only approve their own homeroom's leave has no business
        // reading the rest of the school's applications, reasons included. Applied as
        // a predicate rather than a post-filter so the page count matches the rows.
        // null = sees everything (management); an empty list = sees nothing, which
        // must stay empty rather than degrade into "no filter".
        List<Long> scopedApplicantIds = resolveLeaveApplicantScope();
        if (scopedApplicantIds != null) {
            Specification<LeaveApplication> scopeSpec = scopedApplicantIds.isEmpty()
                    ? (root, query, cb) -> cb.disjunction()
                    : (root, query, cb) -> root.get("applicantId").in(scopedApplicantIds);
            spec = spec == null ? scopeSpec : spec.and(scopeSpec);
        }

        Page<LeaveApplication> page = leaveApplicationRepository.findAll(spec, pageable);
        return toPageResponse(page);
    }

    /**
     * The applicant user ids the caller may see in the management list, or null when
     * they may see every one of them.
     *
     * <p>Only a class teacher is narrowed: to the students of their own homeroom,
     * which is exactly the set they can decide on. A class teacher with no homeroom
     * sees an empty list rather than everything.
     */
    private List<Long> resolveLeaveApplicantScope() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean coreApprover = authorities.contains(ROLE_PREFIX + AppConstants.ROLE_SUPER_ADMIN)
                || authorities.contains(ROLE_PREFIX + AppConstants.ROLE_PRINCIPAL)
                || authorities.contains(ROLE_PREFIX + AppConstants.ROLE_VICE_PRINCIPAL);
        if (coreApprover) {
            return null;
        }
        // No role check: the lookup below *is* the check. It resolves the caller's
        // own homeroom section and returns empty when they head none, which was
        // already the only thing the old CLASS_TEACHER role test could tell us.
        return teacherRepository.findByUserId(principal.getId())
                .flatMap(teacher -> sectionRepository.findByClassTeacherId(teacher.getId()))
                .map(section -> studentRepository.findUserIdsBySectionId(section.getId()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveApplicationDto> getMy(Pageable pageable) {
        Page<LeaveApplication> page = leaveApplicationRepository
                .findAllByApplicantId(SecurityUtils.getCurrentUserId(), pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional
    public LeaveApplicationDto create(LeaveApplicationRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must not be before start date");
        }

        User applicant = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", SecurityUtils.getCurrentUserId()));

        LeaveApplication application = LeaveApplication.builder()
                .applicantId(applicant.getId())
                .applicantType(deriveApplicantType(applicant.getRole().getName()))
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toDto(saved, Map.of(applicant.getId(), applicant));
    }

    @Override
    @Transactional
    public LeaveApplicationDto approve(Long id) {
        LeaveApplication application = findEntity(id);
        ensureCanDecide(application);

        application.setStatus(LeaveStatus.APPROVED);
        application.setApprovedBy(SecurityUtils.getCurrentUserId());
        LeaveApplication saved = leaveApplicationRepository.save(application);
        auditLogService.record("LEAVE_APPROVED", "LeaveApplication", saved.getId(), null, null);
        return toDto(saved, resolveUsers(application));
    }

    @Override
    @Transactional
    public LeaveApplicationDto reject(Long id) {
        LeaveApplication application = findEntity(id);
        ensureCanDecide(application);

        application.setStatus(LeaveStatus.REJECTED);
        application.setApprovedBy(SecurityUtils.getCurrentUserId());
        LeaveApplication saved = leaveApplicationRepository.save(application);
        auditLogService.record("LEAVE_REJECTED", "LeaveApplication", saved.getId(), null, null);
        return toDto(saved, resolveUsers(application));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LeaveApplication application = findEntity(id);

        if (!application.getApplicantId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You may only delete your own leave application");
        }
        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only a pending leave application can be deleted");
        }
        leaveApplicationRepository.delete(application);
    }

    /**
     * Who may approve or reject this application.
     *
     * <p>Management decides anything. A class teacher decides student leave, but
     * only for a student in <em>their own</em> homeroom: the role alone used to be
     * enough, which let any class teacher in the school approve any student's
     * leave — including students they have never taught.
     *
     * <p>The homeroom is resolved through the section (a class teacher is stored as
     * {@code sections.class_teacher_id}), so a class teacher with no homeroom
     * decides nothing, and staff or teacher leave is never theirs to decide.
     */
    private void ensureCanDecide(LeaveApplication application) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean coreApprover = authorities.contains(ROLE_PREFIX + AppConstants.ROLE_SUPER_ADMIN)
                || authorities.contains(ROLE_PREFIX + AppConstants.ROLE_PRINCIPAL)
                || authorities.contains(ROLE_PREFIX + AppConstants.ROLE_VICE_PRINCIPAL);
        if (coreApprover) {
            return;
        }

        // isOwnHomeroomStudent resolves the approver's own homeroom and returns
        // false when they have none, so it already answers "is this a class teacher,
        // and is this their student" in one question. The separate CLASS_TEACHER
        // role test that used to guard it added nothing once the role was gone.
        if (application.getApplicantType() == LeaveApplicantType.STUDENT
                && isOwnHomeroomStudent(principal.getId(), application.getApplicantId())) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to approve/reject this leave application");
    }

    /**
     * True when {@code applicantUserId} is a student sitting in the homeroom that
     * {@code approverUserId} is the class teacher of.
     *
     * <p>Fails closed at every missing link — no teacher record, no homeroom, no
     * student record, no section — because each of those means the approver cannot
     * be shown to be this student's class teacher.
     */
    private boolean isOwnHomeroomStudent(Long approverUserId, Long applicantUserId) {
        if (approverUserId == null || applicantUserId == null) {
            return false;
        }
        Long homeroomSectionId = teacherRepository.findByUserId(approverUserId)
                .flatMap(teacher -> sectionRepository.findByClassTeacherId(teacher.getId()))
                .map(Section::getId)
                .orElse(null);
        if (homeroomSectionId == null) {
            return false;
        }
        return studentRepository.findByUserId(applicantUserId)
                .map(Student::getSection)
                .map(Section::getId)
                .filter(homeroomSectionId::equals)
                .isPresent();
    }

    private LeaveApplicantType deriveApplicantType(String roleName) {
        if (AppConstants.ROLE_STUDENT.equals(roleName)) {
            return LeaveApplicantType.STUDENT;
        }
        if (AppConstants.ROLE_TEACHER.equals(roleName)) {
            return LeaveApplicantType.TEACHER;
        }
        return LeaveApplicantType.STAFF;
    }

    private PageResponse<LeaveApplicationDto> toPageResponse(Page<LeaveApplication> page) {
        Set<Long> userIds = page.getContent().stream()
                .flatMap(app -> java.util.stream.Stream.of(app.getApplicantId(), app.getApprovedBy()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> usersById = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        return PageResponse.from(page, page.getContent().stream().map(app -> toDto(app, usersById)).toList());
    }

    private Map<Long, User> resolveUsers(LeaveApplication application) {
        Map<Long, User> map = new HashMap<>();
        userRepository.findById(application.getApplicantId()).ifPresent(u -> map.put(u.getId(), u));
        if (application.getApprovedBy() != null) {
            userRepository.findById(application.getApprovedBy()).ifPresent(u -> map.put(u.getId(), u));
        }
        return map;
    }

    private LeaveApplicationDto toDto(LeaveApplication application, Map<Long, User> usersById) {
        User applicant = usersById.get(application.getApplicantId());
        User approver = application.getApprovedBy() != null ? usersById.get(application.getApprovedBy()) : null;

        return LeaveApplicationDto.builder()
                .id(application.getId())
                .applicantId(application.getApplicantId())
                .applicantName(applicant != null ? NameUtil.fullName(applicant.getFirstName(), applicant.getLastName()) : null)
                .applicantRole(applicant != null && applicant.getRole() != null ? applicant.getRole().getName() : null)
                .applicantType(application.getApplicantType().name())
                .leaveType(application.getLeaveType())
                .startDate(application.getStartDate())
                .endDate(application.getEndDate())
                .reason(application.getReason())
                .status(application.getStatus().name())
                .approvedBy(application.getApprovedBy())
                .approvedByName(approver != null ? NameUtil.fullName(approver.getFirstName(), approver.getLastName()) : null)
                .appliedAt(application.getAppliedAt())
                .build();
    }

    private LeaveApplication findEntity(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveApplication", "id", id));
    }
}
