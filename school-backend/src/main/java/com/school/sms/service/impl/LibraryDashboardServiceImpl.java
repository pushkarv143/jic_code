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
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryDashboardServiceImpl implements LibraryDashboardService {

    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;

    @Override
    @Transactional(readOnly = true)
    public LibraryDashboardDto getDashboard() {
        List<Object[]> rows = bookRepository.sumCopies();
        Object[] copies = rows.isEmpty() ? new Object[]{0L, 0L} : rows.get(0);
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
