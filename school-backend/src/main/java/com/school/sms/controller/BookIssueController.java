package com.school.sms.controller;

import com.school.sms.dto.request.BookIssueRequest;
import com.school.sms.dto.request.BookReturnRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.BookIssueDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.BookIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/book-issues")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Book Issues", description = "Issue and return library books")
public class BookIssueController {

    private final BookIssueService bookIssueService;

    // STUDENT/PARENT are included; StudentAccessGuard scopes them to their own issues.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN','TEACHER','STUDENT','PARENT')";
    private static final String STAFF_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List book issues, paginated, filterable by book/student/teacher/status")
    public ResponseEntity<ApiResponse<PageResponse<BookIssueDto>>> getAll(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Book issues retrieved successfully",
                bookIssueService.getAll(bookId, studentId, teacherId, status, pageable)));
    }

    @PostMapping("/issue")
    @PreAuthorize(STAFF_ROLES)
    @Operation(summary = "Issue a book to a student or a teacher (exactly one of studentId/teacherId)")
    public ResponseEntity<ApiResponse<BookIssueDto>> issue(@Valid @RequestBody BookIssueRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book issued successfully", bookIssueService.issue(request)));
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize(STAFF_ROLES)
    @Operation(summary = "Return an issued book, computing any overdue fine")
    public ResponseEntity<ApiResponse<BookIssueDto>> returnBook(@PathVariable Long id,
                                                                 @RequestBody(required = false) BookReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book returned successfully",
                bookIssueService.returnBook(id, request != null ? request : new BookReturnRequest())));
    }

    @GetMapping("/overdue")
    @PreAuthorize(STAFF_ROLES)
    @Operation(summary = "List currently overdue book issues (status ISSUED, due date in the past)")
    public ResponseEntity<ApiResponse<PageResponse<BookIssueDto>>> getOverdue(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Overdue book issues retrieved successfully",
                bookIssueService.getOverdue(pageable)));
    }
}
