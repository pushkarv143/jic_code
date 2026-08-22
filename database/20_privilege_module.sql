-- =====================================================================
-- 20_privilege_module.sql
--
-- Gives menu administration a module of its own, held by SUPER_ADMIN
-- alone.
--
-- 19_menu_registry.sql put the menu in the database and hung its
-- endpoints off the ROLE permissions, on the reasoning that assigning a
-- menu and granting a permission are two halves of one job. That is
-- true of the *work*, but not of the *authority*: ROLE_VIEW and
-- ROLE_MANAGE are held by the principal so they can adjust what a
-- teacher may do, and deciding what appears on every role's navigation
-- is a narrower decision than that - it shapes what the whole
-- organisation sees, and getting it wrong empties somebody's sidebar.
--
-- So it becomes its own module with its own two permissions, granted
-- to SUPER_ADMIN and to nobody else. An organisation that wants to
-- delegate it can grant PRIVILEGE_MANAGE from the role editor; nothing
-- here prevents that, which is the point of the permission existing at
-- all rather than the endpoints being pinned to a role.
--
--   PRIVILEGE_VIEW   - read the menu catalogue and each role's assignment
--   PRIVILEGE_MANAGE - change a role's assignment
--
-- ON is_core
--
-- The module is marked core, so the registry refuses to switch it off.
-- The module gate is the one gate SUPER_ADMIN does not bypass, so
-- disabling PRIVILEGE would hide the Privileges menu from the only
-- account that can configure menus - and the way back would be the SQL
-- statement nobody thinks to run. Same reasoning that already protects
-- ROLE and SETTINGS.
--
-- ALSO FIXED HERE
--
-- MenuController was written against 'ROLE_READ', which is not a
-- permission that exists - the catalogue therefore answered 403 to a
-- principal holding ROLE_VIEW and only ever succeeded through the
-- SUPER_ADMIN override. Moving the endpoints onto PRIVILEGE_VIEW /
-- PRIVILEGE_MANAGE retires the mistake rather than patching it.
--
-- Re-running is safe: every statement is guarded by a NOT EXISTS on its
-- natural key.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. The permissions
-- ---------------------------------------------------------------------
INSERT INTO permissions (name, module, description)
SELECT 'PRIVILEGE_VIEW', 'PRIVILEGE',
       'Read the menu catalogue and which menus each role is assigned'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PRIVILEGE_VIEW');

INSERT INTO permissions (name, module, description)
SELECT 'PRIVILEGE_MANAGE', 'PRIVILEGE',
       'Change which menus a role is assigned'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PRIVILEGE_MANAGE');

-- ---------------------------------------------------------------------
-- 2. The module
-- ---------------------------------------------------------------------
INSERT INTO org_modules (module_key, label, description, is_enabled, is_core, sort_order)
SELECT 'PRIVILEGE', 'Privileges',
       'Which menus each role is offered', 1, 1, 25
WHERE NOT EXISTS (SELECT 1 FROM org_modules WHERE module_key = 'PRIVILEGE');

-- ---------------------------------------------------------------------
-- 3. Granted to SUPER_ADMIN, and to nobody else
--
-- Written as a join on the role name rather than a hard-coded id, and
-- deliberately not as "every role that already holds ROLE_MANAGE" - that
-- would quietly hand it to the principal, which is the thing this
-- migration exists to stop.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('PRIVILEGE_VIEW', 'PRIVILEGE_MANAGE')
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- 4. The menu entry
--
-- Under Account, next to Users and Settings, because that is where the
-- other configuration screens live. Gated on PRIVILEGE_VIEW and on the
-- PRIVILEGE module, so it disappears for a role that loses the grant
-- without anyone having to remember to unassign the menu too.
-- ---------------------------------------------------------------------
INSERT INTO menus (menu_key, label, path, icon, parent_id, module_key, required_permission,
                   requires_homeroom, i18n_key, sort_order)
SELECT 'PRIVILEGES', 'Privileges', '/app/privileges', 'AdminPanelSettingsOutlined',
       parent.id, 'PRIVILEGE', 'PRIVILEGE_VIEW', 0, 'nav.privileges', 25
FROM menus parent
WHERE parent.menu_key = 'SECTION_ACCOUNT'
  AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.menu_key = 'PRIVILEGES');

INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus m ON m.menu_key = 'PRIVILEGES'
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- ---------------------------------------------------------------------
-- 5. Verification
-- ---------------------------------------------------------------------

-- Who holds the new grants. Expect exactly one row: SUPER_ADMIN, 2.
SELECT r.name AS role_name, COUNT(*) AS privilege_grants
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.module = 'PRIVILEGE'
GROUP BY r.id, r.name
ORDER BY r.name;

-- Who is offered the menu. Expect exactly one row: SUPER_ADMIN.
SELECT r.name AS role_name
FROM role_menus rm
JOIN roles r ON r.id = rm.role_id
JOIN menus m ON m.id = rm.menu_id
WHERE m.menu_key = 'PRIVILEGES'
ORDER BY r.name;

-- The module must be core, or the only account that can configure menus
-- can lose the screen that does it. Expect is_core = 1.
SELECT module_key, label, is_enabled, is_core
FROM org_modules
WHERE module_key = 'PRIVILEGE';
