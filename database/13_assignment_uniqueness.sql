-- =====================================================================
-- 13_assignment_uniqueness.sql
--
-- Two "one at a time" rules that nothing enforced before:
--
--   1. a teacher is class teacher of at most one section;
--   2. a student holds at most one class post at a time.
--
-- Both were previously enforceable only by convention. sections had no
-- constraint on class_teacher_id at all, and class_officials constrained
-- (class, role) - one head boy per class - but not the student, so the
-- same student could be head boy and monitor simultaneously.
--
-- WHY A UNIQUE KEY IS ENOUGH FOR RULE 1
-- MySQL treats NULLs in a unique index as distinct from one another, which
-- is usually the thing that defeats constraints like this (see the
-- current_flag trick in 12_class_module.sql). Here it is exactly what we
-- want: any number of sections may have no class teacher, but a given
-- teacher id can appear only once.
--
-- Rule 2 needs the current_flag trick, because a student legitimately has
-- many *ended* appointments and at most one live one.
--
-- EXISTING DATA IS CORRECTED, NOT REJECTED
-- ALTER TABLE ADD UNIQUE fails outright if duplicates exist, so the
-- duplicates have to go first. Where one teacher holds several sections,
-- the assignment on the lowest section id is kept and the rest are
-- cleared; the sections that lost a teacher are listed at the end of this
-- script so they can be reassigned deliberately. Clearing is reversible -
-- the section simply needs a teacher picked again - whereas guessing which
-- of two homerooms was intended is not something a migration can do.
--
-- Re-runnable: guarded on information_schema, so applying twice is a no-op.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- What is about to be corrected. Empty on a clean database.
-- ---------------------------------------------------------------------
SELECT s.id AS section_id, s.class_id, s.section_name, s.class_teacher_id,
       'will be cleared - teacher already heads a lower-numbered section' AS action
  FROM sections s
 WHERE s.class_teacher_id IS NOT NULL
   AND s.id > (SELECT MIN(s2.id) FROM sections s2 WHERE s2.class_teacher_id = s.class_teacher_id);

DROP PROCEDURE IF EXISTS sp_add_assignment_uniqueness;
DELIMITER $$
CREATE PROCEDURE sp_add_assignment_uniqueness()
BEGIN

  -- -------------------------------------------------------------------
  -- Rule 1: one homeroom per teacher.
  -- -------------------------------------------------------------------
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sections'
      AND index_name = 'uq_sections_class_teacher'
  ) THEN
    -- Keep the lowest section id per teacher, clear the rest.
    --
    -- The ids are collected into a temporary table first because MySQL refuses
    -- to read the table an UPDATE targets from a subquery of that same
    -- statement (error 1093), and wrapping the subquery in a derived table is
    -- not a reliable escape: the optimiser may merge the derived table back
    -- into the outer query rather than materialising it, and the error returns.
    -- Selecting into a temp table settles the question - the read completes
    -- before the write begins.
    DROP TEMPORARY TABLE IF EXISTS tmp_duplicate_homerooms;
    CREATE TEMPORARY TABLE tmp_duplicate_homerooms (id BIGINT PRIMARY KEY);

    INSERT INTO tmp_duplicate_homerooms (id)
    SELECT s2.id
      FROM sections s2
     WHERE s2.class_teacher_id IS NOT NULL
       AND s2.id > (SELECT MIN(s3.id) FROM sections s3
                     WHERE s3.class_teacher_id = s2.class_teacher_id);

    UPDATE sections s
      JOIN tmp_duplicate_homerooms d ON d.id = s.id
       SET s.class_teacher_id = NULL;

    DROP TEMPORARY TABLE tmp_duplicate_homerooms;

    ALTER TABLE sections
      ADD CONSTRAINT uq_sections_class_teacher UNIQUE (class_teacher_id);
  END IF;

  -- -------------------------------------------------------------------
  -- Rule 2: one live post per student.
  --
  -- Mirrors uq_class_official_current from 12_class_module.sql, keyed on
  -- the student instead of the (class, role) pair. Both indexes coexist:
  -- one stops a class having two head boys, this one stops a student being
  -- head boy and monitor at the same time.
  -- -------------------------------------------------------------------
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'class_officials'
      AND index_name = 'uq_class_official_student_current'
  ) THEN
    -- End the newer of any overlapping appointments, keeping the earliest.
    -- Same temp-table reason as above.
    DROP TEMPORARY TABLE IF EXISTS tmp_duplicate_posts;
    CREATE TEMPORARY TABLE tmp_duplicate_posts (id BIGINT PRIMARY KEY);

    INSERT INTO tmp_duplicate_posts (id)
    SELECT c2.id
      FROM class_officials c2
     WHERE c2.to_date IS NULL
       AND c2.id > (SELECT MIN(c3.id) FROM class_officials c3
                     WHERE c3.student_id = c2.student_id AND c3.to_date IS NULL);

    UPDATE class_officials co
      JOIN tmp_duplicate_posts d ON d.id = co.id
       SET co.to_date = CURRENT_DATE();

    DROP TEMPORARY TABLE tmp_duplicate_posts;

    ALTER TABLE class_officials
      ADD CONSTRAINT uq_class_official_student_current UNIQUE (student_id, current_flag);
  END IF;

END $$
DELIMITER ;

CALL sp_add_assignment_uniqueness();
DROP PROCEDURE IF EXISTS sp_add_assignment_uniqueness;

-- ---------------------------------------------------------------------
-- Verification. Both indexes must exist and both violation counts be 0.
-- ---------------------------------------------------------------------
SELECT
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sections'
      AND index_name = 'uq_sections_class_teacher') AS idx_one_homeroom,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'class_officials'
      AND index_name = 'uq_class_official_student_current') AS idx_one_post;

SELECT
  (SELECT COUNT(*) FROM (SELECT class_teacher_id FROM sections
     WHERE class_teacher_id IS NOT NULL
     GROUP BY class_teacher_id HAVING COUNT(*) > 1) v) AS teachers_with_two_homerooms,
  (SELECT COUNT(*) FROM (SELECT student_id FROM class_officials
     WHERE to_date IS NULL
     GROUP BY student_id HAVING COUNT(*) > 1) v) AS students_with_two_posts;

-- Sections left without a class teacher, so they can be reassigned.
SELECT s.id AS section_id, s.class_id, s.section_name
  FROM sections s WHERE s.class_teacher_id IS NULL ORDER BY s.class_id, s.section_name;
