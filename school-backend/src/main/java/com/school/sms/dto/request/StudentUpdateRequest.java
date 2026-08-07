package com.school.sms.dto.request;

import com.school.sms.entity.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentUpdateRequest {

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Section is required")
    private Long sectionId;

    private Integer rollNumber;

    private LocalDate admissionDate;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 5)
    private String bloodGroup;

    @Size(max = 50)
    private String religion;

    @Size(max = 50)
    private String category;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 10)
    private String pincode;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;
}
