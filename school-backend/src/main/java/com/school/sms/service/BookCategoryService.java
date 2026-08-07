package com.school.sms.service;

import com.school.sms.dto.request.BookCategoryRequest;
import com.school.sms.dto.response.BookCategoryDto;

import java.util.List;

public interface BookCategoryService {

    List<BookCategoryDto> getAll();

    BookCategoryDto create(BookCategoryRequest request);

    BookCategoryDto update(Long id, BookCategoryRequest request);

    void delete(Long id);
}
