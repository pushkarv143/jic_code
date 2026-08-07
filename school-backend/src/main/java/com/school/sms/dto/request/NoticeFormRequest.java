package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bound via @ModelAttribute from a multipart/form-data body (POST/PUT
 * /api/v1/notices) — the accompanying optional "file" part is bound
 * separately via @RequestPart.
 */
@Getter
@Setter
public class NoticeFormRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    // Role name (e.g. "STUDENT", "TEACHER"); null/blank means "everyone".
    private String targetRole;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;
}
