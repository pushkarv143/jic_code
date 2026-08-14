package com.greenwood.school.data.remote.dto

import com.greenwood.school.core.common.Formatters
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Notices, calendar events, birthdays, notifications, admission enquiries    */
/* and the parent portal.                                                     */
/* ------------------------------------------------------------------------- */

@Serializable
data class NoticeDto(
    val id: Long,
    val title: String = "",
    val description: String = "",
    /** Null means "everyone"; otherwise the single role the notice targets. */
    val targetRole: String? = null,
    val publishedBy: Long = 0,
    val publishedByName: String? = null,
    val publishedAt: String? = null,
    val expiryDate: String? = null,
    val attachmentUrl: String? = null,
)

@Serializable
data class CalendarEventDto(
    val id: Long,
    val title: String = "",
    val description: String? = null,
    val eventDate: String? = null,
    val eventType: String = "EVENT",
    val createdBy: Long? = null,
)

@Serializable
data class CalendarEventRequestDto(
    val title: String,
    val description: String? = null,
    val eventDate: String,
    val eventType: String,
)

@Serializable
data class BirthdayPersonDto(
    val id: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val designationName: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName)

    val subtitle: String?
        get() = designationName ?: listOfNotNull(className, sectionName).joinToString(" - ").ifBlank { null }
}

@Serializable
data class BirthdaysResponseDto(
    val students: List<BirthdayPersonDto> = emptyList(),
    val teachers: List<BirthdayPersonDto> = emptyList(),
)

@Serializable
data class NotificationDto(
    val id: Long,
    val recipientId: Long = 0,
    val recipientName: String? = null,
    val type: String = "IN_APP",
    val subject: String = "",
    val message: String = "",
    val status: String = "SENT",
    val sentAt: String? = null,
)

/** Exactly one of `recipientId` / `targetRole` must be set — enforced server-side. */
@Serializable
data class SendNotificationRequestDto(
    val recipientId: Long? = null,
    val targetRole: String? = null,
    val type: String,
    val subject: String,
    val message: String,
)

@Serializable
data class NotificationSendResultDto(
    val sentCount: Int = 0,
    val failedCount: Int = 0,
)

@Serializable
data class AdmissionEnquiryDto(
    val id: Long,
    val studentName: String = "",
    val parentName: String = "",
    val phone: String = "",
    val email: String = "",
    val classApplying: String = "",
    val dob: String? = null,
    val address: String? = null,
    val status: String = "PENDING",
    val documentsUrl: String? = null,
    val appliedAt: String? = null,
)

@Serializable
data class AdmissionEnquiryRequestDto(
    val studentName: String,
    val parentName: String,
    val phone: String,
    val email: String,
    val classApplying: String,
    val dob: String,
    val address: String,
)

/** One row of `GET /parents/me/children`. */
@Serializable
data class ParentChildDto(
    val studentId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val admissionNumber: String = "",
    val className: String? = null,
    val sectionName: String? = null,
    val photoUrl: String? = null,
) {
    val displayName: String get() = Formatters.personName(firstName, lastName, admissionNumber)

    val classSection: String
        get() = listOfNotNull(className, sectionName).joinToString(" - ").ifBlank { Formatters.PLACEHOLDER }
}
