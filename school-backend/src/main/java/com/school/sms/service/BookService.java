package com.school.sms.service;

import com.school.sms.dto.request.BookRequest;
import com.school.sms.dto.response.BookDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BookService {

    PageResponse<BookDto> getAll(String search, Long categoryId, Pageable pageable);

    BookDto getById(Long id);

    BookDto create(BookRequest request);

    BookDto update(Long id, BookRequest request);

    void delete(Long id);
}
