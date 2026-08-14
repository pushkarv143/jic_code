package com.school.sms.service.impl;

import com.school.sms.dto.request.StudyMaterialRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudyMaterialDto;
import com.school.sms.entity.MaterialType;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudyMaterial;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.StudyMaterialRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.security.SectionAccessGuard;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.FileStorageService;
import com.school.sms.service.StudyMaterialService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.NameUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StudyMaterialServiceImpl implements StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;
    private final SectionAccessGuard sectionAccessGuard;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudyMaterialDto> getAll(Long classId, Long sectionId, Long subjectId, String materialType,
                                                  String search, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<StudyMaterial> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));

        if (classId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("schoolClass").get("id"), classId));
        }
        if (sectionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("section").get("id"), sectionId));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }
        if (StringUtils.hasText(materialType)) {
            MaterialType type = parseType(materialType);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("materialType"), type));
        }
        if (StringUtils.hasText(search)) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), term),
                    cb.like(cb.lower(root.get("description")), term)));
        }

        Specification<StudyMaterial> visibility = visibilitySpecification();
        if (visibility != null) {
            spec = spec.and(visibility);
        }

        Page<StudyMaterial> resultPage = studyMaterialRepository.findAll(spec, pageable);
        return PageResponse.from(resultPage.map(this::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public StudyMaterialDto getById(Long id) {
        StudyMaterial material = findEntity(id);
        verifyCanView(material);
        return toDto(material);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyMaterialDto getForDownload(Long id) {
        // Same check as getById. A material's file lives under /uploads, which is
        // served without authentication, so the download endpoint hands back the
        // path only after confirming the caller may see the material at all.
        return getById(id);
    }

    @Override
    @Transactional
    public StudyMaterialDto create(StudyMaterialRequest request, MultipartFile file) {
        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = request.getSectionId() == null ? null : findSection(request.getSectionId());
        Subject subject = findSubject(request.getSubjectId());

        verifyCanPublishTo(schoolClass, section);
        verifySubjectBelongsToClass(subject, schoolClass);
        verifySectionBelongsToClass(section, schoolClass);

        String fileUrl = resolveSource(request, file, null);

        StudyMaterial material = StudyMaterial.builder()
                .schoolClass(schoolClass)
                .section(section)
                .subject(subject)
                // Stamped from the security context, never from the request: a teacher
                // cannot file a material under a colleague's name.
                .teacher(currentTeacher().orElse(null))
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .fileUrl(fileUrl)
                .externalUrl(StringUtils.hasText(request.getExternalUrl()) ? request.getExternalUrl() : null)
                .published(request.getPublished() == null || request.getPublished())
                .deleted(false)
                .build();

        StudyMaterial saved = studyMaterialRepository.save(material);
        auditLogService.record("CREATE_STUDY_MATERIAL", "StudyMaterial", saved.getId(), null, null);
        return toDto(saved);
    }

    @Override
    @Transactional
    public StudyMaterialDto update(Long id, StudyMaterialRequest request, MultipartFile file) {
        StudyMaterial material = findEntity(id);
        verifyCanManage(material);

        SchoolClass schoolClass = findClass(request.getClassId());
        Section section = request.getSectionId() == null ? null : findSection(request.getSectionId());
        Subject subject = findSubject(request.getSubjectId());

        // Checked against the *target* class/section too, so a material cannot be
        // moved into a section the caller has no claim to.
        verifyCanPublishTo(schoolClass, section);
        verifySubjectBelongsToClass(subject, schoolClass);
        verifySectionBelongsToClass(section, schoolClass);

        String previousFileUrl = material.getFileUrl();
        String fileUrl = resolveSource(request, file, previousFileUrl);

        material.setSchoolClass(schoolClass);
        material.setSection(section);
        material.setSubject(subject);
        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setMaterialType(request.getMaterialType());
        material.setFileUrl(fileUrl);
        material.setExternalUrl(StringUtils.hasText(request.getExternalUrl()) ? request.getExternalUrl() : null);
        if (request.getPublished() != null) {
            material.setPublished(request.getPublished());
        }

        StudyMaterial saved = studyMaterialRepository.save(material);

        // Only after the row is safely updated, so a failed save does not leave the
        // record pointing at a file that has already been deleted.
        if (file != null && !file.isEmpty() && StringUtils.hasText(previousFileUrl)
                && !previousFileUrl.equals(fileUrl)) {
            fileStorageService.delete(previousFileUrl);
        }

        auditLogService.record("UPDATE_STUDY_MATERIAL", "StudyMaterial", saved.getId(), null, null);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StudyMaterial material = findEntity(id);
        verifyCanManage(material);

        // Soft delete, matching students/teachers/classes. The uploaded file is left
        // in place: a soft-deleted row can be restored, and a restored material that
        // pointed at a deleted file would be worse than an orphaned file.
        material.setDeleted(true);
        studyMaterialRepository.save(material);
        auditLogService.record("DELETE_STUDY_MATERIAL", "StudyMaterial", material.getId(), null, null);
    }

    /* ------------------------------------------------------------------ */
    /* Visibility                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * The row filter for the list endpoint, or null when the caller sees everything.
     *
     * <ul>
     *   <li>management/office — everything, drafts included;</li>
     *   <li>teacher — materials for the class/sections they teach, drafts included,
     *       because a teacher needs to find their own unpublished work;</li>
     *   <li>student/parent — published materials addressed to their own class and
     *       either their own section or the whole class.</li>
     * </ul>
     */
    private Specification<StudyMaterial> visibilitySpecification() {
        UserPrincipal principal = requirePrincipal();
        String roleName = principal.getRoleName();

        if (AppConstants.MANAGEMENT_ROLES.contains(roleName)
                || AppConstants.ROLE_RECEPTIONIST.equals(roleName)) {
            return null;
        }

        // NOTE on the LEFT joins below: section is nullable and null carries meaning
        // ("shared with the whole class"). Navigating it implicitly via
        // root.get("section").get("id") would emit an INNER join, which silently drops
        // every class-wide material from the result — including from the OR branch
        // written to include them. The join has to be explicit and LEFT.

        if (AppConstants.TEACHING_ROLES.contains(roleName)) {
            List<Long> sectionIds = taughtSectionIds(principal.getId());
            List<Long> classIds = taughtClassIds(principal.getId());
            if (sectionIds.isEmpty() && classIds.isEmpty()) {
                // Teaches nothing yet: sees nothing, rather than everything.
                return (root, query, cb) -> cb.disjunction();
            }
            return (root, query, cb) -> {
                Join<StudyMaterial, Section> sectionJoin = root.join("section", JoinType.LEFT);
                List<Predicate> allowed = new ArrayList<>();
                if (!sectionIds.isEmpty()) {
                    allowed.add(sectionJoin.get("id").in(sectionIds));
                }
                if (!classIds.isEmpty()) {
                    // Class-wide materials for a class they teach in.
                    allowed.add(cb.and(cb.isNull(sectionJoin.get("id")),
                            root.get("schoolClass").get("id").in(classIds)));
                }
                return cb.or(allowed.toArray(new Predicate[0]));
            };
        }

        if (AppConstants.SELF_SCOPED_ROLES.contains(roleName)) {
            List<Student> students = ownStudents(principal.getId());
            if (students.isEmpty()) {
                return (root, query, cb) -> cb.disjunction();
            }
            return (root, query, cb) -> {
                Join<StudyMaterial, Section> sectionJoin = root.join("section", JoinType.LEFT);
                List<Predicate> perStudent = new ArrayList<>();
                for (Student student : students) {
                    Long ownClassId = student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
                    Long ownSectionId = student.getSection() != null ? student.getSection().getId() : null;
                    if (ownClassId == null) {
                        continue;
                    }
                    Predicate sameClass = cb.equal(root.get("schoolClass").get("id"), ownClassId);
                    Predicate sectionMatches = ownSectionId == null
                            ? cb.isNull(sectionJoin.get("id"))
                            : cb.or(cb.isNull(sectionJoin.get("id")),
                                    cb.equal(sectionJoin.get("id"), ownSectionId));
                    perStudent.add(cb.and(sameClass, sectionMatches));
                }
                if (perStudent.isEmpty()) {
                    return cb.disjunction();
                }
                // Drafts are invisible to students however the rest matches.
                return cb.and(cb.isTrue(root.get("published")), cb.or(perStudent.toArray(new Predicate[0])));
            };
        }

        // Any other role has no business in the materials library.
        return (root, query, cb) -> cb.disjunction();
    }

    /** The by-id equivalent of {@link #visibilitySpecification()}. */
    private void verifyCanView(StudyMaterial material) {
        UserPrincipal principal = requirePrincipal();
        String roleName = principal.getRoleName();

        if (AppConstants.MANAGEMENT_ROLES.contains(roleName)
                || AppConstants.ROLE_RECEPTIONIST.equals(roleName)) {
            return;
        }

        if (AppConstants.TEACHING_ROLES.contains(roleName)) {
            Long sectionId = material.getSection() == null ? null : material.getSection().getId();
            if (sectionId == null
                    ? taughtClassIds(principal.getId()).contains(material.getSchoolClass().getId())
                    : taughtSectionIds(principal.getId()).contains(sectionId)) {
                return;
            }
            throw new AccessDeniedException("This material belongs to a class you do not teach");
        }

        if (AppConstants.SELF_SCOPED_ROLES.contains(roleName)) {
            if (!material.isPublished()) {
                throw new AccessDeniedException("This material has not been published");
            }
            boolean matches = ownStudents(principal.getId()).stream().anyMatch(student -> {
                Long ownClassId = student.getSchoolClass() != null ? student.getSchoolClass().getId() : null;
                Long ownSectionId = student.getSection() != null ? student.getSection().getId() : null;
                if (ownClassId == null || !ownClassId.equals(material.getSchoolClass().getId())) {
                    return false;
                }
                return material.getSection() == null
                        || (ownSectionId != null && ownSectionId.equals(material.getSection().getId()));
            });
            if (matches) {
                return;
            }
            throw new AccessDeniedException("This material was not shared with your class");
        }

        throw new AccessDeniedException("You do not have access to study materials");
    }

    /**
     * Who may edit or delete an existing material: management, or the teacher who
     * uploaded it. A teacher who merely shares the section cannot overwrite a
     * colleague's work.
     */
    private void verifyCanManage(StudyMaterial material) {
        UserPrincipal principal = requirePrincipal();

        if (AppConstants.MANAGEMENT_ROLES.contains(principal.getRoleName())) {
            return;
        }

        Long ownTeacherId = currentTeacher().map(Teacher::getId).orElse(null);
        Long ownerId = material.getTeacher() == null ? null : material.getTeacher().getId();
        if (ownTeacherId != null && ownTeacherId.equals(ownerId)) {
            return;
        }
        throw new AccessDeniedException("You may only manage study materials you uploaded");
    }

    /**
     * Where a caller may publish to. Delegates to SectionAccessGuard for a specific
     * section; a class-wide material (no section) is restricted to management,
     * because "every section of the class" necessarily reaches sections the teacher
     * may not teach.
     */
    private void verifyCanPublishTo(SchoolClass schoolClass, Section section) {
        if (section != null) {
            sectionAccessGuard.verifyCanAccessSection(schoolClass.getId(), section.getId());
            return;
        }

        UserPrincipal principal = requirePrincipal();
        if (!AppConstants.MANAGEMENT_ROLES.contains(principal.getRoleName())) {
            throw new AccessDeniedException(
                    "Only management can share a material with every section of a class; pick a section");
        }
    }

    /* ------------------------------------------------------------------ */
    /* Validation                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Resolves which source the material points at, enforcing the either-or rule.
     * On update, an unchanged file counts as a source, so editing the title of a
     * previously uploaded material does not require re-uploading it.
     */
    private String resolveSource(StudyMaterialRequest request, MultipartFile file, String existingFileUrl) {
        boolean hasNewFile = file != null && !file.isEmpty();
        boolean hasExternalUrl = StringUtils.hasText(request.getExternalUrl());
        boolean hasExistingFile = StringUtils.hasText(existingFileUrl);

        if (hasNewFile && hasExternalUrl) {
            throw new BadRequestException("Provide either a file or an external URL, not both");
        }
        if (!hasNewFile && !hasExternalUrl && !hasExistingFile) {
            throw new BadRequestException("A file or an external URL is required");
        }

        if (hasNewFile) {
            return fileStorageService.storeMaterial(file);
        }
        // Switching an uploaded material to a link drops the file reference; the
        // stored file is cleaned up by the caller once the row is saved.
        return hasExternalUrl ? null : existingFileUrl;
    }

    private void verifySubjectBelongsToClass(Subject subject, SchoolClass schoolClass) {
        if (subject.getSchoolClass() == null || !subject.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new BadRequestException("That subject is not taught in the selected class");
        }
    }

    private void verifySectionBelongsToClass(Section section, SchoolClass schoolClass) {
        if (section == null) {
            return;
        }
        if (section.getSchoolClass() == null || !section.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new BadRequestException("That section does not belong to the selected class");
        }
    }

    private MaterialType parseType(String raw) {
        try {
            return MaterialType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown material type: " + raw);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Lookups                                                             */
    /* ------------------------------------------------------------------ */

    /** Sections the teacher reaches by either grant: homeroom or subject mapping. */
    private List<Long> taughtSectionIds(Long userId) {
        return teacherRepository.findByUserId(userId)
                .map(teacher -> union(
                        sectionRepository.findIdsByClassTeacherId(teacher.getId()),
                        sectionRepository.findIdsTaughtByTeacherId(teacher.getId())))
                .orElseGet(List::of);
    }

    /** Classes the teacher reaches by either grant. */
    private List<Long> taughtClassIds(Long userId) {
        return teacherRepository.findByUserId(userId)
                .map(teacher -> union(
                        sectionRepository.findClassIdsByClassTeacherId(teacher.getId()),
                        sectionRepository.findClassIdsTaughtByTeacherId(teacher.getId())))
                .orElseGet(List::of);
    }

    private List<Long> union(List<Long> first, List<Long> second) {
        return new ArrayList<>(new LinkedHashSet<>(Stream.concat(first.stream(), second.stream()).toList()));
    }

    private List<Student> ownStudents(Long userId) {
        return studentRepository.findByUserId(userId)
                .map(List::of)
                .orElseGet(() -> studentRepository.findAllByParentUserId(userId));
    }

    private Optional<Teacher> currentTeacher() {
        return teacherRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    private UserPrincipal requirePrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));
    }

    private StudyMaterial findEntity(Long id) {
        return studyMaterialRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("StudyMaterial", "id", id));
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

    private StudyMaterialDto toDto(StudyMaterial material) {
        Teacher teacher = material.getTeacher();
        User teacherUser = teacher != null ? teacher.getUser() : null;

        return StudyMaterialDto.builder()
                .id(material.getId())
                .classId(material.getSchoolClass().getId())
                .className(material.getSchoolClass().getClassName())
                .sectionId(material.getSection() != null ? material.getSection().getId() : null)
                .sectionName(material.getSection() != null ? material.getSection().getSectionName() : null)
                .subjectId(material.getSubject().getId())
                .subjectName(material.getSubject().getSubjectName())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacherUser != null
                        ? NameUtil.fullName(teacherUser.getFirstName(), teacherUser.getLastName())
                        : null)
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType().name())
                .fileUrl(material.getFileUrl())
                .externalUrl(material.getExternalUrl())
                .published(material.isPublished())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .canManage(canManageQuietly(material))
                .build();
    }

    /** The {@link #verifyCanManage} rule as a boolean, for the DTO's canManage flag. */
    private boolean canManageQuietly(StudyMaterial material) {
        try {
            verifyCanManage(material);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }
}
