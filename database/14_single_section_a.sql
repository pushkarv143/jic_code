-- =====================================================================
-- 14_single_section_a.sql - collapse every class onto a single section "A"
--
-- The school runs one section per class, so the section dimension is
-- reduced to a single "A" per class rather than removed: section_id is
-- NOT NULL on students, student_attendance, assignments, online_classes,
-- study_materials, timetable_slots and class_subject_teacher, so the
-- column has to keep pointing at something real.
--
-- ORDER MATTERS, because the foreign keys disagree about deletion:
--
--   class_subject_teacher  ON DELETE CASCADE   <- rows VANISH silently
--   study_materials        ON DELETE CASCADE   <- rows VANISH silently
--   timetable_slots        ON DELETE CASCADE   <- rows VANISH silently
--   class_officials        ON DELETE SET NULL
--   students               ON DELETE RESTRICT  <- delete fails loudly
--   student_attendance     ON DELETE RESTRICT
--   assignments            ON DELETE RESTRICT
--   online_classes         ON DELETE RESTRICT
--
-- Every child row is therefore repointed at the class's A section BEFORE
-- any section is dropped. Deleting first would take the three CASCADE
-- tables with it and raise no error at all.
--
-- Two unique keys force a de-duplicate rather than a plain repoint:
--   uq_cst            (section_id, subject_id)
--   uq_timetable_slot (section_id, day_of_week, period_number)
-- A class's subjects are shared by its sections, so B/C already hold a
-- row for the same subject that A holds. The A row is treated as
-- authoritative and the B/C duplicate dropped; where A has no such row
-- the B/C row is promoted, so an assignment is only ever discarded as a
-- genuine duplicate.
--
-- Re-running is safe: once no non-A section exists, every statement
-- below matches nothing.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 0. Before/after reporting, so the run leaves an audit trail.
-- ---------------------------------------------------------------------
SELECT 'BEFORE' AS stage,
       (SELECT COUNT(*) FROM sections)                          AS sections,
       (SELECT COUNT(*) FROM sections WHERE section_name <> 'A') AS non_a_sections,
       (SELECT COUNT(*) FROM students s
          JOIN sections x ON x.id = s.section_id
         WHERE x.section_name <> 'A')                            AS students_to_move,
       (SELECT COUNT(*) FROM student_attendance a
          JOIN sections x ON x.id = a.section_id
         WHERE x.section_name <> 'A')                            AS attendance_to_move,
       (SELECT COUNT(*) FROM class_subject_teacher c
          JOIN sections x ON x.id = c.section_id
         WHERE x.section_name <> 'A')                            AS mappings_to_resolve;

-- ---------------------------------------------------------------------
-- 1. Guarantee an A section for every class. All 20 classes have one
--    today; a class without one would have its children repointed at
--    nothing, so this is a precondition rather than a nicety.
-- ---------------------------------------------------------------------
INSERT INTO sections (section_name, class_id, room_number, capacity)
SELECT 'A', c.id, NULL, NULL
  FROM classes c
 WHERE NOT EXISTS (SELECT 1 FROM sections s
                    WHERE s.class_id = c.id AND s.section_name = 'A');

-- ---------------------------------------------------------------------
-- 2. Resolve old section -> surviving A section once, into a temp map,
--    so no later statement has to recompute the lookup.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_section_merge;
CREATE TEMPORARY TABLE tmp_section_merge (
    old_id    BIGINT PRIMARY KEY,
    target_id BIGINT NOT NULL,
    class_id  BIGINT NOT NULL,
    INDEX idx_tsm_target (target_id)
) ENGINE = InnoDB;

INSERT INTO tmp_section_merge (old_id, target_id, class_id)
SELECT old.id, a.id, old.class_id
  FROM sections old
  JOIN sections a ON a.class_id = old.class_id AND a.section_name = 'A'
 WHERE old.section_name <> 'A';

-- ---------------------------------------------------------------------
-- 3. Preserve a homeroom teacher for any A section that lacks one.
--    27 non-A sections hold a class teacher while 4 A sections have
--    none; without this, those four classes finish the merge with no
--    homeroom teacher. uq_sections_class_teacher is UNIQUE on
--    class_teacher_id, so the donor must be cleared before the A
--    section can take the teacher over.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_teacher_transfer;
CREATE TEMPORARY TABLE tmp_teacher_transfer (
    class_id   BIGINT PRIMARY KEY,
    teacher_id BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO tmp_teacher_transfer (class_id, teacher_id)
SELECT m.class_id, MIN(donor.class_teacher_id)
  FROM tmp_section_merge m
  JOIN sections donor ON donor.id = m.old_id
  JOIN sections a     ON a.id = m.target_id
 WHERE donor.class_teacher_id IS NOT NULL
   AND a.class_teacher_id IS NULL
 GROUP BY m.class_id;

UPDATE sections s
  JOIN tmp_section_merge m ON m.old_id = s.id
   SET s.class_teacher_id = NULL;

UPDATE sections a
  JOIN tmp_teacher_transfer t ON t.class_id = a.class_id
   SET a.class_teacher_id = t.teacher_id
 WHERE a.section_name = 'A'
   AND a.class_teacher_id IS NULL;

-- ---------------------------------------------------------------------
-- 4. class_subject_teacher - de-duplicate against uq_cst, then repoint.
--
--    The whole contest is resolved in a scratch table first. MySQL
--    rejects a multi-table DELETE whose subquery reads the table being
--    deleted from (ERROR 1093), so the surviving ids are computed up
--    front and the DELETE then only reads the scratch table.
--
--    Every candidate row for a post-merge (A section, subject) pair is
--    collected - the rows already on A, plus the B/C rows that are about
--    to land on A - and one winner per pair is chosen: the existing A
--    row if there is one, otherwise the lowest-id B/C row. That keeps
--    A authoritative while still promoting a B/C assignment for any
--    subject A does not already cover.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_cst_all;
CREATE TEMPORARY TABLE tmp_cst_all (
    id         BIGINT PRIMARY KEY,
    target_id  BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    is_on_a    TINYINT NOT NULL,
    INDEX idx_cst_all_pair (target_id, subject_id)
) ENGINE = InnoDB;

INSERT INTO tmp_cst_all (id, target_id, subject_id, is_on_a)
SELECT c.id, c.section_id, c.subject_id, 1
  FROM class_subject_teacher c
  JOIN sections s ON s.id = c.section_id
 WHERE s.section_name = 'A'
UNION ALL
SELECT c.id, m.target_id, c.subject_id, 0
  FROM class_subject_teacher c
  JOIN tmp_section_merge m ON m.old_id = c.section_id;

DROP TEMPORARY TABLE IF EXISTS tmp_cst_keep;
CREATE TEMPORARY TABLE tmp_cst_keep (
    keep_id BIGINT PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO tmp_cst_keep (keep_id)
SELECT COALESCE(MIN(CASE WHEN is_on_a = 1 THEN id END), MIN(id))
  FROM tmp_cst_all
 GROUP BY target_id, subject_id;

DELETE cst
  FROM class_subject_teacher cst
  JOIN tmp_section_merge m ON m.old_id = cst.section_id
 WHERE cst.id NOT IN (SELECT keep_id FROM tmp_cst_keep);

UPDATE class_subject_teacher cst
  JOIN tmp_section_merge m ON m.old_id = cst.section_id
   SET cst.section_id = m.target_id;

-- ---------------------------------------------------------------------
-- 5. timetable_slots - the same shape of problem, and the same scratch
--    table treatment, against uq_timetable_slot
--    (section_id, day_of_week, period_number). A period already on A
--    wins; a B/C period lands only in a slot A leaves empty.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_slot_all;
CREATE TEMPORARY TABLE tmp_slot_all (
    id            BIGINT PRIMARY KEY,
    target_id     BIGINT NOT NULL,
    day_of_week   VARCHAR(10) NOT NULL,
    period_number INT NOT NULL,
    is_on_a       TINYINT NOT NULL,
    INDEX idx_slot_all_pair (target_id, day_of_week, period_number)
) ENGINE = InnoDB;

INSERT INTO tmp_slot_all (id, target_id, day_of_week, period_number, is_on_a)
SELECT t.id, t.section_id, t.day_of_week, t.period_number, 1
  FROM timetable_slots t
  JOIN sections s ON s.id = t.section_id
 WHERE s.section_name = 'A'
UNION ALL
SELECT t.id, m.target_id, t.day_of_week, t.period_number, 0
  FROM timetable_slots t
  JOIN tmp_section_merge m ON m.old_id = t.section_id;

DROP TEMPORARY TABLE IF EXISTS tmp_slot_keep;
CREATE TEMPORARY TABLE tmp_slot_keep (
    keep_id BIGINT PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO tmp_slot_keep (keep_id)
SELECT COALESCE(MIN(CASE WHEN is_on_a = 1 THEN id END), MIN(id))
  FROM tmp_slot_all
 GROUP BY target_id, day_of_week, period_number;

DELETE ts
  FROM timetable_slots ts
  JOIN tmp_section_merge m ON m.old_id = ts.section_id
 WHERE ts.id NOT IN (SELECT keep_id FROM tmp_slot_keep);

UPDATE timetable_slots ts
  JOIN tmp_section_merge m ON m.old_id = ts.section_id
   SET ts.section_id = m.target_id;

-- ---------------------------------------------------------------------
-- 6. The RESTRICT and SET NULL children - a straight repoint. None of
--    these carries a unique key involving section_id
--    (student_attendance is unique on student_id + attendance_date, so
--    moving the section cannot collide).
-- ---------------------------------------------------------------------
UPDATE students s
  JOIN tmp_section_merge m ON m.old_id = s.section_id
   SET s.section_id = m.target_id;

UPDATE student_attendance a
  JOIN tmp_section_merge m ON m.old_id = a.section_id
   SET a.section_id = m.target_id;

UPDATE assignments x
  JOIN tmp_section_merge m ON m.old_id = x.section_id
   SET x.section_id = m.target_id;

UPDATE online_classes x
  JOIN tmp_section_merge m ON m.old_id = x.section_id
   SET x.section_id = m.target_id;

UPDATE study_materials x
  JOIN tmp_section_merge m ON m.old_id = x.section_id
   SET x.section_id = m.target_id;

UPDATE class_officials x
  JOIN tmp_section_merge m ON m.old_id = x.section_id
   SET x.section_id = m.target_id;

-- ---------------------------------------------------------------------
-- 7. Drop the emptied sections. Any RESTRICT row missed above makes
--    this fail loudly rather than silently orphan itself - the point of
--    doing the repoints first.
-- ---------------------------------------------------------------------
DELETE s
  FROM sections s
  JOIN tmp_section_merge m ON m.old_id = s.id;

DROP TEMPORARY TABLE IF EXISTS tmp_section_merge;
DROP TEMPORARY TABLE IF EXISTS tmp_teacher_transfer;
DROP TEMPORARY TABLE IF EXISTS tmp_cst_all;
DROP TEMPORARY TABLE IF EXISTS tmp_cst_keep;
DROP TEMPORARY TABLE IF EXISTS tmp_slot_all;
DROP TEMPORARY TABLE IF EXISTS tmp_slot_keep;

-- ---------------------------------------------------------------------
-- 8. Verify. non_a_sections_must_be_0 is the assertion; sections should
--    equal classes, and no student may be left without a section.
-- ---------------------------------------------------------------------
SELECT 'AFTER' AS stage,
       (SELECT COUNT(*) FROM sections)                                   AS sections,
       (SELECT COUNT(*) FROM sections WHERE section_name <> 'A')          AS non_a_sections_must_be_0,
       (SELECT COUNT(*) FROM classes)                                     AS classes,
       (SELECT COUNT(*) FROM students)                                    AS students,
       (SELECT COUNT(*) FROM student_attendance)                          AS attendance,
       (SELECT COUNT(*) FROM class_subject_teacher)                       AS mappings,
       (SELECT COUNT(*) FROM timetable_slots)                             AS timetable_slots,
       (SELECT COUNT(*) FROM sections WHERE class_teacher_id IS NOT NULL) AS sections_with_homeroom_teacher;

SELECT section_name, COUNT(*) AS n FROM sections GROUP BY section_name;
