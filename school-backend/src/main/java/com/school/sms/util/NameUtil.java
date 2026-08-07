package com.school.sms.util;

import org.springframework.util.StringUtils;

public final class NameUtil {

    private NameUtil() {
    }

    /**
     * Joins first/last name for display, tolerating a null last name.
     * Reused wherever a joined User's name needs to be shown next to another
     * entity (teacher assignments, class-teacher name on a section, etc.).
     */
    public static String fullName(String firstName, String lastName) {
        if (!StringUtils.hasText(firstName)) {
            return lastName;
        }
        if (!StringUtils.hasText(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
