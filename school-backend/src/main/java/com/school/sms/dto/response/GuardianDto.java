package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianDto {

    private Long id;
    private Long studentId;
    private String name;
    private String relation;
    private String occupation;
    private String phone;
    private String email;
    private String address;
    /*
     * Named isPrimary, not primary.
     *
     * Lombok's getter for a boolean field called `primary` is isPrimary(), which
     * Jackson serialises back as "primary" — while GuardianRequest, both clients
     * and the database column all say isPrimary. The flag therefore arrived as
     * undefined in the UI: the primary-guardian checkbox never rendered as
     * checked, and the form's "exactly one primary" rule could never be satisfied.
     * @JsonProperty pins the wire name so every layer agrees.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("isPrimary")
    private boolean isPrimary;
}
