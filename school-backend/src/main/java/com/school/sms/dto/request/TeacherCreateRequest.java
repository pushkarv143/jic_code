package com.school.sms.dto.request;

import com.school.sms.util.ValidationPatterns;
import com.school.sms.entity.EmploymentType;
import com.school.sms.entity.Gender;
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

/**
 * Flat request DTO grouping the User-account fields and the Teacher-profile
 * fields in one payload, matching the existing flat style used by
 * CreateUserRequest/UpdateUserRequest rather than introducing a nested
 * sub-object convention.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCreateRequest {

    // ---- user account fields ----
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit and special character"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
    private String phone;

    // ---- teacher profile fields ----
    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Designation is required")
    private Long designationId;

    private String qualification;

    private Integer experienceYears;

    private LocalDate joiningDate;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 10)
    private String pincode;

    @Size(max = 5)
    private String bloodGroup;

    @Pattern(regexp = ValidationPatterns.PHONE, message = "Emergency contact: " + ValidationPatterns.PHONE_MESSAGE)
    private String emergencyContact;

    private java.math.BigDecimal salary;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;
}
