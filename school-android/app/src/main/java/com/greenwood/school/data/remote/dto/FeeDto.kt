package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Fee categories, structures, per-student dues, payments and scholarships.   */
/* ------------------------------------------------------------------------- */

@Serializable
data class FeeCategoryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
)

@Serializable
data class FeeStructureDto(
    val id: Long,
    val classId: Long,
    val className: String? = null,
    val academicYearId: Long,
    val academicYearName: String? = null,
    val feeCategoryId: Long,
    val feeCategoryName: String? = null,
    val amount: Double = 0.0,
    val dueDate: String? = null,
)

@Serializable
data class FeeStructureRequestDto(
    val classId: Long,
    val academicYearId: Long,
    val feeCategoryId: Long,
    val amount: Double,
    val dueDate: String,
)

@Serializable
data class StudentFeeDto(
    val id: Long,
    val studentId: Long,
    val studentName: String? = null,
    val admissionNumber: String? = null,
    val classId: Long? = null,
    val className: String? = null,
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val feeStructureId: Long = 0,
    val feeCategoryId: Long? = null,
    val feeCategoryName: String? = null,
    val academicYearId: Long = 0,
    val academicYearName: String? = null,
    val amountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val dueDate: String? = null,
    val status: String = "UNPAID",
    /** Only populated on `GET /student-fees/{id}`. */
    val feePayments: List<FeePaymentDto>? = null,
) {
    val balance: Double get() = (amountDue - amountPaid).coerceAtLeast(0.0)
}

@Serializable
data class GenerateDuesRequestDto(
    val classId: Long,
    val academicYearId: Long,
    val feeStructureIds: List<Long>,
)

@Serializable
data class GenerateDuesResultDto(
    val generatedCount: Int = 0,
    val skippedCount: Int = 0,
)

@Serializable
data class FeePaymentDto(
    val id: Long,
    val studentFeeId: Long = 0,
    val amount: Double = 0.0,
    val paymentDate: String? = null,
    val paymentMode: String = "CASH",
    val transactionId: String? = null,
    val receiptNumber: String? = null,
    val collectedBy: Long? = null,
    val collectedByName: String? = null,
)

@Serializable
data class FeePaymentRequestDto(
    val studentFeeId: Long,
    val amount: Double,
    val paymentDate: String,
    val paymentMode: String,
    val transactionId: String? = null,
)

/** `POST /fee-payments` returns the payment plus the recalculated dues row. */
@Serializable
data class FeePaymentResultDto(
    val payment: FeePaymentDto? = null,
    val updatedStudentFee: StudentFeeDto? = null,
)

@Serializable
data class FeeReceiptDto(
    val paymentId: Long = 0,
    val receiptNumber: String? = null,
    val paymentDate: String? = null,
    val amount: Double = 0.0,
    val paymentMode: String? = null,
    val transactionId: String? = null,
    val studentName: String? = null,
    val admissionNumber: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val feeCategoryName: String? = null,
    val academicYearName: String? = null,
    val collectedByName: String? = null,
    val schoolName: String? = null,
    val schoolAddress: String? = null,
)

@Serializable
data class DuesSummaryDto(
    val totalDue: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val studentCount: Int = 0,
)

@Serializable
data class ScholarshipDto(
    val id: Long,
    val studentId: Long,
    val studentName: String? = null,
    val admissionNumber: String? = null,
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "FIXED",
    val academicYearId: Long = 0,
    val academicYearName: String? = null,
    val approvedBy: Long? = null,
)

@Serializable
data class ScholarshipRequestDto(
    val studentId: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val academicYearId: Long,
)
