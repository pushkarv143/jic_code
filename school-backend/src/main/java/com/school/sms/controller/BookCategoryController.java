package com.school.sms.controller;

import com.school.sms.dto.request.BookCategoryRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.BookCategoryDto;
import com.school.sms.service.BookCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/book-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Book Categories", description = "Manage library book categories")
public class BookCategoryController {

    private final BookCategoryService bookCategoryService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN','TEACHER','CLASS_TEACHER','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List all book categories")
    public ResponseEntity<ApiResponse<List<BookCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Book categories retrieved successfully", bookCategoryService.getAll()));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Create a new book category")
    public ResponseEntity<ApiResponse<BookCategoryDto>> create(@Valid @RequestBody BookCategoryRequest request) {
        BookCategoryDto created = bookCategoryService.create(request);
        URI location = URI.create("/api/v1/book-categories/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Book category created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a book category")
    public ResponseEntity<ApiResponse<BookCategoryDto>> update(@PathVariable Long id,
                                                                @Valid @RequestBody BookCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book category updated successfully",
                bookCategoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Delete a book category")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
