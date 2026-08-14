package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Settings, roles/permissions, audit logs, global search and Excel import.   */
/* ------------------------------------------------------------------------- */

@Serializable
data class SchoolInfoDto(
    val id: Long? = null,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logoUrl: String? = null,
    val establishedYear: Int? = null,
    val affiliationNumber: String? = null,
)

/** `GET /settings/system` returns a flat `[{key, value}]` list. */
@Serializable
data class SystemSettingDto(val key: String = "", val value: String = "")

@Serializable
data class RoleInfoDto(
    val id: Long,
    val name: String = "",
    val description: String? = null,
)

@Serializable
data class PermissionInfoDto(
    val id: Long,
    val name: String = "",
    val module: String? = null,
    val description: String? = null,
)

@Serializable
data class AuditLogDto(
    val id: Long,
    val userId: Long? = null,
    val userName: String? = null,
    val action: String = "",
    val entityName: String = "",
    val entityId: Long? = null,
    /** Raw JSON strings straight from the backend; rendered verbatim in a mono block. */
    val oldValue: String? = null,
    val newValue: String? = null,
    val ipAddress: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class GlobalSearchResultItemDto(
    val id: Long,
    val displayName: String? = null,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val admissionNumber: String? = null,
    val employeeId: String? = null,
    val isbn: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
) {
    /** The backend populates whichever of these fits the entity; take the first present. */
    val label: String
        get() = displayName ?: name ?: title ?: "#$id"

    val secondary: String?
        get() = subtitle
            ?: admissionNumber
            ?: employeeId
            ?: isbn
            ?: listOfNotNull(className, sectionName).joinToString(" - ").ifBlank { null }
}

@Serializable
data class GlobalSearchResponseDto(
    val students: List<GlobalSearchResultItemDto> = emptyList(),
    val teachers: List<GlobalSearchResultItemDto> = emptyList(),
    val books: List<GlobalSearchResultItemDto> = emptyList(),
) {
    val isEmpty: Boolean get() = students.isEmpty() && teachers.isEmpty() && books.isEmpty()
}

@Serializable
data class ImportSkippedRowDto(val rowNumber: Int = 0, val reason: String = "")

@Serializable
data class ImportResultDto(
    val importedCount: Int = 0,
    val skippedRows: List<ImportSkippedRowDto> = emptyList(),
)
