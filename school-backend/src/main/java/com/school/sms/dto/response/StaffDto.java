package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal staff-directory row — powers the employee search used by Payroll's
 * salary-structure/salary-slip pages for non-teaching staff (ACCOUNTANT,
 * LIBRARIAN, RECEPTIONIST, SECURITY_GUARD), same role Teacher already fills
 * for teaching staff. Read-only by design; full staff CRUD is out of scope.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDto {

    private Long id;
    private Long userId;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String departmentName;
    private String designationName;
    private String status;
}
