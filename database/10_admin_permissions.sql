-- =====================================================================
-- 10_admin_permissions.sql
--
-- Reconciles role_permissions with the authorization the controllers
-- actually enforce, and guarantees SUPER_ADMIN holds every permission.
--
-- Why this file exists. Two independent things decide what a user may do:
--   * the role gate on the endpoint (@PreAuthorize hasAnyRole(...)), and
--   * the permission grants in role_permissions, which the clients use to
--     decide which buttons to render.
-- When those two disagree the symptom is confusing rather than dangerous:
-- the UI hides an action the API would happily accept. Three such
-- disagreements existed:
--
--   1. PRINCIPAL / VICE_PRINCIPAL held MATERIAL_VIEW but not
--      MATERIAL_MANAGE, while StudyMaterialServiceImpl grants management
--      the right to edit any material and is the *only* role allowed to
--      share one with a whole class. The controller's
--      hasAuthority('PERM_MATERIAL_MANAGE') gate blocked them, which made
--      the class-wide share path unreachable by anyone at all.
--   2. VICE_PRINCIPAL is in StudentController's WRITE_ROLES but held
--      neither STUDENT_CREATE nor STUDENT_DELETE, so the Add/Delete
--      buttons were hidden from a role the API accepts.
--   3. The same for TeacherController.
--
-- Re-runnable: every grant is inserted only when absent.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- SUPER_ADMIN holds every permission, including any added after the
-- original seed ran.
--
-- Written as "every permission not already granted" rather than a fixed
-- list on purpose: a module added later only has to insert its
-- permission rows, and re-running this file picks them up. Without it,
-- a new permission silently leaves the administrator unable to use a
-- feature they own.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- PRINCIPAL: add the grants whose endpoints already admit them.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
  'MATERIAL_VIEW','MATERIAL_MANAGE',
  'ASSIGNMENT_VIEW','ASSIGNMENT_CREATE','ASSIGNMENT_GRADE'
)
WHERE r.name = 'PRINCIPAL'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- VICE_PRINCIPAL: a full peer on students, teachers and academics, which
-- is what the endpoints already allow. Deliberately still excluded from
-- the administrative side - no USER_*/ROLE_*, no ADMISSION_MANAGE, no
-- PAYROLL_*, no SETTINGS_MANAGE - so this is a reconciliation, not a
-- promotion to administrator.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
  'STUDENT_CREATE','STUDENT_DELETE',
  'TEACHER_CREATE','TEACHER_UPDATE','TEACHER_DELETE',
  'CLASS_MANAGE','SECTION_MANAGE','SUBJECT_MANAGE',
  'ATTENDANCE_MARK',
  'MARKS_ENTRY',
  'ASSIGNMENT_CREATE','ASSIGNMENT_GRADE',
  'MATERIAL_VIEW','MATERIAL_MANAGE'
)
WHERE r.name = 'VICE_PRINCIPAL'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- Verification. The first query must return 0: any row means SUPER_ADMIN
-- is missing a permission, which is exactly the condition this file
-- exists to prevent.
-- ---------------------------------------------------------------------
SELECT 'Permissions SUPER_ADMIN is missing (must be 0)' AS check_name,
       COUNT(*) AS failing_rows
FROM permissions p
WHERE NOT EXISTS (
  SELECT 1 FROM role_permissions rp
  JOIN roles r ON r.id = rp.role_id
  WHERE r.name = 'SUPER_ADMIN' AND rp.permission_id = p.id
);

SELECT r.name AS role_name, COUNT(rp.permission_id) AS granted_permissions
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
GROUP BY r.name
ORDER BY granted_permissions DESC;
