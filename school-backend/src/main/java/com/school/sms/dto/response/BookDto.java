package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Long categoryId;
    private String categoryName;
    private String publisher;
    private Integer totalCopies;
    private Integer availableCopies;
    private String rackNumber;
    private BigDecimal price;
}
