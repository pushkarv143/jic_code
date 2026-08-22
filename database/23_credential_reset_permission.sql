-- =====================================================================
-- 23_credential_reset_permission.sql
--
-- Resending a student's login credentials, held by SUPER_ADMIN alone.
--
-- WHY A PERMISSION AND NOT hasRole('SUPER_ADMIN')
--
-- The requirement is "Super Admin only", and a role check would say
-- exactly that in one line. It is a permission instead for the same
-- reason PRIVILEGE_MANAGE is: every other authority in this system
-- became configurable, and an endpoint pinned to a role name is the one
-- an organisation cannot delegate without a code change. Granted to
-- SUPER_ADMIN and to nobody else, so the behaviour today is identical
-- to the role check - the difference only shows up the day a school
-- wants their office manager to be able to do it.
--
-- WHAT THE PERMISSION GUARDS
--
-- Regenerating a student's temporary password, marking the account as
-- needing a first-login reset again, revoking its sessions, and sending
-- the username and new password by email and SMS. That is a credential
-- reset performed on somebody else's account, which is why it is
-- separate from STUDENT_UPDATE: a clerk correcting a spelling should
-- not also be able to lock a student out and mail their password.
--
-- The username is deliberately NOT regenerated. It is the student's
-- identity - on their timetable, in their parents' notes - and a lost
-- password says nothing about it. Regenerating would also collide with
-- the existing row and suffix a counter, so "pushkarv" would come back
-- as "pushkarv1" and read as a different person.
--
-- Module STUDENT, so switching that module off withdraws this with the
-- rest of it.
--
-- Re-running is safe.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. The permission
-- ---------------------------------------------------------------------
INSERT INTO permissions (name, module, description)
SELECT 'STUDENT_CREDENTIALS_RESET', 'STUDENT',
       'Regenerate a student''s temporary password and resend their credentials'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'STUDENT_CREDENTIALS_RESET');

-- ---------------------------------------------------------------------
-- 2. Granted to SUPER_ADMIN, and to nobody else
--
-- Written as a join on the role name rather than an id, and deliberately
-- not as "everyone holding STUDENT_UPDATE" - that would hand it to the
-- principal and the vice-principal, which is the thing this separation
-- exists to avoid.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'STUDENT_CREDENTIALS_RESET'
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- 3. Verification
-- ---------------------------------------------------------------------

-- Who holds it. Expect exactly one row: SUPER_ADMIN.
SELECT r.name AS role_name
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'STUDENT_CREDENTIALS_RESET'
ORDER BY r.name;

-- The permission must exist and sit in the STUDENT module. Expect 1 row.
SELECT name, module, description FROM permissions WHERE name = 'STUDENT_CREDENTIALS_RESET';

-- Students whose account has no phone number, for context. SMS is
-- best-effort - a missing number means the email still goes - but these
-- are the records where "and SMS" quietly means "email only".
SELECT COUNT(*) AS students_with_no_phone
FROM students s
JOIN users u ON u.id = s.user_id
WHERE (s.phone IS NULL OR s.phone = '') AND (u.phone IS NULL OR u.phone = '');
