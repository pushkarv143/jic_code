package com.school.sms.dto.request;

import com.school.sms.util.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The subset of their own profile a STUDENT (or their PARENT) is allowed to
 * maintain. Deliberately much narrower than {@link StudentUpdateRequest}: class,
 * section, roll number, admission details, academic year and status stay under
 * office control, since letting a student edit them would let them move
 * themselves between classes or reinstate a withdrawn admission.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentSelfUpdateRequest {

    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
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
}
