package com.school.sms.service.impl;

import com.school.sms.dto.request.SectionRequest;
import com.school.sms.dto.response.SectionDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.exception.BadRequestException;
import com.school.sms.mapper.SchoolClassMapper;
import com.school.sms.mapper.SectionMapper;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.ClassOfficialRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.UserService;
import com.school.sms.util.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * One section per class, named "A".
 *
 * <p>The rule lives in the service rather than in a CHECK constraint so it can be
 * lifted by changing one class if the school ever streams a year again. That makes
 * it worth pinning down here: nothing in the schema would stop a second section
 * from being inserted, so these tests are the only thing standing between the
 * single-section assumption and a UI that quietly hides the extra section it
 * cannot show.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SingleSectionRuleTest {

    private static final long CLASS_ID = 7L;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SectionMapper sectionMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setClassName("Class 5");
        schoolClass.setDeleted(false);

        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));
        when(sectionMapper.toEntity(any(SectionRequest.class))).thenAnswer(invocation -> {
            SectionRequest request = invocation.getArgument(0);
            Section entity = new Section();
            entity.setSectionName(request.getSectionName());
            return entity;
        });
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        // The service maps the saved entity on the way out; only the happy-path
        // tests reach it, and they care that a save happened, not what it returned.
        when(sectionMapper.toDto(any(Section.class))).thenReturn(new SectionDto());
    }

    private SectionRequest request(String name) {
        SectionRequest request = new SectionRequest();
        request.setSectionName(name);
        return request;
    }

    private Section existingA() {
        Section existing = new Section();
        existing.setId(70L);
        existing.setSectionName("A");
        existing.setSchoolClass(schoolClass);
        return existing;
    }

    @Nested
    @DisplayName("creating a section")
    class Creating {

        @Test
        @DisplayName("refuses a name other than A, rather than quietly storing it as A")
        void refusesNonAName() {
            when(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(CLASS_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> sectionService.create(CLASS_ID, request("B")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("single section per class");

            verify(sectionRepository, never()).save(any(Section.class));
        }

        @Test
        @DisplayName("refuses a second section even when it is also called A")
        void refusesSecondSection() {
            // The duplicate-name check catches this one first; either way nothing is
            // written, which is what the caller cares about.
            when(sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(CLASS_ID, "A")).thenReturn(true);
            when(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(CLASS_ID))
                    .thenReturn(List.of(existingA()));

            assertThatThrownBy(() -> sectionService.create(CLASS_ID, request("A")))
                    .isInstanceOf(RuntimeException.class);

            verify(sectionRepository, never()).save(any(Section.class));
        }

        @Test
        @DisplayName("refuses a differently-named second section with the single-section message")
        void refusesSecondSectionUnderAnotherName() {
            when(sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(CLASS_ID, "C")).thenReturn(false);
            when(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(CLASS_ID))
                    .thenReturn(List.of(existingA()));

            assertThatThrownBy(() -> sectionService.create(CLASS_ID, request("C")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("single section per class");
        }

        @Test
        @DisplayName("accepts A for a class that has none, since a class must have one")
        void acceptsFirstA() {
            when(sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(CLASS_ID, "A")).thenReturn(false);
            when(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(CLASS_ID)).thenReturn(List.of());
            when(studentRepository.countBySectionIdAndDeletedFalseAndStatus(any(), any())).thenReturn(0L);

            sectionService.create(CLASS_ID, request("A"));

            verify(sectionRepository).save(any(Section.class));
        }

        @Test
        @DisplayName("is case-insensitive about the name, so \"a\" is not a second section")
        void acceptsLowercaseA() {
            when(sectionRepository.existsBySchoolClassIdAndSectionNameIgnoreCase(CLASS_ID, "a")).thenReturn(false);
            when(sectionRepository.findAllBySchoolClassIdOrderBySectionNameAsc(CLASS_ID)).thenReturn(List.of());
            when(studentRepository.countBySectionIdAndDeletedFalseAndStatus(any(), any())).thenReturn(0L);

            sectionService.create(CLASS_ID, request("a"));

            verify(sectionRepository).save(any(Section.class));
        }
    }

    @Nested
    @DisplayName("creating a class")
    class ClassCreation {

        @Mock
        private SchoolClassMapper schoolClassMapper;

        @Mock
        private AcademicYearRepository academicYearRepository;

        @Mock
        private SubjectRepository subjectRepository;

        @Mock
        private ClassOfficialRepository classOfficialRepository;

        @Test
        @DisplayName("creates the class's one section with it, so students can be enrolled straight away")
        void createsSectionWithClass() {
            SchoolClassServiceImpl classService = new SchoolClassServiceImpl(
                    schoolClassRepository,
                    academicYearRepository,
                    sectionRepository,
                    subjectRepository,
                    studentRepository,
                    classOfficialRepository,
                    schoolClassMapper);

            AcademicYear year = new AcademicYear();
            year.setId(1L);
            com.school.sms.dto.request.SchoolClassRequest request = new com.school.sms.dto.request.SchoolClassRequest();
            request.setClassName("Class 6");
            request.setAcademicYearId(1L);

            when(schoolClassRepository.existsByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse("Class 6", 1L))
                    .thenReturn(false);
            when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
            when(schoolClassMapper.toEntity(request)).thenReturn(new SchoolClass());
            when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> {
                SchoolClass saved = invocation.getArgument(0);
                saved.setId(CLASS_ID);
                return saved;
            });

            classService.create(request);

            ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
            verify(sectionRepository).save(captor.capture());
            assertThat(captor.getValue().getSectionName()).isEqualTo(AppConstants.SINGLE_SECTION_NAME);
            assertThat(captor.getValue().getSchoolClass().getId()).isEqualTo(CLASS_ID);
        }
    }
}
