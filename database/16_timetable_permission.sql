-- =====================================================================
-- 16_timetable_permission.sql
--
-- Gives the timetable its own permission, granted to SUPER_ADMIN alone.
--
-- Until now /api/v1/timetable/** writes were gated by the hardcoded list
-- hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL'), which had two
-- problems. It admitted three roles where the school wants one, and being
-- a role list rather than a grant it could not be reconfigured: revoking
-- something on the Roles & Permissions screen would hide the controls and
-- leave the API still accepting the requests.
--
-- TIMETABLE_MANAGE fixes both. It is held by SUPER_ADMIN only, so
-- assembling the week is the administrator's job by default, and because
-- it is an ordinary row in role_permissions a school that wants its
-- principal to help can grant it without a code change.
--
-- Filed under the ACADEMIC module rather than a new one: the timetable is
-- part of academics, and ACADEMIC is already core, so this permission
-- cannot be switched off by disabling a module out from under it.
--
-- Reading a timetable is deliberately untouched. Those endpoints have
-- their own rules already - a teacher reads their own week on
-- /timetable/me, a student theirs, and the class-wide views stay
-- management-only - and none of them is a write.
--
-- Re-running is safe.
-- =====================================================================

USE school_management_system;

INSERT INTO permissions (name, module, description)
SELECT 'TIMETABLE_MANAGE', 'ACADEMIC', 'Assemble and edit a class timetable'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'TIMETABLE_MANAGE');

-- SUPER_ADMIN only. PRINCIPAL and VICE_PRINCIPAL are deliberately omitted:
-- they held this through the old role list, and narrowing it is the point
-- of this migration. Grant it to them here if that is not what you want.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'TIMETABLE_MANAGE'
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- Verification: exactly one role should hold it, and that role is
-- SUPER_ADMIN.
-- ---------------------------------------------------------------------
SELECT r.name AS role_holding_timetable_manage
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'TIMETABLE_MANAGE'
ORDER BY r.name;
