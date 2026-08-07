package com.school.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String author;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private String publisher;

    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies;

    private String rackNumber;

    @DecimalMin(value = "0.0", message = "Price must not be negative")
    private BigDecimal price;
}
