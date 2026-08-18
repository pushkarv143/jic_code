package com.school.sms.service.impl;

import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.entity.ClassOfficial;
import com.school.sms.entity.ClassOfficialRole;
import com.school.sms.entity.Gender;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Appointing a class official.
 *
 * <p>Three rules carry the weight here. A post belongs to the class it is a post
 * of, so a student from another class cannot hold it — the foreign keys alone
 * would happily allow that. HEAD_BOY/HEAD_GIRL are gender-checked, because
 * picking the wrong row out of a long student list is the common mistake. And
 * appointing over a sitting holder is a succession rather than an overwrite: the
 * outgoing tenure is closed so the history survives and the database's
 * one-current-holder constraint is satisfied before the new row is written.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassOfficialAppointmentTest {

    private static final long CLASS_ID = 7L;

    @Mock
    private ClassOfficialRepository classOfficialRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ClassOfficialServiceImpl service;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setClassName("Class 10");
        schoolClass.setDeleted(false);
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

        when(classOfficialRepository.save(any(ClassOfficial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(classOfficialRepository.saveAndFlush(any(ClassOfficial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Student student(long id, Gender gender, Long classId, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setGender(gender);
        student.setStatus(status);
        student.setDeleted(false);
        if (classId != null) {
            SchoolClass owner = new SchoolClass();
            owner.setId(classId);
            student.setSchoolClass(owner);
        }
        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        return student;
    }

    private ClassOfficialRequest request(long studentId, ClassOfficialRole role, LocalDate from) {
        ClassOfficialRequest request = new ClassOfficialRequest();
        request.setStudentId(studentId);
        request.setRole(role);
        request.setFromDate(from);
        return request;
    }

    /* ---- the post belongs to the class ----------------------------------- */

    @Test
    void appointsAStudentOfThisClass() {
        student(1L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        ClassOfficialDto dto = service.appoint(CLASS_ID, request(1L, ClassOfficialRole.HEAD_BOY, LocalDate.of(2026, 4, 1)));

        assertThat(dto.getRole()).isEqualTo("HEAD_BOY");
        assertThat(dto.getStudentId()).isEqualTo(1L);
        assertThat(dto.isCurrent()).isTrue();
        assertThat(dto.getToDate()).isNull();
    }

    @Test
    void rejectsAStudentFromAnotherClass() {
        student(2L, Gender.MALE, 99L, StudentStatus.ACTIVE);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(2L, ClassOfficialRole.HEAD_BOY, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not enrolled in this class");
        verify(classOfficialRepository, never()).save(any());
    }

    @Test
    void rejectsAStudentWhoIsNoLongerActive() {
        student(3L, Gender.MALE, CLASS_ID, StudentStatus.INACTIVE);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(3L, ClassOfficialRole.HEAD_BOY, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active student");
        verify(classOfficialRepository, never()).save(any());
    }

    /* ---- gender rules ----------------------------------------------------- */

    @Test
    void rejectsAFemaleStudentAsHeadBoy() {
        student(4L, Gender.FEMALE, CLASS_ID, StudentStatus.ACTIVE);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(4L, ClassOfficialRole.HEAD_BOY, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be a male student");
    }

    @Test
    void rejectsAMaleStudentAsHeadGirl() {
        student(5L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(5L, ClassOfficialRole.HEAD_GIRL, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be a female student");
    }

    @Test
    void rejectsAStudentWithNoGenderRecordedForAGenderedPost() {
        student(6L, null, CLASS_ID, StudentStatus.ACTIVE);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(6L, ClassOfficialRole.HEAD_BOY, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no gender recorded");
    }

    /** Monitor and the rest are open to anyone, so no gender check applies. */
    @Test
    void allowsEitherGenderForAnUngenderedPost() {
        student(7L, Gender.FEMALE, CLASS_ID, StudentStatus.ACTIVE);

        assertThat(service.appoint(CLASS_ID, request(7L, ClassOfficialRole.MONITOR, null)).getRole())
                .isEqualTo("MONITOR");
    }

    /* ---- succession ------------------------------------------------------- */

    @Test
    void endsTheSittingHoldersTenureTheDayBeforeTheSuccessorStarts() {
        Student outgoingStudent = student(8L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);
        student(9L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        ClassOfficial sitting = ClassOfficial.builder()
                .schoolClass(schoolClass)
                .student(outgoingStudent)
                .role(ClassOfficialRole.HEAD_BOY)
                .fromDate(LocalDate.of(2026, 4, 1))
                .build();
        when(classOfficialRepository.findBySchoolClassIdAndRoleAndToDateIsNull(CLASS_ID, ClassOfficialRole.HEAD_BOY))
                .thenReturn(Optional.of(sitting));

        service.appoint(CLASS_ID, request(9L, ClassOfficialRole.HEAD_BOY, LocalDate.of(2026, 8, 15)));

        ArgumentCaptor<ClassOfficial> captor = ArgumentCaptor.forClass(ClassOfficial.class);
        verify(classOfficialRepository).saveAndFlush(captor.capture());
        // The day before, not the same day: two tenures must not both cover 15 Aug.
        assertThat(captor.getValue().getToDate()).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    /**
     * A handover dated on the sitting holder's own start date would otherwise end
     * their tenure the day before it began.
     */
    @Test
    void neverEndsATenureBeforeItStarted() {
        Student outgoingStudent = student(10L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);
        student(11L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        LocalDate sameDay = LocalDate.of(2026, 4, 1);
        ClassOfficial sitting = ClassOfficial.builder()
                .schoolClass(schoolClass)
                .student(outgoingStudent)
                .role(ClassOfficialRole.HEAD_BOY)
                .fromDate(sameDay)
                .build();
        when(classOfficialRepository.findBySchoolClassIdAndRoleAndToDateIsNull(CLASS_ID, ClassOfficialRole.HEAD_BOY))
                .thenReturn(Optional.of(sitting));

        service.appoint(CLASS_ID, request(11L, ClassOfficialRole.HEAD_BOY, sameDay));

        ArgumentCaptor<ClassOfficial> captor = ArgumentCaptor.forClass(ClassOfficial.class);
        verify(classOfficialRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getToDate()).isEqualTo(sameDay);
    }

    @Test
    void rejectsReappointingTheSittingHolder() {
        Student sittingStudent = student(12L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        ClassOfficial sitting = ClassOfficial.builder()
                .schoolClass(schoolClass)
                .student(sittingStudent)
                .role(ClassOfficialRole.HEAD_BOY)
                .fromDate(LocalDate.of(2026, 4, 1))
                .build();
        when(classOfficialRepository.findBySchoolClassIdAndRoleAndToDateIsNull(CLASS_ID, ClassOfficialRole.HEAD_BOY))
                .thenReturn(Optional.of(sitting));
        // The sitting holder is, by definition, a live post of that student, so the
        // one-post-per-student check sees it and rejects first. Stubbed here because
        // that check now owns this rejection — the succession branch no longer
        // repeats it.
        when(classOfficialRepository.findAllByStudentIdAndToDateIsNull(12L)).thenReturn(List.of(sitting));

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request(12L, ClassOfficialRole.HEAD_BOY, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already holds this post");
        verify(classOfficialRepository, never()).save(any());
    }

    /* ---- section-scoped posts --------------------------------------------- */

    @Test
    void rejectsASectionThatBelongsToAnotherClass() {
        student(13L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE);

        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(99L);
        Section foreignSection = new Section();
        foreignSection.setId(500L);
        foreignSection.setSchoolClass(otherClass);
        when(sectionRepository.findById(500L)).thenReturn(Optional.of(foreignSection));

        ClassOfficialRequest request = request(13L, ClassOfficialRole.MONITOR, null);
        request.setSectionId(500L);

        assertThatThrownBy(() -> service.appoint(CLASS_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("section does not belong to this class");
    }

    /* ---- ending a tenure --------------------------------------------------- */

    @Test
    void rejectsEndingAnAppointmentThatHasAlreadyEnded() {
        ClassOfficial ended = ClassOfficial.builder()
                .schoolClass(schoolClass)
                .student(student(14L, Gender.MALE, CLASS_ID, StudentStatus.ACTIVE))
                .role(ClassOfficialRole.HEAD_BOY)
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 6, 1))
                .build();
        when(classOfficialRepository.findById(77L)).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> service.end(CLASS_ID, 77L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already ended");
    }

    @Test
    void rejectsEndingAnAppointmentBelongingToAnotherClass() {
        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(99L);
        ClassOfficial foreign = ClassOfficial.builder()
                .schoolClass(otherClass)
                .student(student(15L, Gender.MALE, 99L, StudentStatus.ACTIVE))
                .role(ClassOfficialRole.HEAD_BOY)
                .fromDate(LocalDate.of(2026, 4, 1))
                .build();
        when(classOfficialRepository.findById(78L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.end(CLASS_ID, 78L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to this class");
    }
}
