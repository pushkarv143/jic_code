package com.school.sms.util;

public final class AppConstants {

    private AppConstants() {
    }

    // Seeded role names — must match SCHEMA_CONTRACT.md roles table exactly (id 1-11 in this order)
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_PRINCIPAL = "PRINCIPAL";
    public static final String ROLE_VICE_PRINCIPAL = "VICE_PRINCIPAL";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_CLASS_TEACHER = "CLASS_TEACHER";
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT";
    public static final String ROLE_LIBRARIAN = "LIBRARIAN";
    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_SECURITY_GUARD = "SECURITY_GUARD";

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";

    public static final String JWT_ROLE_PREFIX = "ROLE_";
}
