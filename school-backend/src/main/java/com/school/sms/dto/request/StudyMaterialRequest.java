package com.school.sms.dto.request;

import com.school.sms.entity.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create/update payload for a study material.
 *
 * Sent as multipart so an optional file can ride along with the fields. The
 * file/externalUrl either-or rule is checked in the service rather than with a
 * class-level constraint, so the error names the field that is actually missing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialRequest {

    @NotNull(message = "Class is required")
    private Long classId;

    /** Omit to share with every section of the class. */
    private Long sectionId;

    @NotNull(message = "Subject is required")
    private Long subjectId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @NotNull(message = "Material type is required")
    private MaterialType materialType;

    @Size(max = 500, message = "External URL must be at most 500 characters")
    private String externalUrl;

    /** Defaults to published; send false to keep it as a draft. */
    private Boolean published;
}
