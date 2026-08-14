package com.school.sms.repository;

import com.school.sms.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudyMaterialRepository
        extends JpaRepository<StudyMaterial, Long>, JpaSpecificationExecutor<StudyMaterial> {
}
