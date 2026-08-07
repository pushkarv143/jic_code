package com.school.sms.repository;

import com.school.sms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllBySchoolClassIdOrderBySectionNameAsc(Long classId);

    boolean existsBySchoolClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);

    // Excel student import: resolves a row's plain-text section name to an id within its class.
    Optional<Section> findFirstBySchoolClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);
}
