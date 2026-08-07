package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Lighter sibling of {@link BaseEntity} for tables that only carry
 * created_at/updated_at (no created_by/updated_by columns) per
 * SCHEMA_CONTRACT.md — e.g. academic_years, departments, designations,
 * teachers, classes, sections, subjects, students, guardians,
 * student_medical_details, student_documents. Do not add createdBy/updatedBy
 * here: the physical columns do not exist on those tables and Hibernate
 * would fail to read/write them since ddl-auto is none.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
