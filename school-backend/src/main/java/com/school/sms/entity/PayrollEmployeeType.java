package com.school.sms.entity;

/**
 * employee_type on salary_structures/payroll — distinct from {@link EmploymentType}
 * (Teacher.employmentType is FULL_TIME/PART_TIME/CONTRACT; this instead says
 * whether the payroll employee is a teacher or a non-teaching staff member).
 */
public enum PayrollEmployeeType {
    TEACHER,
    STAFF
}
