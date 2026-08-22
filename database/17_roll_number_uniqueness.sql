-- =====================================================================
-- 17_roll_number_uniqueness.sql
--
-- Makes a roll number identify exactly one student in a class, and
-- renumbers the existing data so it does.
--
-- WHY THE DUPLICATES ARE THERE
-- 07_sample_data.sql spread 500 students round-robin across 54 sections
-- and numbered them 1..N *within each section*. 14_single_section_a.sql
-- then collapsed every class onto a single section "A" - correctly
-- repointing the students, but without renumbering them. Three former
-- sections' worth of students therefore landed in one section each still
-- carrying rolls 1, 2, 3..., which is why class 1 has three students on
-- every roll number up to its strength.
--
-- Nothing was enforcing it either way: `roll_number INT` on students had
-- no unique key at any level. The service already auto-assigned the next
-- free number when the field was omitted, so new admissions were fine;
-- it was only the migrated rows that were wrong, and nothing would have
-- caught it if they were not.
--
-- WHAT THIS DOES
-- 1. Renumbers every student 1..N within their class, ordered by
--    admission_number so the sequence follows the order they joined and
--    the result is deterministic rather than dependent on row order.
-- 2. Adds UNIQUE KEY (class_id, roll_number) so it cannot drift again.
--
-- SCOPE OF THE KEY - class, not section or school:
--   * admission_number is already UNIQUE school-wide
--     (uq_students_admission_number in 01_schema.sql) and is the
--     school-level identifier. Nothing to do there.
--   * roll_number is a position within a class, so the class is the right
--     scope. With one section per class this is the same set of rows as
--     (section_id, roll_number) today, but it stays correct if a class
--     ever runs two sections again - two students in different sections
--     of the same class should not share roll 1.
--   * classes carry academic_year_id, so a class id belongs to one year
--     and the key is naturally scoped to that year. Promoting a student
--     moves them to next year's class, where they are renumbered.
--
-- SOFT-DELETED STUDENTS
-- A unique key applies to every row, including is_deleted = 1, so a
-- removed student would otherwise keep holding a roll number nobody can
-- reuse. There are none in this database today; the renumber below covers
-- active students and leaves any deleted row's number as it is, which is
-- safe because MySQL allows repeated NULLs but not repeated values - if a
-- future delete ever needs its roll released, set it to NULL.
--
-- Re-running is safe: the renumber is idempotent (already-correct rows are
-- set to the value they hold) and the key is added only if absent.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. Renumber, per class, in admission order.
--
-- ROW_NUMBER() needs MySQL 8.0+, which this schema already requires.
-- Assigned via a join on the derived table rather than a correlated
-- subquery so it is one pass over the rows.
-- ---------------------------------------------------------------------
UPDATE students s
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY class_id ORDER BY admission_number, id) AS seq
    FROM students
    WHERE is_deleted = 0
) numbered ON numbered.id = s.id
SET s.roll_number = numbered.seq;

-- ---------------------------------------------------------------------
-- 2. Enforce it.
--
-- Guarded so the file can be re-run: ADD UNIQUE KEY is not idempotent on
-- its own and would fail with "Duplicate key name" the second time.
-- ---------------------------------------------------------------------
SET @key_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'students'
      AND index_name = 'uq_students_class_roll'
);

SET @ddl := IF(@key_exists = 0,
    'ALTER TABLE students ADD UNIQUE KEY uq_students_class_roll (class_id, roll_number)',
    'SELECT ''uq_students_class_roll already present'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. Verification. Both counts must be 0.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS duplicate_rolls_within_a_class_should_be_0
FROM (
    SELECT class_id, roll_number
    FROM students
    WHERE is_deleted = 0 AND roll_number IS NOT NULL
    GROUP BY class_id, roll_number
    HAVING COUNT(*) > 1
) dupes;

SELECT COUNT(*) AS active_students_without_a_roll_should_be_0
FROM students WHERE is_deleted = 0 AND roll_number IS NULL;

-- Informational: the first few classes' numbering after the renumber.
SELECT class_id, MIN(roll_number) AS lowest, MAX(roll_number) AS highest, COUNT(*) AS students
FROM students WHERE is_deleted = 0
GROUP BY class_id ORDER BY class_id LIMIT 5;
