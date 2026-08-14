package com.school.sms.service.impl;

import com.school.sms.entity.Role;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudyMaterial;
import com.school.sms.entity.Subject;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.StudyMaterialRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.security.SectionAccessGuard;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Who may see which study material.
 *
 * Two rules interact and both have to hold: a material is addressed to a class and
 * optionally a section (null meaning "the whole class"), and a draft is invisible to
 * students however well the addressing matches. These tests drive the by-id path
 * ({@code getById}); the list path applies the same rules as a Specification, which
 * needs a database to exercise.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyMaterialVisibilityTest {

    private static final long CLASS_ID = 5L;
    private static final long OTHER_CLASS_ID = 6L;
    private static final long SECTION_A = 50L;
    private static final long SECTION_B = 51L;
    private static final long TEACHER_ID = 70L;
    private static final long OTHER_TEACHER_ID = 71L;

    @Mock private StudyMaterialRepository studyMaterialRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private SectionAccessGuard sectionAccessGuard;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private StudyMaterialServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /* --------------------------------------------------------------- */
    /* Students: own class/section, published only.                     */
    /* --------------------------------------------------------------- */

    @Test
    void studentSeesAMaterialForTheirOwnSection() {
        signIn(5L, "STUDENT");
        givenStudent(5L, CLASS_ID, SECTION_A);
        StudyMaterial material = material(1L, CLASS_ID, SECTION_A, true, TEACHER_ID);

        assertThatCode(() -> service.getById(1L)).doesNotThrowAnyException();
    }

    /** A class-wide material (null section) reaches every section of that class. */
    @Test
    void studentSeesAClassWideMaterial() {
        signIn(5L, "STUDENT");
        givenStudent(5L, CLASS_ID, SECTION_A);
        material(2L, CLASS_ID, null, true, TEACHER_ID);

        assertThatCode(() -> service.getById(2L)).doesNotThrowAnyException();
    }

    @Test
    void studentIsRefusedAMaterialForAnotherSection() {
        signIn(5L, "STUDENT");
        givenStudent(5L, CLASS_ID, SECTION_A);
        material(3L, CLASS_ID, SECTION_B, true, TEACHER_ID);

        assertThatThrownBy(() -> service.getById(3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not shared with your class");
    }

    @Test
    void studentIsRefusedAMaterialForAnotherClass() {
        signIn(5L, "STUDENT");
        givenStudent(5L, CLASS_ID, SECTION_A);
        material(4L, OTHER_CLASS_ID, null, true, TEACHER_ID);

        assertThatThrownBy(() -> service.getById(4L))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * The draft rule is independent of the addressing rule: a material aimed
     * squarely at the student's own section is still invisible until published.
     */
    @Test
    void studentIsRefusedAnUnpublishedMaterialForTheirOwnSection() {
        signIn(5L, "STUDENT");
        givenStudent(5L, CLASS_ID, SECTION_A);
        material(5L, CLASS_ID, SECTION_A, false, TEACHER_ID);

        assertThatThrownBy(() -> service.getById(5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not been published");
    }

    @Test
    void parentSeesWhatTheirChildSees() {
        signIn(6L, "PARENT");
        when(studentRepository.findByUserId(6L)).thenReturn(Optional.empty());
        when(studentRepository.findAllByParentUserId(6L))
                .thenReturn(List.of(student(CLASS_ID, SECTION_A)));
        material(6L, CLASS_ID, SECTION_A, true, TEACHER_ID);

        assertThatCode(() -> service.getById(6L)).doesNotThrowAnyException();
    }

    /* --------------------------------------------------------------- */
    /* Teachers: the classes they teach, drafts included.               */
    /* --------------------------------------------------------------- */

    @Test
    void teacherSeesAMaterialForASectionTheyTeach() {
        signInTeacher(7L, TEACHER_ID);
        when(sectionRepository.findIdsByClassTeacherId(TEACHER_ID)).thenReturn(List.of());
        when(sectionRepository.findIdsTaughtByTeacherId(TEACHER_ID)).thenReturn(List.of(SECTION_A));
        material(7L, CLASS_ID, SECTION_A, true, TEACHER_ID);

        assertThatCode(() -> service.getById(7L)).doesNotThrowAnyException();
    }

    /** A teacher needs to find their own unpublished work, so drafts stay visible. */
    @Test
    void teacherSeesTheirOwnDraft() {
        signInTeacher(7L, TEACHER_ID);
        when(sectionRepository.findIdsByClassTeacherId(TEACHER_ID)).thenReturn(List.of());
        when(sectionRepository.findIdsTaughtByTeacherId(TEACHER_ID)).thenReturn(List.of(SECTION_A));
        material(8L, CLASS_ID, SECTION_A, false, TEACHER_ID);

        assertThatCode(() -> service.getById(8L)).doesNotThrowAnyException();
    }

    @Test
    void teacherIsRefusedAMaterialForASectionTheyDoNotTeach() {
        signInTeacher(7L, TEACHER_ID);
        when(sectionRepository.findIdsByClassTeacherId(TEACHER_ID)).thenReturn(List.of());
        when(sectionRepository.findIdsTaughtByTeacherId(TEACHER_ID)).thenReturn(List.of(SECTION_A));
        material(9L, CLASS_ID, SECTION_B, true, TEACHER_ID);

        assertThatThrownBy(() -> service.getById(9L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("class you do not teach");
    }

    /* --------------------------------------------------------------- */
    /* Ownership on write.                                              */
    /* --------------------------------------------------------------- */

    /**
     * Sharing a section is not the same as owning a colleague's upload: a teacher who
     * teaches the section may read the material but must not be able to overwrite or
     * delete it.
     */
    @Test
    void teacherCannotDeleteAColleaguesMaterialInTheirOwnSection() {
        signInTeacher(7L, TEACHER_ID);
        when(sectionRepository.findIdsByClassTeacherId(TEACHER_ID)).thenReturn(List.of());
        when(sectionRepository.findIdsTaughtByTeacherId(TEACHER_ID)).thenReturn(List.of(SECTION_A));
        material(10L, CLASS_ID, SECTION_A, true, OTHER_TEACHER_ID);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only manage study materials you uploaded");
    }

    @Test
    void teacherCanDeleteTheirOwnMaterial() {
        signInTeacher(7L, TEACHER_ID);
        StudyMaterial material = material(11L, CLASS_ID, SECTION_A, true, TEACHER_ID);
        when(studyMaterialRepository.save(material)).thenReturn(material);

        assertThatCode(() -> service.delete(11L)).doesNotThrowAnyException();
        assertTrue(material.isDeleted(), "delete should soft-delete rather than remove the row");
    }

    @Test
    void managementCanDeleteAnyMaterial() {
        signIn(1L, "PRINCIPAL");
        StudyMaterial material = material(12L, CLASS_ID, SECTION_A, true, OTHER_TEACHER_ID);
        when(studyMaterialRepository.save(material)).thenReturn(material);

        assertThatCode(() -> service.delete(12L)).doesNotThrowAnyException();
    }

    /* --------------------------------------------------------------- */
    /* The canManage flag on the DTO.                                   */
    /* --------------------------------------------------------------- */

    @Test
    void canManageIsTrueOnlyForTheUploader() {
        signInTeacher(7L, TEACHER_ID);
        when(sectionRepository.findIdsByClassTeacherId(TEACHER_ID)).thenReturn(List.of());
        when(sectionRepository.findIdsTaughtByTeacherId(TEACHER_ID)).thenReturn(List.of(SECTION_A));

        material(13L, CLASS_ID, SECTION_A, true, TEACHER_ID);
        assertTrue(service.getById(13L).isCanManage());

        material(14L, CLASS_ID, SECTION_A, true, OTHER_TEACHER_ID);
        assertFalse(service.getById(14L).isCanManage());
    }

    /* --------------------------------------------------------------- */

    private void signIn(Long userId, String roleName) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setEmail("user" + userId + "@school.edu");
        user.setPassword("hash");
        user.setActive(true);
        user.setRole(Role.builder().id(1L).name(roleName).build());

        UserPrincipal principal = UserPrincipal.create(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void signInTeacher(Long userId, Long teacherId) {
        signIn(userId, "TEACHER");
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));
    }

    private void givenStudent(Long userId, Long classId, Long sectionId) {
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student(classId, sectionId)));
    }

    private Student student(Long classId, Long sectionId) {
        SchoolClass cls = new SchoolClass();
        cls.setId(classId);
        Section sec = new Section();
        sec.setId(sectionId);

        Student student = new Student();
        student.setId(100L);
        student.setSchoolClass(cls);
        student.setSection(sec);
        return student;
    }

    private StudyMaterial material(Long id, Long classId, Long sectionId, boolean published, Long teacherId) {
        SchoolClass cls = new SchoolClass();
        cls.setId(classId);
        cls.setClassName("Class " + classId);

        Section section = null;
        if (sectionId != null) {
            section = new Section();
            section.setId(sectionId);
            section.setSectionName("S" + sectionId);
        }

        Subject subject = new Subject();
        subject.setId(900L);
        subject.setSubjectName("Mathematics");

        Teacher teacher = new Teacher();
        teacher.setId(teacherId);

        StudyMaterial material = StudyMaterial.builder()
                .schoolClass(cls)
                .section(section)
                .subject(subject)
                .teacher(teacher)
                .title("Material " + id)
                .materialType(com.school.sms.entity.MaterialType.NOTES)
                .externalUrl("https://example.edu/m/" + id)
                .published(published)
                .deleted(false)
                .build();
        material.setId(id);

        when(studyMaterialRepository.findById(id)).thenReturn(Optional.of(material));
        return material;
    }
}
