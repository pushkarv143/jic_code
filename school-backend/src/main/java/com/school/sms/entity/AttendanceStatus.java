package com.school.sms.entity;

/**
 * Shared status enum for both student_attendance and teacher_attendance
 * (identical ENUM definition on both tables per SCHEMA_CONTRACT.md).
 */
public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    HALF_DAY,
    LEAVE
}
