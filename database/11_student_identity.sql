-- =====================================================================
-- 11_student_identity.sql
--
-- Gives a student a name of their own.
--
-- Until now first_name/last_name existed only on `users`, and
-- students.user_id is nullable because a login is optional at admission.
-- The consequence was that a student admitted without a login had no name
-- anywhere: /api/v1/students/1 returned firstName/lastName/email/phone all
-- null, the edit form rendered blank, and the UI fell back to displaying
-- the guardian's name in the student's place ("Manoj Pal (Guardian)").
--
-- A student's name is an attribute of the student, not of an account they
-- may never have, so it belongs on this table. Where a login does exist
-- the two are kept in sync by the application.
--
-- Re-runnable: guarded on information_schema, so applying twice is a no-op.
-- =====================================================================

USE school_management_system;

DROP PROCEDURE IF EXISTS sp_add_student_identity;
DELIMITER $$
CREATE PROCEDURE sp_add_student_identity()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'first_name'
  ) THEN
    ALTER TABLE students
      ADD COLUMN first_name VARCHAR(100) NULL AFTER user_id,
      ADD COLUMN last_name  VARCHAR(100) NULL AFTER first_name,
      ADD COLUMN email      VARCHAR(150) NULL AFTER last_name,
      ADD COLUMN phone      VARCHAR(20)  NULL AFTER email;
  END IF;

  -- Students already carrying a name on their linked account keep it: copy
  -- across rather than leaving two sources of truth to drift apart.
  UPDATE students s
    JOIN users u ON u.id = s.user_id
     SET s.first_name = COALESCE(s.first_name, u.first_name),
         s.last_name  = COALESCE(s.last_name,  u.last_name),
         s.email      = COALESCE(s.email,      u.email),
         s.phone      = COALESCE(s.phone,      u.phone)
   WHERE s.user_id IS NOT NULL;

  -- Students with no login have no name to recover. Seed a readable
  -- placeholder from the admission number so lists stay usable until an
  -- administrator edits them; this is visibly a placeholder rather than a
  -- guess at a real name.
  UPDATE students
     SET first_name = CONCAT('Student ', admission_number)
   WHERE (first_name IS NULL OR first_name = '')
     AND is_deleted = 0;

  -- Exactly one primary guardian per student. Rows exist today with every
  -- guardian flagged 0, which is why primaryGuardianName was being filled
  -- from whichever row happened to come back first.
  UPDATE guardians g
    JOIN (
      SELECT student_id, MIN(id) AS first_id
        FROM guardians
       GROUP BY student_id
      HAVING SUM(is_primary) = 0
    ) fix ON fix.first_id = g.id
     SET g.is_primary = 1;
END $$
DELIMITER ;

CALL sp_add_student_identity();
DROP PROCEDURE IF EXISTS sp_add_student_identity;

-- ---------------------------------------------------------------------
-- Verification. students_without_name must be 0.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS students_without_name
  FROM students WHERE is_deleted = 0 AND (first_name IS NULL OR first_name = '');

SELECT COUNT(*) AS students_without_primary_guardian
  FROM students s
 WHERE s.is_deleted = 0
   AND EXISTS (SELECT 1 FROM guardians g WHERE g.student_id = s.id)
   AND NOT EXISTS (SELECT 1 FROM guardians g WHERE g.student_id = s.id AND g.is_primary = 1);

SELECT id, admission_number, first_name, last_name, roll_number FROM students ORDER BY id;
