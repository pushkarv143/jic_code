package com.school.sms.controller;

import com.school.sms.dto.request.BookRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.BookDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Books", description = "Manage the library book catalog")
public class BookController {

    private final BookService bookService;

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN','TEACHER','CLASS_TEACHER','STUDENT','PARENT')";
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','LIBRARIAN')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "List books, paginated, filterable by search (title/author/isbn) and category")
    public ResponseEntity<ApiResponse<PageResponse<BookDto>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Books retrieved successfully",
                bookService.getAll(search, categoryId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Get a single book")
    public ResponseEntity<ApiResponse<BookDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Book retrieved successfully", bookService.getById(id)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Add a new book to the catalog")
    public ResponseEntity<ApiResponse<BookDto>> create(@Valid @RequestBody BookRequest request) {
        BookDto created = bookService.create(request);
        URI location = URI.create("/api/v1/books/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.success("Book created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Update a book")
    public ResponseEntity<ApiResponse<BookDto>> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", bookService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Soft-delete a book")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
