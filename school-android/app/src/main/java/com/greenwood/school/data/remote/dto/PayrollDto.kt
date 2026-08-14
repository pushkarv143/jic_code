package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Payroll: salary structures, monthly runs, slips and the payroll dashboard. */
/* ------------------------------------------------------------------------- */

/**
 * `employeeId` here is the person's **`users.id`**, not their teacher/staff-table
 * primary key — a trap worth knowing about when wiring the employee picker.
 */
@Serializable
data class SalaryStructureDto(
    val id: Long,
    val employeeId: Long = 0,
    val employeeType: String = "TEACHER",
    val employeeName: String? = null,
    val basicSalary: Double = 0.0,
    val hra: Double = 0.0,
    val da: Double = 0.0,
    val otherAllowances: Double = 0.0,
    val pfPercentage: Double = 0.0,
    val esiPercentage: Double = 0.0,
) {
    val grossSalary: Double get() = basicSalary + hra + da + otherAllowances
}

@Serializable
data class SalaryStructureRequestDto(
    val employeeId: Long,
    val employeeType: String,
    val basicSalary: Double,
    val hra: Double,
    val da: Double,
    val otherAllowances: Double,
    val pfPercentage: Double,
    val esiPercentage: Double,
)

@Serializable
data class PayrollRunDto(
    val id: Long,
    val employeeId: Long = 0,
    val employeeType: String = "TEACHER",
    val employeeName: String? = null,
    val month: Int = 0,
    val year: Int = 0,
    val basicSalary: Double = 0.0,
    val allowances: Double = 0.0,
    val deductions: Double = 0.0,
    val pf: Double = 0.0,
    val esi: Double = 0.0,
    val netSalary: Double = 0.0,
    val paymentDate: String? = null,
    val status: String = "PENDING",
)

@Serializable
data class GeneratePayrollRequestDto(
    val employeeType: String,
    val month: Int,
    val year: Int,
)

@Serializable
data class GeneratePayrollResultDto(
    val generatedCount: Int = 0,
    val skippedCount: Int = 0,
)

@Serializable
data class MarkPaidRequestDto(val paymentDate: String)

@Serializable
data class PayrollDashboardDto(
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0,
    val employeeCount: Int = 0,
)

@Serializable
data class SalarySlipDto(
    val employeeName: String? = null,
    val employeeId: String? = null,
    val departmentName: String? = null,
    val designationName: String? = null,
    val month: Int = 0,
    val year: Int = 0,
    val basicSalary: Double = 0.0,
    val hra: Double? = null,
    val da: Double? = null,
    val otherAllowances: Double? = null,
    val pf: Double = 0.0,
    val esi: Double = 0.0,
    val netSalary: Double = 0.0,
    val schoolName: String? = null,
)
