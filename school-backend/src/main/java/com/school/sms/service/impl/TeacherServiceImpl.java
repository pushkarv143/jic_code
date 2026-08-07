package com.school.sms.service.impl;

import com.school.sms.dto.request.TeacherCreateRequest;
import com.school.sms.dto.request.TeacherStatusRequest;
import com.school.sms.dto.request.TeacherUpdateRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.TeacherAssignmentDto;
import com.school.sms.dto.response.TeacherDto;
import com.school.sms.entity.ClassSubjectTeacher;
import com.school.sms.entity.Department;
import com.school.sms.entity.Designation;
import com.school.sms.entity.Role;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TeacherStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.TeacherMapper;
import com.school.sms.repository.ClassSubjectTeacherRepository;
import com.school.sms.repository.DepartmentRepository;
import com.school.sms.repository.DesignationRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.EmailService;
import com.school.sms.service.TeacherService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.specification.GenericSpecification;
import com.school.sms.util.specification.SearchCriteria;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TeacherMapper teacherMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherDto> getAll(String search, Long departmentId, Long designationId, String status,
                                            int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<Teacher> spec = new SpecificationBuilder<Teacher>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(departmentId != null, "department.id", SearchOperation.EQUALS, departmentId)
                .with(designationId != null, "designation.id", SearchOperation.EQUALS, designationId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? TeacherStatus.valueOf(status.toUpperCase()) : null)
                .build();

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase();
            Specification<Teacher> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("user").get("firstName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("lastName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("username")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("email")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("employeeId")), "%" + term + "%")
            );
            spec = spec == null ? searchSpec : spec.and(searchSpec);
        }

        Page<Teacher> teacherPage = teacherRepository.findAll(spec, pageable);
        Page<TeacherDto> dtoPage = teacherPage.map(teacherMapper::toDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDto getById(Long id) {
        Teacher teacher = findEntity(id);
        TeacherDto dto = teacherMapper.toDto(teacher);
        dto.setAssignments(getAssignments(id));
        return dto;
    }

    @Override
    @Transactional
    public TeacherDto create(TeacherCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));
        Role teacherRole = roleRepository.findByName(AppConstants.ROLE_TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", AppConstants.ROLE_TEACHER));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .role(teacherRole)
                .active(true)
                .emailVerified(false)
                .build();
        User savedUser = userRepository.save(user);

        Teacher teacher = Teacher.builder()
                .user(savedUser)
                .employeeId(generateEmployeeId())
                .department(department)
                .designation(designation)
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears())
                .joiningDate(request.getJoiningDate())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .bloodGroup(request.getBloodGroup())
                .emergencyContact(request.getEmergencyContact())
                .salary(request.getSalary())
                .employmentType(request.getEmploymentType())
                .status(TeacherStatus.ACTIVE)
                .deleted(false)
                .build();
        Teacher savedTeacher = teacherRepository.save(teacher);

        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getUsername());

        return teacherMapper.toDto(savedTeacher);
    }

    @Override
    @Transactional
    public TeacherDto update(Long id, TeacherUpdateRequest request) {
        Teacher teacher = findEntity(id);
        User user = teacher.getUser();

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        userRepository.save(user);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId()));

        teacherMapper.updateEntityFromRequest(request, teacher);
        teacher.setDepartment(department);
        teacher.setDesignation(designation);
        teacher.setGender(request.getGender());

        return teacherMapper.toDto(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Teacher teacher = findEntity(id);
        teacher.setDeleted(true);
        teacher.setStatus(TeacherStatus.INACTIVE);
        teacherRepository.save(teacher);

        User user = teacher.getUser();
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public TeacherDto updateStatus(Long id, TeacherStatusRequest request) {
        Teacher teacher = findEntity(id);
        teacher.setStatus(request.getStatus());
        return teacherMapper.toDto(teacherRepository.save(teacher));
    }

    private List<TeacherAssignmentDto> getAssignments(Long teacherId) {
        Specification<ClassSubjectTeacher> spec = new GenericSpecification<>(
                new SearchCriteria("teacher.id", SearchOperation.EQUALS, teacherId));
        return classSubjectTeacherRepository.findAll(spec).stream()
                .map(cst -> TeacherAssignmentDto.builder()
                        .id(cst.getId())
                        .classId(cst.getSchoolClass().getId())
                        .className(cst.getSchoolClass().getClassName())
                        .sectionId(cst.getSection().getId())
                        .sectionName(cst.getSection().getSectionName())
                        .subjectId(cst.getSubject().getId())
                        .subjectName(cst.getSubject().getSubjectName())
                        .build())
                .toList();
    }

    private String generateEmployeeId() {
        int year = Year.now().getValue();
        String prefix = "EMP" + year;
        long sequence = teacherRepository.countByEmployeeIdStartingWith(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private Teacher findEntity(Long id) {
        return teacherRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
    }
}
