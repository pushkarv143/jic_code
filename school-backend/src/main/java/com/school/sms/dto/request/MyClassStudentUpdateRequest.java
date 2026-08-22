package com.school.sms.dto.request;

import com.school.sms.entity.Gender;
import com.school.sms.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Editing a student on the caller's own class roster.
 *
 * <p>Omits {@code classId}, {@code sectionId} and {@code academicYearId} because a class teacher who could set {@code sectionId} would be able to
 * move a student out of their class, or pull one in from another. That is a
 * transfer, it belongs to the office, and {@code StudentController} already has an
 * audited endpoint for it. Leaving the field out of this type means "edit my
 * student" cannot quietly become "transfer any student".
 *
 * <p>Null means "leave unchanged", matching {@link StudentUpdateRequest}, so a
 * caller can send only the fields the form actually touched.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyClassStudentUpdateRequest {

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
}
