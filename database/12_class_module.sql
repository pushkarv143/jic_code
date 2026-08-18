-- =====================================================================
-- 12_class_module.sql
--
-- Rounds out the class module with the two things it could not express:
-- who holds a post in a class (head boy, head girl, monitor, ...) and
-- when each section is taught what, by whom, in which room.
--
-- Class teacher and subject teacher already exist and are NOT touched
-- here: sections.class_teacher_id carries the former and
-- class_subject_teacher the latter. This file adds only what was missing.
--
-- Re-runnable: every object is guarded on information_schema, so applying
-- it twice is a no-op.
-- =====================================================================

USE school_management_system;

DROP PROCEDURE IF EXISTS sp_add_class_module;
DELIMITER $$
CREATE PROCEDURE sp_add_class_module()
BEGIN

  -- -------------------------------------------------------------------
  -- class_officials — one row per appointment, historical rows kept.
  --
  -- `current_flag` is a generated column, and it exists to work around a
  -- MySQL limitation rather than to carry information. The obvious
  -- constraint for "one current head boy per class" is
  -- UNIQUE (class_id, role, to_date) with to_date NULL meaning current —
  -- but MySQL treats NULLs in a unique index as distinct from each other,
  -- so that index happily accepts two sitting head boys. Generating
  -- 1 for a current row and NULL for an ended one inverts the quirk into
  -- the behaviour we want: the 1s collide (at most one current holder per
  -- class and role) while the NULLs do not (any number of past holders).
  --
  -- ON DELETE CASCADE on student_id: an appointment is meaningless without
  -- the student, and a deleted student must not linger as head boy. The
  -- service additionally ends an appointment when the student leaves the
  -- class, which a foreign key cannot see.
  -- -------------------------------------------------------------------
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'class_officials'
  ) THEN
    CREATE TABLE class_officials (
      id           BIGINT AUTO_INCREMENT PRIMARY KEY,
      class_id     BIGINT NOT NULL,
      section_id   BIGINT NULL,
      student_id   BIGINT NOT NULL,
      role         ENUM('HEAD_BOY','HEAD_GIRL','MONITOR','SPORTS_CAPTAIN','CULTURAL_SECRETARY') NOT NULL,
      from_date    DATE NOT NULL,
      to_date      DATE NULL,
      remarks      VARCHAR(255) NULL,
      created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      current_flag TINYINT(1) AS (IF(to_date IS NULL, 1, NULL)) STORED,
      UNIQUE KEY uq_class_official_current (class_id, role, current_flag),
      CONSTRAINT fk_co_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE CASCADE,
      CONSTRAINT fk_co_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL,
      CONSTRAINT fk_co_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    CREATE INDEX idx_class_officials_student ON class_officials(student_id);
    CREATE INDEX idx_class_officials_class_current ON class_officials(class_id, current_flag);
  END IF;

  -- -------------------------------------------------------------------
  -- timetable_slots — one row per (section, day, period).
  --
  -- subject_id and teacher_id are nullable on purpose: assembly, games and
  -- free periods occupy a slot without either. room_number is denormalised
  -- from sections.room_number rather than joined, because a period can be
  -- taught somewhere other than the section's home room (a lab, a field)
  -- and the clash check has to reason about where the class actually is.
  --
  -- The unique key stops a section being double-booked. Teacher and room
  -- clashes span sections, so no single-table constraint can express them;
  -- they are detected in the service and surfaced as warnings rather than
  -- rejected, since a half-built timetable is legitimately inconsistent
  -- while it is being edited.
  -- -------------------------------------------------------------------
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'timetable_slots'
  ) THEN
    CREATE TABLE timetable_slots (
      id            BIGINT AUTO_INCREMENT PRIMARY KEY,
      class_id      BIGINT NOT NULL,
      section_id    BIGINT NOT NULL,
      day_of_week   ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
      period_number INT NOT NULL,
      start_time    TIME NOT NULL,
      end_time      TIME NOT NULL,
      subject_id    BIGINT NULL,
      teacher_id    BIGINT NULL,
      room_number   VARCHAR(50) NULL,
      label         VARCHAR(100) NULL,
      created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      UNIQUE KEY uq_timetable_slot (section_id, day_of_week, period_number),
      CONSTRAINT fk_ts_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE CASCADE,
      CONSTRAINT fk_ts_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
      CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL,
      CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- The clash queries scan by (day, period) across every section, so the
    -- teacher/room lookups get their own composite indexes.
    CREATE INDEX idx_timetable_teacher_slot ON timetable_slots(teacher_id, day_of_week, period_number);
    CREATE INDEX idx_timetable_room_slot    ON timetable_slots(room_number, day_of_week, period_number);
  END IF;

END $$
DELIMITER ;

CALL sp_add_class_module();
DROP PROCEDURE IF EXISTS sp_add_class_module;

-- ---------------------------------------------------------------------
-- Verification. Both counts must be 1.
-- ---------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'class_officials')  AS class_officials_table,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'timetable_slots')  AS timetable_slots_table;
