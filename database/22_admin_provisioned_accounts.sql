-- =====================================================================
-- 22_admin_provisioned_accounts.sql
--
-- Student accounts are created by the school, not by the student.
--
-- WHY
--
-- The login page offered "Create a Student / Parent Account", so anyone
-- who found the URL could create an account and wait to be approved.
-- That is the wrong direction for a school: the school already knows
-- who its students are, it admits them through the Add Student form,
-- and an account that exists before the admission does is an account
-- nobody asked for. Self-registration is removed, and admitting a
-- student now provisions their login as part of the same action.
--
-- WHAT THIS ADDS
--
-- users.must_change_password - true for an account still on the
-- password the system generated for it. While it is set, the API
-- refuses every request except changing the password and signing out,
-- so a generated password cannot be used to do anything except replace
-- itself.
--
-- Existing accounts are backfilled to 0. They chose their own
-- passwords, and forcing 502 students to reset one they already picked
-- would be a support incident rather than a security improvement.
--
-- WHAT IT DOES NOT ADD
--
-- No column for the generated password. It is emailed once and stored
-- only as a bcrypt hash like any other, so a lost one is reset through
-- the existing forgot-password flow rather than looked up.
--
-- ON PARENTS
--
-- The removed flow was labelled "Student / Parent", but `parents` and
-- `student_parents` are both empty and no user holds the PARENT role,
-- so no working parent login is being taken away. Parent provisioning
-- is deliberately not bundled in here: a guardian is currently a
-- contact record on `guardians` with no user link at all, so giving
-- guardians logins means deciding which guardian of a student gets one
-- and bridging guardians -> parents -> student_parents. That is a piece
-- of modelling in its own right, and the credential generator this
-- migration supports is written to be reused by it.
--
-- Re-running is safe.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. The flag
--
-- Guarded through information_schema: MySQL has no ADD COLUMN IF NOT
-- EXISTS, and a bare ALTER would fail the whole file on a second run.
-- ---------------------------------------------------------------------
SET @has_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'must_change_password'
);

SET @ddl := IF(@has_column = 0,
    'ALTER TABLE users ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER password',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. Verification
-- ---------------------------------------------------------------------

-- The column must exist. Expect 1.
SELECT COUNT(*) AS must_change_password_column
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'must_change_password';

-- Nobody is forced to reset on the day this runs: every existing
-- account chose its own password. Expect 0 in the middle column.
SELECT COUNT(*)                          AS users_total,
       SUM(must_change_password)         AS awaiting_first_login_reset,
       SUM(must_change_password = 0)     AS settled
FROM users;

-- Students without a login, for context rather than as a failure. These
-- were admitted before provisioning was automatic; each needs an
-- account creating from the admin screen, which now generates the
-- credentials and emails them.
SELECT COUNT(*) AS students_with_no_login
FROM students s
WHERE s.user_id IS NULL;

-- Students whose login has no email to deliver credentials to. A row
-- here is a record to correct before its account is regenerated.
SELECT COUNT(*) AS logins_without_an_email
FROM students s
JOIN users u ON u.id = s.user_id
WHERE u.email IS NULL OR u.email = '';
