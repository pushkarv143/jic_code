package com.school.sms.service.impl;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.request.PromoteStudentsRequest;
import com.school.sms.dto.request.StudentCreateRequest;
import com.school.sms.dto.request.StudentSelfUpdateRequest;
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
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.EmailService;
import com.school.sms.service.FileStorageService;
import com.school.sms.service.StudentService;
import com.school.sms.util.AppConstants;
import com.school.sms.entity.RefreshToken;
import com.school.sms.repository.RefreshTokenRepository;
import com.school.sms.service.SmsService;
import com.school.sms.util.CredentialGenerator;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    private final StudentAccessGuard studentAccessGuard;
    private final CredentialGenerator credentialGenerator;
    private final SmsService smsService;
    private final RefreshTokenRepository refreshTokenRepository;
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

        // Row-level scoping, applied as a predicate rather than a post-filter so the
        // page count and the page contents agree. null = caller sees everyone; an
        // empty list = caller legitimately sees nobody, which must stay empty rather
        // than degrade into "no filter".
        List<Long> scopedIds = studentAccessGuard.resolveStudentDirectoryScope();
        if (scopedIds != null) {
            Specification<Student> scopeSpec = scopedIds.isEmpty()
                    ? (root, query, cb) -> cb.disjunction()
                    : (root, query, cb) -> root.get("id").in(scopedIds);
            spec = spec == null ? scopeSpec : spec.and(scopeSpec);
        }

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

        // The account is provisioned by the school, always, as part of admitting the
        // student. Self-registration is gone, and the admin form no longer carries a
        // username or password field: both are generated here and emailed.
        //
        // An email address is what makes that possible, so it is the one thing still
        // required. Admitting without one used to mean "no login"; it now means the
        // credentials have nowhere to go, which is worth refusing rather than
        // silently creating an account nobody can be told about.
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BadRequestException(
                    "An email address is required: the student's username and first-time password are sent to it");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role studentRole = roleRepository.findByName(AppConstants.ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", AppConstants.ROLE_STUDENT));

        String username = credentialGenerator.usernameFor(
                request.getFirstName(), request.getLastName(), admissionNumber,
                userRepository::existsByUsername);
        String temporaryPassword = credentialGenerator.temporaryPassword();

        User user = User.builder()
                .username(username)
                .email(request.getEmail())
                .password(passwordEncoder.encode(temporaryPassword))
                // The account can do exactly one thing until its owner chooses a
                // password: replace it. See PasswordChangeRequiredFilter.
                .mustChangePassword(true)
                .firstName(StringUtils.hasText(request.getFirstName()) ? request.getFirstName() : username)
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .role(studentRole)
                .active(true)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        // Sent after the account exists and asynchronously, so a mail server that is
        // down delays the credentials rather than failing the admission. The password
        // is passed in and not stored: from here it lives only as a bcrypt hash and in
        // this one message.
        emailService.sendAccountCredentialsEmail(
                user.getEmail(),
                StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : username,
                username,
                temporaryPassword);

        Student student = Student.builder()
                .user(user)
                // Recorded on the student regardless of whether a login was created,
                // so admitting without an account no longer produces a nameless record.
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .admissionNumber(admissionNumber)
                .schoolClass(schoolClass)
                .section(section)
                .rollNumber(nextRollNumberIn(schoolClass.getId()))
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

        // No welcome email here. It used to greet the new account and name its
        // username, which made sense when the password was something an
        // administrator had typed and passed on separately. The credentials email
        // above now carries the username *and* the password, so sending both meant
        // two messages minutes apart, one of them missing the half that matters.
        // The null-check on the user went with it: provisioning is unconditional.

        return toFullDto(saved);
    }

    @Override
    @Transactional
    public StudentDto update(Long id, StudentUpdateRequest request) {
        Student student = findEntity(id);

        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = findSection(request.getSectionId());
        AcademicYear academicYear = findAcademicYear(request.getAcademicYearId());

        // Captured before the mapper runs, so a class change can be detected below.
        Long previousClassId = student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;

        studentMapper.updateEntityFromRequest(request, student);
        student.setSchoolClass(schoolClass);
        student.setSection(section);
        student.setAcademicYear(academicYear);

        // Moving a class means a new roll number: the old one is a position in the
        // class they left, and carrying it over would either collide with a student
        // already holding it in the new class — uq_students_class_roll would reject
        // the save — or leave them out of sequence if it happened to be free.
        if (previousClassId != null && !previousClassId.equals(schoolClass.getId())) {
            student.setRollNumber(nextRollNumberIn(schoolClass.getId()));
        }

        Student saved = studentRepository.save(student);
        // Name, email and phone live on the linked users row, so the mapper above
        // cannot reach them. Applied in the same transaction as the student save so
        // the two halves of one edit cannot diverge.
        applyIdentityFields(saved, request);

        auditLogService.record("UPDATE_STUDENT", "Student", saved.getId(), null, null);
        return toFullDto(saved);
    }

    /**
     * Applies the identity fields to the student, and mirrors them onto the linked
     * login account when one exists.
     *
     * The student row is the source of truth: it is always present, whereas the
     * account is optional. The mirror keeps the two consistent so that signing in
     * shows the same name the office sees, but its absence is never an error —
     * an earlier version rejected the edit outright when there was no account,
     * which made the four login-less students in the database uneditable.
     *
     * A null field means "leave unchanged", so a caller can update only the
     * academic details without echoing the name back.
     */
    private void applyIdentityFields(Student student, StudentUpdateRequest request) {
        boolean hasIdentityChange = StringUtils.hasText(request.getFirstName())
                || StringUtils.hasText(request.getLastName())
                || StringUtils.hasText(request.getEmail())
                || StringUtils.hasText(request.getPhone());
        if (!hasIdentityChange) {
            return;
        }

        User user = student.getUser();

        if (StringUtils.hasText(request.getEmail())) {
            // users.email is UNIQUE. Checked before writing either row so a clash
            // surfaces as a message naming the field rather than a constraint
            // violation from deep in the persistence layer.
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> user == null || !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("User", "email", request.getEmail());
                    });
            student.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getFirstName())) {
            student.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            student.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            student.setPhone(request.getPhone());
        }
        studentRepository.save(student);

        if (user == null) {
            return;
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getOwnProfile() {
        return toFullDto(findOwnEntity());
    }

    @Override
    @Transactional
    public StudentDto updateOwnProfile(StudentSelfUpdateRequest request) {
        Student student = findOwnEntity();

        // Only the contact fields on StudentSelfUpdateRequest are copied. Everything
        // academic (class, section, roll number, status, academic year) is untouched
        // here and remains editable through the office-only update(...) path.
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            student.setCity(request.getCity());
        }
        if (request.getState() != null) {
            student.setState(request.getState());
        }
        if (request.getPincode() != null) {
            student.setPincode(request.getPincode());
        }
        if (request.getBloodGroup() != null) {
            student.setBloodGroup(request.getBloodGroup());
        }
        Student saved = studentRepository.save(student);

        if (request.getPhone() != null && saved.getUser() != null) {
            User user = saved.getUser();
            user.setPhone(request.getPhone());
            userRepository.save(user);
        }

        auditLogService.record("UPDATE_OWN_STUDENT_PROFILE", "Student", saved.getId(), null, null);
        return toFullDto(saved);
    }

    /**
     * Resolves "me" from the security context rather than from a client-supplied id,
     * so the self-service endpoints have no id to tamper with in the first place.
     * A PARENT has no single own record, so they are steered to the by-id endpoints
     * (which the guard already narrows to their own children).
     */
    private Student findOwnEntity() {
        Long userId = SecurityUtils.getCurrentUserId();
        return studentRepository.findByUserId(userId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BadRequestException(
                        "Your login is not linked to a student record"));
    }

    @Override
    @Transactional
    public void resendCredentials(Long id) {
        Student student = findEntity(id);
        User user = student.getUser();
        if (user == null) {
            // Admitted before provisioning was automatic. There is no account to
            // reset, and silently creating one here would hide that from the caller.
            throw new BadRequestException(
                    "This student has no login yet, so there are no credentials to resend");
        }

        String temporaryPassword = credentialGenerator.temporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        // Re-arms the forced first-login reset: the account is back on a password the
        // student did not choose, so it may do nothing but replace it.
        user.setMustChangePassword(true);
        userRepository.save(user);

        // Every existing session dies with the old password. Without this a device
        // already signed in would keep working indefinitely on credentials that no
        // longer exist, and the forced reset would apply to everyone except whoever
        // is holding that device — which is the one person it was aimed at.
        List<RefreshToken> live = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        live.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(live);

        // Audited before the sending, so the record exists even if delivery fails.
        // A credential reset is a thing done to somebody else's account, so who did
        // it and when is the point; the password itself is never written down.
        auditLogService.record("RESEND_STUDENT_CREDENTIALS", "Student", student.getId(),
                null, "username=" + user.getUsername() + ", sessionsRevoked=" + live.size());

        String displayName = StringUtils.hasText(user.getFirstName())
                ? user.getFirstName()
                : user.getUsername();

        if (StringUtils.hasText(user.getEmail())) {
            emailService.sendAccountCredentialsEmail(
                    user.getEmail(), displayName, user.getUsername(), temporaryPassword);
        }

        // SMS is best effort and secondary: the student's own record first, falling
        // back to the account's number. A missing gateway or number leaves the email
        // as the delivery, which is why this neither throws nor is waited on.
        String phone = StringUtils.hasText(student.getPhone()) ? student.getPhone() : user.getPhone();
        if (StringUtils.hasText(phone) && smsService.isAvailable()) {
            smsService.send(phone,
                    "Greenwood School: your username is " + user.getUsername()
                            + " and your temporary password is " + temporaryPassword
                            + ". You will be asked to choose your own password when you sign in.");
        }

        log.info("Credentials resent for student {} (user {}), {} session(s) revoked",
                student.getId(), user.getId(), live.size());
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

        /*
         * Renumbered into the destination class, continuing its sequence.
         *
         * Promotion previously carried each student's old roll number across. With
         * uq_students_class_roll in place that fails outright the moment two
         * promoted students, or a promoted student and a sitting one, hold the same
         * number — which is the normal case when a whole class moves up.
         *
         * Ordered by the roll they held so the class keeps its existing sequence
         * rather than being shuffled by whatever order the ids arrived in, and
         * counted forward from one cursor so the batch does not re-query the max
         * between every student.
         */
        int nextRoll = nextRollNumberIn(toClass.getId());
        List<Student> inRollOrder = students.stream()
                .sorted(java.util.Comparator.comparing(
                        Student::getRollNumber, java.util.Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (Student student : inRollOrder) {
            // A student already in the destination class keeps their number: they are
            // not moving, and renumbering them would collide with the cursor.
            boolean alreadyThere = student.getSchoolClass() != null
                    && student.getSchoolClass().getId().equals(toClass.getId());
            student.setSchoolClass(toClass);
            student.setSection(toSection);
            student.setAcademicYear(academicYear);
            if (!alreadyThere) {
                student.setRollNumber(nextRoll++);
            }
        }
        studentRepository.saveAll(inRollOrder);

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

    /**
     * The single lookup every by-id operation in this service funnels through, which
     * is why the row-level check lives here rather than in each caller: a new endpoint
     * that forgets to ask is scoped anyway. The guard is a no-op for management/office
     * roles, so this costs nothing on the admin paths.
     *
     * <p>Existence is checked before access on purpose — a caller asking for a student
     * id that does not exist gets 404 regardless of role, so the response cannot be
     * used to probe which ids are real.
     */
    /**
     * The next roll number in a class. Always server-assigned.
     *
     * <p>A roll number is a student's position in their class, not a fact about
     * them, so nobody types it. It used to be accepted from the caller and merely
     * validated, which produced duplicates and values like 151611; the request DTOs
     * no longer carry the field at all, and {@code uq_students_class_roll} enforces
     * uniqueness underneath.
     *
     * <p>Scoped to the class rather than the section, matching that key: two
     * students in different sections of one class should not share roll 1.
     *
     * <p>Gaps are left alone. Numbering from {@code max + 1} means a student who
     * leaves does not cause the class to be renumbered around them — every other
     * student's roll would change, and a roll number is what a register, a mark
     * sheet and a parent all refer to.
     */
    private Integer nextRollNumberIn(Long classId) {
        Integer highest = studentRepository.findMaxRollNumberInClass(classId);
        return highest == null ? 1 : highest + 1;
    }

    private Student findEntity(Long id) {
        Student student = studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        studentAccessGuard.verifyCanViewStudentRecord(id);
        return student;
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
