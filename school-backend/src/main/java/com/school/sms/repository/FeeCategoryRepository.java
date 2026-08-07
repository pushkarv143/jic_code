package com.school.sms.repository;

import com.school.sms.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {

    List<FeeCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
