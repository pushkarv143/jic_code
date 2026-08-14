package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Hostel: buildings, rooms, residents, visitor log and hostel fees.          */
/* ------------------------------------------------------------------------- */

@Serializable
data class HostelDto(
    val id: Long,
    val name: String = "",
    val wardenName: String? = null,
    val wardenContact: String? = null,
    val type: String = "BOYS",
)

@Serializable
data class HostelRequestDto(
    val name: String,
    val wardenName: String? = null,
    val wardenContact: String? = null,
    val type: String,
)

@Serializable
data class HostelRoomDto(
    val id: Long,
    val hostelId: Long = 0,
    val hostelName: String? = null,
    val roomNumber: String = "",
    val capacity: Int = 0,
    val occupiedCount: Int = 0,
) {
    val vacancies: Int get() = (capacity - occupiedCount).coerceAtLeast(0)
    val isFull: Boolean get() = vacancies == 0
}

@Serializable
data class HostelRoomRequestDto(
    val roomNumber: String,
    val capacity: Int,
)

@Serializable
data class HostelStudentDto(
    val id: Long,
    val studentId: Long = 0,
    val studentName: String? = null,
    val admissionNumber: String? = null,
    val roomId: Long = 0,
    val roomNumber: String? = null,
    val hostelId: Long? = null,
    val hostelName: String? = null,
    val allocationDate: String? = null,
    val vacateDate: String? = null,
    val status: String = "ACTIVE",
)

@Serializable
data class HostelStudentRequestDto(
    val studentId: Long,
    val roomId: Long,
    val allocationDate: String,
)

@Serializable
data class VacateRequestDto(val vacateDate: String)

@Serializable
data class HostelVisitorDto(
    val id: Long,
    val studentId: Long = 0,
    val studentName: String? = null,
    val visitorName: String = "",
    val relation: String? = null,
    val phone: String? = null,
    val visitDate: String? = null,
    val purpose: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
) {
    val isCheckedIn: Boolean get() = checkOut.isNullOrBlank()
}

@Serializable
data class HostelVisitorRequestDto(
    val studentId: Long,
    val visitorName: String,
    val relation: String,
    val phone: String? = null,
    val visitDate: String,
    val purpose: String? = null,
)

@Serializable
data class HostelFeeDto(
    val id: Long,
    val studentId: Long = 0,
    val studentName: String? = null,
    val month: Int = 0,
    val year: Int = 0,
    val amount: Double = 0.0,
    val paidStatus: String = "UNPAID",
)

@Serializable
data class HostelFeeRequestDto(
    val studentId: Long,
    val month: Int,
    val year: Int,
    val amount: Double,
)
