package com.school.sms.util;

public final class AppConstants {

    private AppConstants() {
    }

    // Seeded role names — must match SCHEMA_CONTRACT.md roles table exactly (id 1-11 in this order)
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_PRINCIPAL = "PRINCIPAL";
    public static final String ROLE_VICE_PRINCIPAL = "VICE_PRINCIPAL";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT";
    public static final String ROLE_LIBRARIAN = "LIBRARIAN";
    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_SECURITY_GUARD = "SECURITY_GUARD";

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";

    /**
     * The school runs a single section per class, named "A".
     *
     * <p>The section dimension is kept in the schema rather than removed — it is a
     * NOT NULL foreign key on students, attendance, assignments, study materials,
     * timetable slots and the subject-teacher mapping — but exactly one section
     * exists per class, and the UI does not ask anyone to choose it. See
     * {@code database/14_single_section_a.sql} for the migration that collapsed the
     * existing sections onto it.
     */
    public static final String SINGLE_SECTION_NAME = "A";

    public static final String JWT_ROLE_PREFIX = "ROLE_";

    /**
     * SpEL fragment for {@code @PreAuthorize}: the administrator is never gated by a
     * permission grant.
     *
     * <p>Permission-gated endpoints must OR this in. Without it a missing
     * {@code role_permissions} row — a data problem, not a policy decision — would
     * lock the administrator out of a module they own, and the only fix would be a
     * SQL statement nobody thinks to run. Role-gated endpoints already list
     * SUPER_ADMIN explicitly and do not need it.
     */
    public static final String ADMIN_OVERRIDE = "hasRole('" + ROLE_SUPER_ADMIN + "')";

    // Prefix for the fine-grained role_permissions grants carried on the principal.
    // Kept distinct from JWT_ROLE_PREFIX so permission names such as ROLE_VIEW /
    // ROLE_MANAGE cannot be mistaken for Spring Security roles.
    public static final String PERMISSION_AUTHORITY_PREFIX = "PERM_";

    // Roles that see every student/teacher record. Grouped here so the guards and
    // the controllers agree on one definition of "management".
    public static final java.util.Set<String> MANAGEMENT_ROLES = java.util.Set.of(
            ROLE_SUPER_ADMIN, ROLE_PRINCIPAL, ROLE_VICE_PRINCIPAL);

    // Roles whose view of student data is narrowed to the students they teach.
    //
    // One role since 21_single_teacher_role.sql. CLASS_TEACHER used to sit beside
    // TEACHER here, but the two were never two kinds of person: it held exactly
    // TEACHER's permissions plus six, and 44 users carried it while only 17 held a
    // section. Being a class teacher is now teachers.is_class_teacher, which grants
    // those six on top of this role — see CustomUserDetailsService.
    public static final java.util.Set<String> TEACHING_ROLES = java.util.Set.of(ROLE_TEACHER);

    // Roles whose view of student data is narrowed to their own record (STUDENT)
    // or their own children's records (PARENT).
    public static final java.util.Set<String> SELF_SCOPED_ROLES = java.util.Set.of(
            ROLE_STUDENT, ROLE_PARENT);
}
