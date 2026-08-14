package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.BookCategoryDto
import com.greenwood.school.data.remote.dto.BookDto
import com.greenwood.school.data.remote.dto.BookIssueDto
import com.greenwood.school.data.remote.dto.BookRequestDto
import com.greenwood.school.data.remote.dto.IssueBookRequestDto
import com.greenwood.school.data.remote.dto.LibraryDashboardDto
import com.greenwood.school.data.remote.dto.NamedRequestDto
import com.greenwood.school.data.remote.dto.PageEnvelope
import com.greenwood.school.data.remote.dto.ReturnBookRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** `/book-categories`, `/books`, `/book-issues`, `/library/dashboard`. */
interface LibraryApi {

    @GET("book-categories")
    suspend fun getCategories(): ApiEnvelope<List<BookCategoryDto>>

    @POST("book-categories")
    suspend fun createCategory(@Body request: NamedRequestDto): ApiEnvelope<BookCategoryDto>

    @PUT("book-categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body request: NamedRequestDto): ApiEnvelope<BookCategoryDto>

    @DELETE("book-categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("books")
    suspend fun getBooks(
        @Query("search") search: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "title,asc",
    ): ApiEnvelope<PageEnvelope<BookDto>>

    @GET("books/{id}")
    suspend fun getBook(@Path("id") id: Long): ApiEnvelope<BookDto>

    @POST("books")
    suspend fun createBook(@Body request: BookRequestDto): ApiEnvelope<BookDto>

    @PUT("books/{id}")
    suspend fun updateBook(@Path("id") id: Long, @Body request: BookRequestDto): ApiEnvelope<BookDto>

    @DELETE("books/{id}")
    suspend fun deleteBook(@Path("id") id: Long): ApiEnvelope<Unit>

    @GET("book-issues")
    suspend fun getIssues(
        @Query("bookId") bookId: Long? = null,
        @Query("studentId") studentId: Long? = null,
        @Query("teacherId") teacherId: Long? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "issueDate,desc",
    ): ApiEnvelope<PageEnvelope<BookIssueDto>>

    @POST("book-issues/issue")
    suspend fun issueBook(@Body request: IssueBookRequestDto): ApiEnvelope<BookIssueDto>

    @PATCH("book-issues/{id}/return")
    suspend fun returnBook(@Path("id") id: Long, @Body request: ReturnBookRequestDto): ApiEnvelope<BookIssueDto>

    @GET("book-issues/overdue")
    suspend fun getOverdue(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "dueDate,asc",
    ): ApiEnvelope<PageEnvelope<BookIssueDto>>

    @GET("library/dashboard")
    suspend fun getDashboard(): ApiEnvelope<LibraryDashboardDto>
}
