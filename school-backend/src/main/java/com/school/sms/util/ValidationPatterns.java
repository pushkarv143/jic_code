package com.school.sms.util;

/**
 * Regex constants shared by the request DTOs.
 *
 * They live in one place because the alternative was nine independently-written
 * copies of the same rule, which had already drifted: most DTOs demanded
 * {@code ^\+?[0-9]{10,15}$} — digits only — while every phone number the
 * application actually stores looks like {@code +91-9810011122}. The result was
 * that a record could be read from the database and then rejected when saved
 * back unchanged.
 */
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    /**
     * Phone numbers as they are really stored and entered: digits, optionally with
     * {@code + - ( )} and spaces.
     *
     * Deliberately permissive. Phone formatting varies by country and by data
     * source, and a validator stricter than the data it guards does not improve
     * quality — it just makes existing records uneditable. The empty alternative
     * lets optional fields be cleared; required fields pair this with
     * {@code @NotBlank}.
     *
     * Kept identical to PHONE_PATTERN in
     * {@code school-frontend/src/utils/validationPatterns.ts}, so the client and
     * the API agree on what they will accept.
     */
    public static final String PHONE = "^$|^[0-9+\\-\\s()]{6,20}$";

    public static final String PHONE_MESSAGE =
            "Phone number may contain digits and optionally + - ( ) and spaces (6-20 characters)";

    /** Indian PIN code — six digits, matching what the seed data and forms use. */
    public static final String PINCODE = "^$|^[0-9]{4,10}$";

    public static final String PINCODE_MESSAGE = "Pincode must be 4-10 digits";
}
