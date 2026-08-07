package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A single persisted student_attendance row, as returned by the paginated
 * /attendance/students/report endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceRecordDto {

    private Long id;
    private Long studentId;
    private String firstName;
    private String lastName;
    private String admissionNumber;
    private Integer rollNumber;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private LocalDate attendanceDate;
    private String status;
    private String remarks;
    private Long markedBy;
    private String markedByName;
}
