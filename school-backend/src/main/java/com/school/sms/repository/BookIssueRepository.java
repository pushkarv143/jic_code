package com.school.sms.repository;

import com.school.sms.entity.BookIssue;
import com.school.sms.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long>, JpaSpecificationExecutor<BookIssue> {

    long countByStatus(IssueStatus status);

    long countByStatusAndDueDateBefore(IssueStatus status, LocalDate date);
}
