package com.greenwood.school.core.common

/**
 * Values shared across features. Enum *option lists* live here so a dropdown and
 * the validator that checks it can never drift apart; the strings are the exact
 * wire values the backend enums accept (see `SCHEMA_CONTRACT.md`).
 */
object Constants {

    /** Matches `AppConstants.DEFAULT_PAGE_SIZE` on the backend. */
    const val DEFAULT_PAGE_SIZE = 20

    /** Larger page for "load everything into a picker" cases (classes, subjects…). */
    const val PICKER_PAGE_SIZE = 200

    const val SEARCH_DEBOUNCE_MS = 350L

    val GENDERS = listOf("MALE", "FEMALE", "OTHER")

    val STUDENT_STATUSES = listOf("ACTIVE", "INACTIVE", "ALUMNI", "TRANSFERRED")
    val STAFF_STATUSES = listOf("ACTIVE", "INACTIVE", "RESIGNED", "TERMINATED")
    val EMPLOYMENT_TYPES = listOf("FULL_TIME", "PART_TIME", "CONTRACT")

    val ATTENDANCE_STATUSES = listOf("PRESENT", "ABSENT", "LATE", "HALF_DAY", "LEAVE")

    val LEAVE_STATUSES = listOf("PENDING", "APPROVED", "REJECTED")
    val LEAVE_APPLICANT_TYPES = listOf("TEACHER", "STAFF", "STUDENT")

    /** The web app offers these as free text; the same shortlist keeps data tidy. */
    val LEAVE_TYPES = listOf("CASUAL", "SICK", "EARNED", "MATERNITY", "UNPAID", "OTHER")

    val STUDENT_FEE_STATUSES = listOf("PAID", "UNPAID", "PARTIAL", "OVERDUE")
    val PAYMENT_MODES = listOf("CASH", "ONLINE", "CHEQUE", "CARD")
    val SCHOLARSHIP_TYPES = listOf("PERCENTAGE", "FIXED")

    val BOOK_ISSUE_STATUSES = listOf("ISSUED", "RETURNED", "OVERDUE")

    val HOSTEL_TYPES = listOf("BOYS", "GIRLS")
    val HOSTEL_STUDENT_STATUSES = listOf("ACTIVE", "VACATED")
    val HOSTEL_FEE_STATUSES = listOf("PAID", "UNPAID")

    val PAYROLL_EMPLOYEE_TYPES = listOf("TEACHER", "STAFF")
    val PAYROLL_STATUSES = listOf("PENDING", "PAID")

    val CALENDAR_EVENT_TYPES = listOf("HOLIDAY", "EVENT", "EXAM", "OTHER")

    val NOTIFICATION_CHANNELS = listOf("SMS", "EMAIL", "PUSH", "IN_APP")

    val ADMISSION_STATUSES = listOf("PENDING", "APPROVED", "REJECTED")

    val ASSIGNMENT_SUBMISSION_STATUSES = listOf("SUBMITTED", "LATE", "GRADED")

    val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    val DOCUMENT_TYPES = listOf(
        "BIRTH_CERTIFICATE",
        "TRANSFER_CERTIFICATE",
        "AADHAR",
        "PHOTO",
        "MARKSHEET",
        "OTHER",
    )
}
