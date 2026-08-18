package com.school.sms.entity;

/**
 * A post a student can hold in a class. Stored as a string so adding a post
 * is a migration on the ENUM rather than a renumbering of existing rows.
 *
 * <p>HEAD_BOY/HEAD_GIRL are gender-checked on assignment; the rest are not,
 * since nothing about being a monitor or a sports captain implies one.
 */
public enum ClassOfficialRole {
    HEAD_BOY,
    HEAD_GIRL,
    MONITOR,
    SPORTS_CAPTAIN,
    CULTURAL_SECRETARY;

    /** The gender this post requires, or null when it is open to anyone. */
    public Gender requiredGender() {
        return switch (this) {
            case HEAD_BOY -> Gender.MALE;
            case HEAD_GIRL -> Gender.FEMALE;
            default -> null;
        };
    }
}
