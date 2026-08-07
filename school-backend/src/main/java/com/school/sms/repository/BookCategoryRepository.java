package com.school.sms.repository;

import com.school.sms.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    List<BookCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
