package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------- */
/* Library: catalogue, categories, issue/return and the librarian dashboard.  */
/* ------------------------------------------------------------------------- */

@Serializable
data class BookCategoryDto(val id: Long, val name: String)

@Serializable
data class BookDto(
    val id: Long,
    val title: String = "",
    val author: String? = null,
    val isbn: String? = null,
    val categoryId: Long = 0,
    val categoryName: String? = null,
    val publisher: String? = null,
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val rackNumber: String? = null,
    val price: Double? = null,
) {
    val isAvailable: Boolean get() = availableCopies > 0
}

@Serializable
data class BookRequestDto(
    val title: String,
    val author: String,
    val isbn: String,
    val categoryId: Long,
    val publisher: String? = null,
    val totalCopies: Int,
    val rackNumber: String? = null,
    val price: Double? = null,
)

@Serializable
data class BookIssueDto(
    val id: Long,
    val bookId: Long = 0,
    val bookTitle: String? = null,
    val isbn: String? = null,
    val studentId: Long? = null,
    val studentName: String? = null,
    val teacherId: Long? = null,
    val teacherName: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val returnDate: String? = null,
    val fineAmount: Double = 0.0,
    val status: String = "ISSUED",
) {
    /** A book is issued either to a student or to a teacher, never both. */
    val borrowerName: String? get() = studentName ?: teacherName
}

@Serializable
data class IssueBookRequestDto(
    val bookId: Long,
    val studentId: Long? = null,
    val teacherId: Long? = null,
    val dueDate: String,
)

@Serializable
data class ReturnBookRequestDto(val returnDate: String)

@Serializable
data class LibraryDashboardDto(
    val totalBooks: Int = 0,
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val issuedCount: Int = 0,
    val overdueCount: Int = 0,
)
