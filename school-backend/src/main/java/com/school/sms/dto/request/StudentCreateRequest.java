package com.school.sms.dto.request;

import com.school.sms.util.ValidationPatterns;
import com.school.sms.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCreateRequest {

    // ---- optional login account fields (student login is optional per schema contract) ----
    /*
     * No username and no password.
     *
     * Both are generated server-side when the student is admitted and emailed to
     * the address below — the username as first name plus the initial of the last
     * name, the password as a random one the student must replace at first sign-in.
     * Accepting either from a client would put the school back in the business of
     * inventing credentials, and would let a caller set a password the account's
     * owner does not know.
     *
     * The email is therefore required in practice, and the service says so: it is
     * where the credentials go, and an account nobody can be told about is worse
     * than a refused admission.
     */
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required — the student's credentials are sent to it")
    private String email;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;

    // ---- student fields ----
    // Admission number is generated server-side (ADM{year}{sequence}) when omitted.
    private String admissionNumber;

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

    @Valid
    private List<GuardianRequest> guardians;
}
