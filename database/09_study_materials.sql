-- =====================================================================
-- 09_study_materials.sql
--
-- Migration for an ALREADY-DEPLOYED database. A fresh install gets the
-- study_materials table from 01_schema.sql, its indexes from
-- 02_indexes.sql and the MATERIAL_* permissions from
-- 06_seed_reference_data.sql — this file exists so an existing database
-- can pick up the same objects without being rebuilt.
--
-- The DDL below is duplicated from 01_schema.sql on purpose: this project
-- has no migration tool, and 01_schema.sql is the canonical schema. Keep
-- the two definitions identical if either changes.
--
-- Every statement is guarded, so running this on a fresh install (or
-- twice) is a no-op rather than an error.
-- =====================================================================

USE school_management_system;

CREATE TABLE IF NOT EXISTS study_materials (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id       BIGINT NOT NULL,
  section_id     BIGINT NULL,
  subject_id     BIGINT NOT NULL,
  teacher_id     BIGINT NULL,
  title          VARCHAR(255) NOT NULL,
  description    TEXT,
  material_type  ENUM('NOTES','PRESENTATION','WORKSHEET','REFERENCE','VIDEO','OTHER')
                 NOT NULL DEFAULT 'NOTES',
  file_url       VARCHAR(255),
  external_url   VARCHAR(500),
  is_published   TINYINT(1) NOT NULL DEFAULT 1,
  is_deleted     TINYINT(1) NOT NULL DEFAULT 0,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by     BIGINT NULL,
  updated_by     BIGINT NULL,
  CONSTRAINT fk_material_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_material_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
  CONSTRAINT fk_material_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
  CONSTRAINT fk_material_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MySQL has no CREATE INDEX IF NOT EXISTS, so each index is created only when
-- information_schema says it is absent. Wrapped in a procedure because a bare
-- prepared statement cannot be made conditional at the top level.
DROP PROCEDURE IF EXISTS sp_add_material_indexes;
DELIMITER $$
CREATE PROCEDURE sp_add_material_indexes()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'study_materials'
      AND index_name = 'idx_study_materials_class_section'
  ) THEN
    CREATE INDEX idx_study_materials_class_section ON study_materials(class_id, section_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'study_materials'
      AND index_name = 'idx_study_materials_teacher'
  ) THEN
    CREATE INDEX idx_study_materials_teacher ON study_materials(teacher_id);
  END IF;
END $$
DELIMITER ;

CALL sp_add_material_indexes();
DROP PROCEDURE IF EXISTS sp_add_material_indexes;

-- ---------------------------------------------------------------------
-- Permissions.
-- ---------------------------------------------------------------------
INSERT INTO permissions (name, module, description)
SELECT 'MATERIAL_VIEW', 'MATERIAL', 'View and download study materials'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MATERIAL_VIEW');

INSERT INTO permissions (name, module, description)
SELECT 'MATERIAL_MANAGE', 'MATERIAL', 'Upload and manage study materials'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MATERIAL_MANAGE');

-- SUPER_ADMIN holds every permission, including any added later.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('MATERIAL_VIEW','MATERIAL_MANAGE')
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Teaching roles upload and manage; the section they may upload to is decided at
-- request time by SectionAccessGuard, not by this grant.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('MATERIAL_VIEW','MATERIAL_MANAGE')
WHERE r.name IN ('TEACHER','CLASS_TEACHER')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Everyone else who has a legitimate view gets read-only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'MATERIAL_VIEW'
WHERE r.name IN ('PRINCIPAL','VICE_PRINCIPAL','STUDENT','PARENT')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- Demo rows, mirroring the sections set up in 08_rbac_demo_data.sql.
--
-- Three rows on purpose:
--   1. section A, published   -> student.demo sees it
--   2. section A, unpublished -> student.demo must NOT see it (draft)
--   3. section B, published   -> student.demo must NOT see it (not their section)
-- Between them a single student login demonstrates both visibility rules.
-- ---------------------------------------------------------------------
SET @SECTION_A := (SELECT id FROM sections ORDER BY id LIMIT 1);
SET @CLASS_A   := (SELECT class_id FROM sections WHERE id = @SECTION_A);
SET @SECTION_B := (SELECT id FROM sections WHERE id <> @SECTION_A ORDER BY id LIMIT 1);
SET @CLASS_B   := (SELECT class_id FROM sections WHERE id = @SECTION_B);
SET @SUBJECT_A := (SELECT id FROM subjects WHERE class_id = @CLASS_A AND is_deleted = 0 ORDER BY id LIMIT 1);
SET @SUBJECT_B := (SELECT id FROM subjects WHERE class_id = @CLASS_B AND is_deleted = 0 ORDER BY id LIMIT 1);
SET @DEMO_TEACHER_ID := (SELECT id FROM teachers WHERE employee_id = 'EMP-DEMO-TCH-001');

INSERT INTO study_materials (class_id, section_id, subject_id, teacher_id, title, description, material_type, external_url, is_published)
SELECT @CLASS_A, @SECTION_A, @SUBJECT_A, @DEMO_TEACHER_ID,
       'Chapter 1 - Introduction Notes',
       'Class notes covering the first chapter, shared after today''s lesson.',
       'NOTES', 'https://example.edu/materials/chapter-1-notes.pdf', 1
WHERE @SUBJECT_A IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM study_materials WHERE title = 'Chapter 1 - Introduction Notes');

INSERT INTO study_materials (class_id, section_id, subject_id, teacher_id, title, description, material_type, external_url, is_published)
SELECT @CLASS_A, @SECTION_A, @SUBJECT_A, @DEMO_TEACHER_ID,
       'Chapter 2 - Draft Slides',
       'Not yet released to students - used to verify drafts stay hidden.',
       'PRESENTATION', 'https://example.edu/materials/chapter-2-draft.pptx', 0
WHERE @SUBJECT_A IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM study_materials WHERE title = 'Chapter 2 - Draft Slides');

INSERT INTO study_materials (class_id, section_id, subject_id, teacher_id, title, description, material_type, external_url, is_published)
SELECT @CLASS_B, @SECTION_B, @SUBJECT_B, @DEMO_TEACHER_ID,
       'Other Section Worksheet',
       'Belongs to a section student.demo is not in - used to verify scoping.',
       'WORKSHEET', 'https://example.edu/materials/other-section-worksheet.pdf', 1
WHERE @SUBJECT_B IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM study_materials WHERE title = 'Other Section Worksheet');

-- ---------------------------------------------------------------------
-- Verification: what each demo login should see through
-- /api/v1/study-materials.
--
--   admin / principal   -> all three rows, drafts included
--   teacher.demo        -> the two section A rows (their own uploads,
--                          draft included); section B is refused on write
--                          and filtered out on read
--   student.demo        -> exactly one row, "Chapter 1 - Introduction Notes"
--   parent.demo         -> the same one row, via their child
-- ---------------------------------------------------------------------
SELECT 'Study material schema + demo data loaded' AS status,
       (SELECT COUNT(*) FROM study_materials)     AS material_rows,
       @SECTION_A                                  AS taught_section_id,
       @SECTION_B                                  AS untaught_section_id;
