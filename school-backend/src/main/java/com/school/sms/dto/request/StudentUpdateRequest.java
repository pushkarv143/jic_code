package com.school.sms.dto.request;

import com.school.sms.util.ValidationPatterns;
import com.school.sms.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
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

    /*
     * Identity fields. These live on the linked `users` row rather than on
     * `students`, but they belong in this request because the edit form presents
     * one student record: splitting them across two endpoints would make renaming
     * a student a two-call operation that can half-fail.
     *
     * They were previously absent while the frontend sent them anyway. With
     * FAIL_ON_UNKNOWN_PROPERTIES disabled, Jackson dropped them without complaint,
     * so editing a name returned 200 and changed nothing.
     *
     * Null means "leave unchanged", which is what lets a caller update only the
     * academic fields without having to echo the name back.
     */
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;

    @NotNull(message = "Class is required")
    private Long classId;

    @NotNull(message = "Section is required")
    private Long sectionId;

    /*
     * roll_number is deliberately absent.
     *
     * It is a student position in their class, assigned by the server and unique
     * per class (uq_students_class_roll) - not a fact about the student that
     * anyone types. Accepting it and validating it is what allowed duplicates and
     * values like 151611; leaving the field out means there is nothing to
     * validate and nothing to silently ignore.
     */
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
