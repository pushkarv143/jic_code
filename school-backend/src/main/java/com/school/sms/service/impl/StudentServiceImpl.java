package com.school.sms.service.impl;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.request.PromoteStudentsRequest;
import com.school.sms.dto.request.StudentCreateRequest;
import com.school.sms.dto.request.StudentStatusRequest;
import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.dto.request.TransferStudentRequest;
import com.school.sms.dto.response.GuardianDto;
import com.school.sms.dto.response.MedicalDetailsDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDocumentDto;
import com.school.sms.dto.response.StudentDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.Guardian;
import com.school.sms.entity.Role;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentDocument;
import com.school.sms.entity.StudentMedicalDetails;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.GuardianMapper;
import com.school.sms.mapper.MedicalDetailsMapper;
import com.school.sms.mapper.StudentDocumentMapper;
import com.school.sms.mapper.StudentMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.GuardianRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentDocumentRepository;
import com.school.sms.repository.StudentMedicalDetailsRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.EmailService;
import com.school.sms.service.FileStorageService;
import com.school.sms.service.StudentService;
import com.school.sms.util.AppConstants;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GuardianRepository guardianRepository;
    private final StudentMedicalDetailsRepository medicalDetailsRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final StudentMapper studentMapper;
    private final GuardianMapper guardianMapper;
    private final MedicalDetailsMapper medicalDetailsMapper;
    private final StudentDocumentMapper studentDocumentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentDto> getAll(String search, Long classId, Long sectionId, String status,
                                            int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<Student> spec = new SpecificationBuilder<Student>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(sectionId != null, "section.id", SearchOperation.EQUALS, sectionId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? StudentStatus.valueOf(status.toUpperCase()) : null)
                .build();

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase();
            Specification<Student> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("admissionNumber")), "%" + term + "%"),
                    cb.like(cb.lower(root.join("user", jakarta.persistence.criteria.JoinType.LEFT).get("firstName")), "%" + term + "%"),
                    cb.like(cb.lower(root.join("user", jakarta.persistence.criteria.JoinType.LEFT).get("lastName")), "%" + term + "%")
            );
            spec = spec == null ? searchSpec : spec.and(searchSpec);
        }

        Page<Student> studentPage = studentRepository.findAll(spec, pageable);
        Page<StudentDto> dtoPage = studentPage.map(this::toListDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getById(Long id) {
        Student student = findEntity(id);
        return toFullDto(student);
    }

    @Override
    @Transactional
    public StudentDto create(StudentCreateRequest request) {
        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        String admissionNumber = StringUtils.hasText(request.getAdmissionNumber())
                ? request.getAdmissionNumber()
                : generateAdmissionNumber();
        if (studentRepository.existsByAdmissionNumber(admissionNumber)) {
            throw new DuplicateResourceException("Student", "admissionNumber", admissionNumber);
        }

        User user = null;
        boolean creatingLogin = StringUtils.hasText(request.getUsername()) || StringUtils.hasText(request.getPassword());
        if (creatingLogin) {
            if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                    || !StringUtils.hasText(request.getEmail())) {
                throw new BadRequestException("Username, email and password are all required to create a student login");
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("User", "username", request.getUsername());
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }

            Role studentRole = roleRepository.findByName(AppConstants.ROLE_STUDENT)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", AppConstants.ROLE_STUDENT));

            user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(StringUtils.hasText(request.getFirstName()) ? request.getFirstName() : request.getUsername())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .gender(request.getGender())
                    .role(studentRole)
                    .active(true)
                    .emailVerified(false)
                    .build();
            user = userRepository.save(user);
        }

        Student student = Student.builder()
                .user(user)
                .admissionNumber(admissionNumber)
                .schoolClass(schoolClass)
                .section(section)
                .rollNumber(request.getRollNumber())
                .admissionDate(request.getAdmissionDate())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .religion(request.getReligion())
                .category(request.getCategory())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .academicYear(academicYear)
                .status(StudentStatus.ACTIVE)
                .deleted(false)
                .build();

        if (request.getGuardians() != null) {
            for (GuardianRequest guardianRequest : request.getGuardians()) {
                Guardian guardian = guardianMapper.toEntity(guardianRequest);
                guardian.setStudent(student);
                student.getGuardians().add(guardian);
            }
        }

        Student saved = studentRepository.save(student);
        auditLogService.record("CREATE_STUDENT", "Student", saved.getId(), null, null);

        if (user != null) {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), user.getUsername());
        }

        return toFullDto(saved);
    }

    @Override
    @Transactional
    public StudentDto update(Long id, StudentUpdateRequest request) {
        Student student = findEntity(id);

        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        studentMapper.updateEntityFromRequest(request, student);
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setAcademicYear(academicYear);

        Student saved = studentRepository.save(student);
        auditLogService.record("UPDATE_STUDENT", "Student", saved.getId(), null, null);
        return toFullDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = findEntity(id);
        student.setDeleted(true);
        studentRepository.save(student);
        auditLogService.record("DELETE_STUDENT", "Student", student.getId(), null, null);

        if (student.getUser() != null) {
            User user = student.getUser();
            user.setActive(false);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public StudentDto updateStatus(Long id, StudentStatusRequest request) {
        Student student = findEntity(id);
        student.setStatus(request.getStatus());
        return toFullDto(studentRepository.save(student));
    }

    @Override
    @Transactional
    public String uploadPhoto(Long id, MultipartFile file) {
        Student student = findEntity(id);
        String oldPhotoUrl = student.getPhotoUrl();

        String url = fileStorageService.storePhoto(file);
        student.setPhotoUrl(url);
        studentRepository.save(student);

        if (StringUtils.hasText(oldPhotoUrl)) {
            fileStorageService.delete(oldPhotoUrl);
        }

        return url;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianDto> getGuardians(Long studentId) {
        findEntity(studentId);
        return guardianRepository.findAllByStudentIdOrderByIdAsc(studentId).stream()
                .map(guardianMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public GuardianDto addGuardian(Long studentId, GuardianRequest request) {
        Student student = findEntity(studentId);
        Guardian guardian = guardianMapper.toEntity(request);
        guardian.setStudent(student);
        return guardianMapper.toDto(guardianRepository.save(guardian));
    }

    @Override
    @Transactional
    public GuardianDto updateGuardian(Long studentId, Long guardianId, GuardianRequest request) {
        Guardian guardian = findGuardian(studentId, guardianId);
        guardianMapper.updateEntityFromRequest(request, guardian);
        return guardianMapper.toDto(guardianRepository.save(guardian));
    }

    @Override
    @Transactional
    public void deleteGuardian(Long studentId, Long guardianId) {
        Guardian guardian = findGuardian(studentId, guardianId);
        guardianRepository.delete(guardian);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalDetailsDto getMedicalDetails(Long studentId) {
        findEntity(studentId);
        return medicalDetailsRepository.findByStudentId(studentId)
                .map(medicalDetailsMapper::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public MedicalDetailsDto upsertMedicalDetails(Long studentId, MedicalDetailsRequest request) {
        Student student = findEntity(studentId);
        StudentMedicalDetails details = medicalDetailsRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentMedicalDetails created = medicalDetailsMapper.toEntity(request);
                    created.setStudent(student);
                    return created;
                });
        medicalDetailsMapper.updateEntityFromRequest(request, details);
        details.setStudent(student);
        return medicalDetailsMapper.toDto(medicalDetailsRepository.save(details));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDocumentDto> getDocuments(Long studentId) {
        findEntity(studentId);
        return studentDocumentRepository.findAllByStudentIdOrderByIdDesc(studentId).stream()
                .map(studentDocumentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StudentDocumentDto addDocument(Long studentId, String documentType, MultipartFile file) {
        Student student = findEntity(studentId);
        String url = fileStorageService.storeDocument(file);

        StudentDocument document = StudentDocument.builder()
                .student(student)
                .documentType(documentType)
                .fileUrl(url)
                .uploadedAt(LocalDateTime.now())
                .build();

        return studentDocumentMapper.toDto(studentDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public void deleteDocument(Long studentId, Long docId) {
        StudentDocument document = studentDocumentRepository.findById(docId)
                .filter(d -> d.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new ResourceNotFoundException("StudentDocument", "id", docId));

        fileStorageService.delete(document.getFileUrl());
        studentDocumentRepository.delete(document);
    }

    @Override
    @Transactional
    public int promote(PromoteStudentsRequest request) {
        SchoolClass toClass = findClass(request.getToClassId());
        Section toSection = findSection(request.getToSectionId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        List<Student> students = studentRepository.findAllByIdIn(request.getStudentIds());
        students.forEach(student -> {
            student.setSchoolClass(toClass);
            student.setSection(toSection);
            student.setAcademicYear(academicYear);
        });
        studentRepository.saveAll(students);

        return students.size();
    }

    @Override
    @Transactional
    public void transfer(Long id, TransferStudentRequest request) {
        // Note: `remarks` has no corresponding column on the students table per
        // SCHEMA_CONTRACT.md, so it is accepted but intentionally not persisted here.
        Student student = findEntity(id);
        student.setStatus(StudentStatus.TRANSFERRED);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void markAlumni(Long id) {
        Student student = findEntity(id);
        student.setStatus(StudentStatus.ALUMNI);
        studentRepository.save(student);
    }

    private StudentDto toListDto(Student student) {
        StudentDto dto = studentMapper.toDto(student);
        enrichPrimaryGuardian(student.getId(), dto);
        return dto;
    }

    private StudentDto toFullDto(Student student) {
        StudentDto dto = studentMapper.toDto(student);
        List<Guardian> guardians = guardianRepository.findAllByStudentIdOrderByIdAsc(student.getId());
        dto.setGuardians(guardians.stream().map(guardianMapper::toDto).toList());
        setPrimaryGuardian(dto, guardians);

        medicalDetailsRepository.findByStudentId(student.getId())
                .ifPresent(details -> dto.setMedicalDetails(medicalDetailsMapper.toDto(details)));

        dto.setDocuments(studentDocumentRepository.findAllByStudentIdOrderByIdDesc(student.getId()).stream()
                .map(studentDocumentMapper::toDto)
                .toList());

        return dto;
    }

    private void enrichPrimaryGuardian(Long studentId, StudentDto dto) {
        List<Guardian> guardians = guardianRepository.findAllByStudentIdOrderByIdAsc(studentId);
        setPrimaryGuardian(dto, guardians);
    }

    private void setPrimaryGuardian(StudentDto dto, List<Guardian> guardians) {
        guardians.stream()
                .filter(Guardian::isPrimary)
                .findFirst()
                .or(() -> guardians.stream().findFirst())
                .ifPresent(guardian -> {
                    dto.setPrimaryGuardianName(guardian.getName());
                    dto.setPrimaryGuardianPhone(guardian.getPhone());
                });
    }

    private String generateAdmissionNumber() {
        int year = Year.now().getValue();
        String prefix = "ADM" + year;
        long sequence = studentRepository.countByAdmissionNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private Guardian findGuardian(Long studentId, Long guardianId) {
        return guardianRepository.findById(guardianId)
                .filter(g -> g.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "id", guardianId));
    }

    private Student findEntity(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
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

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }
}
