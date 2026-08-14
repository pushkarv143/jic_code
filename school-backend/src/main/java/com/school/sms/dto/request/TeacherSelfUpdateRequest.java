package com.school.sms.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The subset of their own profile a teacher may maintain.
 *
 * Deliberately much narrower than {@link TeacherUpdateRequest}: department,
 * designation, salary, employment type, joining date and status are absent,
 * because those are HR decisions rather than personal details. Qualification is
 * included — teachers add certifications over time and the office has no reason
 * to gatekeep that.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSelfUpdateRequest {

    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{6,20}$", message = "Phone number format is invalid")
    private String phone;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Size(max = 10, message = "Pincode must be at most 10 characters")
    @Pattern(regexp = "^$|^[0-9]{4,10}$", message = "Pincode must be 4-10 digits")
    private String pincode;

    @Size(max = 5, message = "Blood group must be at most 5 characters")
    private String bloodGroup;

    @Size(max = 20, message = "Emergency contact must be at most 20 characters")
    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{6,20}$", message = "Emergency contact format is invalid")
    private String emergencyContact;

    @Size(max = 255, message = "Qualification must be at most 255 characters")
    private String qualification;
}
