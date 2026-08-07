package com.school.sms.service;

import com.school.sms.dto.request.BookIssueRequest;
import com.school.sms.dto.request.BookReturnRequest;
import com.school.sms.dto.response.BookIssueDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BookIssueService {

    PageResponse<BookIssueDto> getAll(Long bookId, Long studentId, Long teacherId, String status, Pageable pageable);

    BookIssueDto issue(BookIssueRequest request);

    BookIssueDto returnBook(Long id, BookReturnRequest request);

    PageResponse<BookIssueDto> getOverdue(Pageable pageable);
}
