-- =====================================================================
-- 18_my_class_edit_only.sql
--
-- A class teacher may keep their own students' records up to date. They
-- may not admit a student.
--
-- 15_role_config_and_my_class.sql described MY_CLASS_ROSTER_MANAGE as
-- "Add and edit students within your own homeroom section", and
-- POST /api/v1/my-class/students backed the "add" half. That route is
-- gone: admitting a pupil creates a login, an admission number and a
-- guardian record and decides which class the child joins, none of which
-- belongs to a class teacher. Editing the records of students already in
-- their class is a different act, and the only one that was wanted.
--
-- This file corrects the description, which is what the Roles &
-- Permissions screen shows an administrator. Left alone it would promise
-- a capability the API no longer has.
--
-- The permission NAME is deliberately unchanged. Renaming it would mean
-- rewriting the role_permissions rows that reference it and would silently
-- discard any per-role configuration a school has already applied - a
-- migration hazard out of all proportion to a clearer noun. "ROSTER_MANAGE"
-- now means "manage the records on your roster", not "manage who is on it".
--
-- No grant changes. CLASS_TEACHER keeps MY_CLASS_ROSTER_MANAGE (for the
-- edit) and has never held STUDENT_CREATE (for the admission), so the
-- verification at the bottom should already pass on any database.
--
-- Re-running is safe.
-- =====================================================================

USE school_management_system;

UPDATE permissions
SET description = 'Edit the records of students already in your own homeroom section'
WHERE name = 'MY_CLASS_ROSTER_MANAGE';

-- ---------------------------------------------------------------------
-- Verification
--
-- The first query must return 0 rows: no teaching role may hold the
-- admission grant. If it returns anything, that role can add students
-- through StudentController.create and this change has been undone
-- somewhere.
-- ---------------------------------------------------------------------
SELECT r.name AS teaching_role_holding_student_create_should_be_empty
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'STUDENT_CREATE'
  AND r.name IN ('TEACHER', 'CLASS_TEACHER');

-- Who may admit a student, after this file. Expected: SUPER_ADMIN,
-- PRINCIPAL, VICE_PRINCIPAL. Revoke from the latter two if admissions
-- should be administrator-only.
SELECT r.name AS may_admit_students
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'STUDENT_CREATE'
ORDER BY r.name;

SELECT name, description AS my_class_roster_manage_now_reads
FROM permissions WHERE name = 'MY_CLASS_ROSTER_MANAGE';
