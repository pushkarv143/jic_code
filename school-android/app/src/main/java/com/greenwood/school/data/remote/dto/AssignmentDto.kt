package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Assignments, submissions and scheduled online classes.                     */
/* ------------------------------------------------------------------------- */

@Serializable
data class AssignmentDto(
    val id: Long,
    val classId: Long = 0,
    val className: String? = null,
    val sectionId: Long = 0,
    val sectionName: String? = null,
    val subjectId: Long = 0,
    val subjectName: String? = null,
    val teacherId: Long = 0,
    val teacherName: String? = null,
    val title: String = "",
    val description: String? = null,
    val fileUrl: String? = null,
    val assignedDate: String? = null,
    val dueDate: String? = null,
)

/**
 * A teaching resource shared with a class or a single section.
 *
 * [sectionId] is null when the material is shared with every section of the class —
 * a meaningful null, since student visibility is derived from it. Exactly one of
 * [fileUrl] / [externalUrl] is populated.
 */
@Serializable
data class StudyMaterialDto(
    val id: Long,
    val classId: Long = 0,
    val className: String? = null,
    val sectionId: Long? = null,
    val sectionName: String? = null,
    val subjectId: Long = 0,
    val subjectName: String? = null,
    val teacherId: Long? = null,
    val teacherName: String? = null,
    val title: String = "",
    val description: String? = null,
    val materialType: String = "NOTES",
    val fileUrl: String? = null,
    val externalUrl: String? = null,
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    /**
     * Whether the signed-in user may edit or delete this material. Computed by the
     * backend from the ownership rule it enforces, so the app never re-derives
     * "did I upload this?".
     */
    val canManage: Boolean = false,
) {
    /** Where the resource actually lives, whichever kind it is. */
    val resourceUrl: String? get() = externalUrl ?: fileUrl

    val isExternalLink: Boolean get() = externalUrl != null
}

@Serializable
data class AssignmentSubmissionDto(
    val id: Long,
    val assignmentId: Long = 0,
    val studentId: Long = 0,
    val studentName: String? = null,
    val rollNumber: String? = null,
    val fileUrl: String? = null,
    val submittedAt: String? = null,
    val marksObtained: Double? = null,
    val feedback: String? = null,
    val status: String = "SUBMITTED",
)

@Serializable
data class GradeSubmissionRequestDto(
    val marksObtained: Double,
    val feedback: String? = null,
)

@Serializable
data class OnlineClassDto(
    val id: Long,
    val classId: Long = 0,
    val className: String? = null,
    val sectionId: Long = 0,
    val sectionName: String? = null,
    val subjectId: Long = 0,
    val subjectName: String? = null,
    val teacherId: Long = 0,
    val teacherName: String? = null,
    val title: String = "",
    val meetingLink: String = "",
    val scheduledAt: String? = null,
    val durationMinutes: Int = 0,
)

@Serializable
data class OnlineClassRequestDto(
    val classId: Long,
    val sectionId: Long,
    val subjectId: Long,
    val title: String,
    val meetingLink: String,
    val scheduledAt: String,
    val durationMinutes: Int,
)
