package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto {

    private Long id;
    private String admissionNumber;
    private Integer rollNumber;

    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long academicYearId;
    private String academicYearName;

    private LocalDate admissionDate;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String religion;
    private String category;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String photoUrl;
    private String status;

    // Present only when the student has an optional login account (user_id is nullable per schema).
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Convenience fields for the list view, sourced from the primary guardian
    // since students carry no name/contact columns of their own in the schema.
    private String primaryGuardianName;
    private String primaryGuardianPhone;

    // Populated only on the single-resource ("full profile") GET.
    private List<GuardianDto> guardians;
    private MedicalDetailsDto medicalDetails;
    private List<StudentDocumentDto> documents;
}
