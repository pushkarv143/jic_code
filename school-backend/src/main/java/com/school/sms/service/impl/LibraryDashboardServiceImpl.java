package com.school.sms.service.impl;

import com.school.sms.dto.response.LibraryDashboardDto;
import com.school.sms.entity.IssueStatus;
import com.school.sms.repository.BookIssueRepository;
import com.school.sms.repository.BookRepository;
import com.school.sms.service.LibraryDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LibraryDashboardServiceImpl implements LibraryDashboardService {

    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;

    @Override
    @Transactional(readOnly = true)
    public LibraryDashboardDto getDashboard() {
        Object[] copies = bookRepository.sumCopies();
        long totalCopies = ((Number) copies[0]).longValue();
        long availableCopies = ((Number) copies[1]).longValue();

        return LibraryDashboardDto.builder()
                .totalBooks(bookRepository.countByDeletedFalse())
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .issuedCount(bookIssueRepository.countByStatus(IssueStatus.ISSUED))
                .overdueCount(bookIssueRepository.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now()))
                .build();
    }
}
