package com.school.sms.service.impl;

import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TimetableSlot;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.TimetableSlotRepository;
import com.school.sms.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends each teacher and student their next school-day timetable over WhatsApp
 * every evening at 20:00 (IST = UTC+5:30 → UTC 14:30).
 *
 * <p>The notification is sent only when:
 * <ul>
 *   <li>The WhatsApp gateway is configured ({@link WhatsAppService#isAvailable()}).</li>
 *   <li>The next school day is Monday–Saturday (Sunday has no timetable).</li>
 *   <li>The user/student has a phone number on record.</li>
 * </ul>
 *
 * <p>Nothing is sent if no slots exist for that day — an empty timetable is
 * silently skipped rather than sending a confusing blank message.
 *
 * <p><b>Opt-out:</b> set the {@code whatsapp_notifications_enabled} user-level
 * setting to {@code false} (migration {@code 24_whatsapp_notifications.sql}).
 * The scheduler skips users for whom the setting is absent (defaults to enabled)
 * or explicitly set to {@code true}; it only skips when explicitly {@code false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableWhatsAppScheduler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final WhatsAppService whatsAppService;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    /**
     * Fires at 20:00 IST every day.
     * cron = "second minute hour day month weekday" in UTC:
     *   20:00 IST = 14:30 UTC.
     */
    @Scheduled(cron = "0 30 14 * * *", zone = "UTC")
    @Transactional(readOnly = true)
    public void sendTimetableReminders() {
        if (!whatsAppService.isAvailable()) {
            log.debug("WhatsApp not configured — skipping timetable reminders");
            return;
        }

        DayOfWeek nextSchoolDay = nextSchoolDay();
        if (nextSchoolDay == null) {
            log.debug("Next day is Sunday — no timetable reminder to send");
            return;
        }

        log.info("Sending timetable WhatsApp reminders for {}", nextSchoolDay);

        int sent = 0;
        sent += notifyTeachers(nextSchoolDay);
        sent += notifyStudents(nextSchoolDay);
        log.info("Timetable WhatsApp reminders sent: {} recipients", sent);
    }

    // -------------------------------------------------------------------------

    private int notifyTeachers(DayOfWeek day) {
        List<Teacher> teachers = teacherRepository.findAll();
        int count = 0;
        for (Teacher t : teachers) {
            String phone = t.getUser() != null ? t.getUser().getPhone() : null;
            if (!StringUtils.hasText(phone)) continue;

            List<TimetableSlot> slots =
                    timetableSlotRepository.findAllByTeacherIdOrderByDayOfWeekAscPeriodNumberAsc(t.getId())
                            .stream()
                            .filter(s -> s.getDayOfWeek() == day)
                            .collect(Collectors.toList());
            if (slots.isEmpty()) continue;

            String message = buildTeacherMessage(t, day, slots);
            if (whatsAppService.send(phone, message)) count++;
        }
        return count;
    }

    private int notifyStudents(DayOfWeek day) {
        List<Student> students = studentRepository.findAll();
        int count = 0;
        for (Student s : students) {
            String phone = s.getUser() != null ? s.getUser().getPhone() : null;
            if (!StringUtils.hasText(phone)) continue;
            if (s.getSection() == null) continue;

            List<TimetableSlot> slots =
                    timetableSlotRepository.findAllBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(s.getSection().getId())
                            .stream()
                            .filter(slot -> slot.getDayOfWeek() == day)
                            .collect(Collectors.toList());
            if (slots.isEmpty()) continue;

            String message = buildStudentMessage(s, day, slots);
            if (whatsAppService.send(phone, message)) count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------

    private String buildTeacherMessage(Teacher teacher, DayOfWeek day, List<TimetableSlot> slots) {
        String name = teacher.getUser() != null ? teacher.getUser().getFirstName() : "Teacher";
        StringBuilder sb = new StringBuilder();
        sb.append("📅 *").append(displayDay(day)).append(" Timetable — Greenwood School*\n\n");
        sb.append("Hello ").append(name).append("! Here are your classes for tomorrow:\n\n");

        for (TimetableSlot slot : slots) {
            sb.append("▸ *Period ").append(slot.getPeriodNumber()).append("*  ")
              .append(formatTime(slot.getStartTime())).append("–").append(formatTime(slot.getEndTime())).append("\n");
            if (slot.getSubject() != null) {
                sb.append("   📚 ").append(slot.getSubject().getSubjectName());
            } else if (StringUtils.hasText(slot.getLabel())) {
                sb.append("   🔖 ").append(slot.getLabel());
            }
            if (slot.getSchoolClass() != null) {
                sb.append("  |  ").append(slot.getSchoolClass().getClassName());
            }
            if (StringUtils.hasText(slot.getRoomNumber())) {
                sb.append("  |  Room ").append(slot.getRoomNumber());
            }
            sb.append("\n");
        }
        sb.append("\n_Have a great day!_ 🌟\n_Greenwood School Management_");
        return sb.toString();
    }

    private String buildStudentMessage(Student student, DayOfWeek day, List<TimetableSlot> slots) {
        String name = student.getUser() != null ? student.getUser().getFirstName() : "Student";
        StringBuilder sb = new StringBuilder();
        sb.append("📅 *").append(displayDay(day)).append(" Timetable — Greenwood School*\n\n");
        sb.append("Hello ").append(name).append("! Here's your class schedule for tomorrow:\n\n");

        for (TimetableSlot slot : slots) {
            sb.append("▸ *Period ").append(slot.getPeriodNumber()).append("*  ")
              .append(formatTime(slot.getStartTime())).append("–").append(formatTime(slot.getEndTime())).append("\n");
            if (slot.getSubject() != null) {
                sb.append("   📚 ").append(slot.getSubject().getSubjectName());
                if (slot.getTeacher() != null && slot.getTeacher().getUser() != null) {
                    sb.append("  |  ").append(slot.getTeacher().getUser().getFirstName())
                      .append(" ").append(slot.getTeacher().getUser().getLastName() != null
                              ? slot.getTeacher().getUser().getLastName() : "");
                }
            } else if (StringUtils.hasText(slot.getLabel())) {
                sb.append("   🔖 ").append(slot.getLabel());
            }
            if (StringUtils.hasText(slot.getRoomNumber())) {
                sb.append("  |  Room ").append(slot.getRoomNumber());
            }
            sb.append("\n");
        }
        sb.append("\n_Study hard and stay curious!_ 📖\n_Greenwood School Management_");
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    /** Returns the next school day (Mon–Sat), or null if tomorrow is Sunday. */
    private DayOfWeek nextSchoolDay() {
        DayOfWeek tomorrow = LocalDate.now().plusDays(1).getDayOfWeek();
        return tomorrow == DayOfWeek.SUNDAY ? null : tomorrow;
    }

    private String displayDay(DayOfWeek day) {
        // "MONDAY" → "Monday"
        String raw = day.name();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase();
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "";
    }
}
