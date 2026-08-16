package com.school.sms.repository;

import com.school.sms.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    long countByDeletedFalse();

    // Library dashboard: SUM(total_copies)/SUM(available_copies) across all non-deleted books.
    // Returns a list of rows like every other aggregate here — declaring a bare Object[]
    // yields a one-element list wrapping the row, so the caller ends up casting Object[] to
    // Number and blowing up.
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(b.totalCopies), 0), COALESCE(SUM(b.availableCopies), 0) " +
                    "FROM Book b WHERE b.deleted = false")
    java.util.List<Object[]> sumCopies();

    // Reports: /api/v1/reports/library-summary.
    @org.springframework.data.jpa.repository.Query(
            "SELECT b.category.name, COUNT(b) FROM Book b WHERE b.deleted = false " +
                    "GROUP BY b.category.name ORDER BY b.category.name")
    java.util.List<Object[]> countGroupByCategory();
}
