package com.school.sms.service.impl;

import com.school.sms.dto.request.SaveTimetableRequest;
import com.school.sms.dto.request.TimetableSlotRequest;
import com.school.sms.dto.response.TimetableSlotDto;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Subject;
import com.school.sms.entity.TimetableSlot;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.SubjectRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.TimetableSlotRepository;
import com.school.sms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saving a section's week.
 *
 * <p>The save replaces the whole week rather than diffing it, because a grid edit
 * usually moves several periods at once and an in-place reconcile would have to
 * order its writes so no intermediate state violates the (section, day, period)
 * unique key. That makes the delete-then-flush-then-insert ordering load-bearing,
 * so it is asserted here rather than left to chance.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimetableSaveTest {

    private static final long CLASS_ID = 3L;
    private static final long SECTION_ID = 30L;

    @Mock
    private TimetableSlotRepository timetableSlotRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TimetableServiceImpl service;

    private SchoolClass schoolClass;
    private Section section;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setClassName("Class 5");
        schoolClass.setDeleted(false);

        section = new Section();
        section.setId(SECTION_ID);
        section.setSectionName("A");
        section.setSchoolClass(schoolClass);
        section.setRoomNumber("Home-5A");

        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        when(timetableSlotRepository.save(any(TimetableSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(timetableSlotRepository.findTeacherClashes()).thenReturn(List.of());
        when(timetableSlotRepository.findRoomClashes()).thenReturn(List.of());
    }

    private TimetableSlotRequest slot(DayOfWeek day, int period, String start, String end) {
        TimetableSlotRequest request = new TimetableSlotRequest();
        request.setDayOfWeek(day);
        request.setPeriodNumber(period);
        request.setStartTime(LocalTime.parse(start));
        request.setEndTime(LocalTime.parse(end));
        return request;
    }

    private SaveTimetableRequest requestOf(TimetableSlotRequest... slots) {
        SaveTimetableRequest request = new SaveTimetableRequest();
        request.setSlots(List.of(slots));
        return request;
    }

    /**
     * Without the flush between them Hibernate is free to run the inserts before
     * the delete, tripping the very unique key the replace-the-week approach
     * exists to avoid.
     */
    @Test
    void deletesTheExistingWeekAndFlushesBeforeInserting() {
        service.saveForSection(CLASS_ID, SECTION_ID,
                requestOf(slot(DayOfWeek.MONDAY, 1, "09:00", "09:40")));

        InOrder order = inOrder(timetableSlotRepository);
        order.verify(timetableSlotRepository).deleteAllBySectionId(SECTION_ID);
        order.verify(timetableSlotRepository).flush();
        order.verify(timetableSlotRepository).save(any(TimetableSlot.class));
    }

    @Test
    void anEmptySlotListClearsTheWeek() {
        SaveTimetableRequest request = new SaveTimetableRequest();
        request.setSlots(List.of());

        assertThat(service.saveForSection(CLASS_ID, SECTION_ID, request)).isEmpty();
        verify(timetableSlotRepository).deleteAllBySectionId(SECTION_ID);
        verify(timetableSlotRepository, never()).save(any());
    }

    @Test
    void rejectsTheSameDayAndPeriodTwice() {
        assertThatThrownBy(() -> service.saveForSection(CLASS_ID, SECTION_ID, requestOf(
                slot(DayOfWeek.MONDAY, 1, "09:00", "09:40"),
                slot(DayOfWeek.MONDAY, 1, "10:00", "10:40"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("listed twice");
        // Rejected before anything is written, so the existing week survives.
        verify(timetableSlotRepository, never()).deleteAllBySectionId(any());
    }

    @Test
    void rejectsAPeriodThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> service.saveForSection(CLASS_ID, SECTION_ID,
                requestOf(slot(DayOfWeek.MONDAY, 1, "10:00", "09:00"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ends before it starts");
    }

    @Test
    void rejectsAZeroLengthPeriod() {
        assertThatThrownBy(() -> service.saveForSection(CLASS_ID, SECTION_ID,
                requestOf(slot(DayOfWeek.MONDAY, 1, "09:00", "09:00"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ends before it starts");
    }

    @Test
    void rejectsASubjectBelongingToAnotherClass() {
        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(99L);
        Subject foreign = new Subject();
        foreign.setId(200L);
        foreign.setSubjectName("Chemistry");
        foreign.setSchoolClass(otherClass);
        foreign.setDeleted(false);
        when(subjectRepository.findById(200L)).thenReturn(Optional.of(foreign));

        TimetableSlotRequest item = slot(DayOfWeek.MONDAY, 1, "09:00", "09:40");
        item.setSubjectId(200L);

        assertThatThrownBy(() -> service.saveForSection(CLASS_ID, SECTION_ID, requestOf(item)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to this class");
    }

    @Test
    void rejectsASectionBelongingToAnotherClass() {
        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(99L);
        Section foreignSection = new Section();
        foreignSection.setId(500L);
        foreignSection.setSchoolClass(otherClass);
        when(sectionRepository.findById(500L)).thenReturn(Optional.of(foreignSection));

        assertThatThrownBy(() -> service.saveForSection(CLASS_ID, 500L,
                requestOf(slot(DayOfWeek.MONDAY, 1, "09:00", "09:40"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to this class");
    }

    /** A period with no room of its own sits in the section's home room. */
    @Test
    void fallsBackToTheSectionsHomeRoomWhenNoRoomIsGiven() {
        List<TimetableSlotDto> saved = service.saveForSection(CLASS_ID, SECTION_ID,
                requestOf(slot(DayOfWeek.MONDAY, 1, "09:00", "09:40")));

        assertThat(saved).singleElement()
                .satisfies(dto -> assertThat(dto.getRoomNumber()).isEqualTo("Home-5A"));
    }

    @Test
    void keepsAnExplicitRoomOverTheSectionsHomeRoom() {
        TimetableSlotRequest item = slot(DayOfWeek.MONDAY, 1, "09:00", "09:40");
        item.setRoomNumber("Lab-2");

        assertThat(service.saveForSection(CLASS_ID, SECTION_ID, requestOf(item)))
                .singleElement()
                .satisfies(dto -> assertThat(dto.getRoomNumber()).isEqualTo("Lab-2"));
    }

    /* ---- clash reporting --------------------------------------------------- */

    /**
     * Clashes span sections, so a timetable built one section at a time is
     * legitimately in conflict until the rest are filled in. Rejecting the save
     * would make the grid impossible to complete in any order — so the slot is
     * saved and flagged instead.
     */
    @Test
    void reportsATeacherClashWithoutRefusingTheSave() {
        when(timetableSlotRepository.findTeacherClashes())
                .thenReturn(List.<Object[]>of(new Object[]{42L, DayOfWeek.MONDAY, 1, 2L}));

        com.school.sms.entity.Teacher teacher = new com.school.sms.entity.Teacher();
        teacher.setId(42L);
        when(teacherRepository.findById(42L)).thenReturn(Optional.of(teacher));

        TimetableSlotRequest item = slot(DayOfWeek.MONDAY, 1, "09:00", "09:40");
        item.setTeacherId(42L);

        assertThat(service.saveForSection(CLASS_ID, SECTION_ID, requestOf(item)))
                .singleElement()
                .satisfies(dto -> assertThat(dto.getClashWarning())
                        .contains("booked for another section"));
    }

    @Test
    void reportsARoomClash() {
        when(timetableSlotRepository.findRoomClashes())
                .thenReturn(List.<Object[]>of(new Object[]{"Lab-2", DayOfWeek.MONDAY, 1, 2L}));

        TimetableSlotRequest item = slot(DayOfWeek.MONDAY, 1, "09:00", "09:40");
        item.setRoomNumber("Lab-2");

        assertThat(service.saveForSection(CLASS_ID, SECTION_ID, requestOf(item)))
                .singleElement()
                .satisfies(dto -> assertThat(dto.getClashWarning()).contains("Lab-2"));
    }

    @Test
    void leavesAnUncontestedSlotUnflagged() {
        assertThat(service.saveForSection(CLASS_ID, SECTION_ID,
                requestOf(slot(DayOfWeek.MONDAY, 1, "09:00", "09:40"))))
                .singleElement()
                .satisfies(dto -> assertThat(dto.getClashWarning()).isNull());
    }
}
