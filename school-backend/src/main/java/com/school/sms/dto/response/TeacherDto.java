package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDto {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
    private boolean active;

    private String employeeId;

    /**
     * Whether this teacher also holds a class as its class teacher.
     *
     * <p>Replaces the CLASS_TEACHER role: the base role stays TEACHER for a
     * teacher's whole career and this flag comes and goes with the assignment,
     * granting the six permissions in {@code class_teacher_permissions} while it is
     * set. Server-maintained from {@code sections.class_teacher_id}, so a client
     * shows it and never sends it.
     */
    private boolean classTeacher;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;
    private String qualification;
    private Integer experienceYears;
    private LocalDate joiningDate;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String bloodGroup;
    private String emergencyContact;
    private BigDecimal salary;
    private String employmentType;
    private String status;

    private List<TeacherAssignmentDto> assignments;
}
