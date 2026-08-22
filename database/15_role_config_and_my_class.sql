-- =====================================================================
-- 15_role_config_and_my_class.sql
--
-- Two changes, both in service of making authorization configurable per
-- organisation rather than baked into seed SQL and TypeScript arrays.
--
-- 1. org_modules - a registry of the functional modules this deployment
--    runs. Rows key off permissions.module, which already groups the
--    permission catalogue ('STUDENT', 'FEE', 'HOSTEL', ...). Turning a
--    module off removes every permission in that module from the
--    effective grant set, which in turn hides the menu entries and the
--    in-page actions that depend on them. One row, whole module dark.
--
--    is_core marks the modules that must never be switched off. Without
--    it an administrator can disable ROLE or SETTINGS and lock the
--    installation into its current configuration permanently - the only
--    way back would be the SQL statement nobody thinks to run. The API
--    refuses to disable a core module; this column is where that list
--    lives so the rule is data, not a hard-coded constant.
--
-- 2. The MY_CLASS module and its three permissions, backing the homeroom
--    teacher's own view of their section.
--
--    MY_CLASS_VIEW             - see the section they are homeroom of
--    MY_CLASS_ROSTER_MANAGE    - add/edit students within it
--    MY_CLASS_OFFICIALS_MANAGE - appoint head boy/girl/monitor within it
--
--    These are deliberately NOT the global STUDENT_CREATE /
--    STUDENT_UPDATE grants. A homeroom teacher needs to add a student to
--    *their own* section, and granting STUDENT_CREATE would let them
--    POST /api/v1/students for any section in the school. The narrow
--    permission plus a homeroom-scoped endpoint gives the capability
--    without the reach.
--
--    Note that holding MY_CLASS_VIEW is necessary but not sufficient:
--    the module is only reachable by a teacher who actually appears in
--    sections.class_teacher_id. 44 users currently hold the
--    CLASS_TEACHER role while only 17 are homeroom of anything, so the
--    role alone was never a usable signal. See HomeroomGuard.
--
-- Re-running is safe: every statement below is guarded by a uniqueness
-- check or IF NOT EXISTS.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. Module registry
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS org_modules (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  module_key   VARCHAR(50)  NOT NULL,
  label        VARCHAR(100) NOT NULL,
  description  VARCHAR(255),
  is_enabled   TINYINT(1) NOT NULL DEFAULT 1,
  -- Core modules cannot be disabled through the API; see the header.
  is_core      TINYINT(1) NOT NULL DEFAULT 0,
  sort_order   INT NOT NULL DEFAULT 100,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_org_modules_key (module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. MY_CLASS permissions
--
-- Inserted before the module registry is seeded, so the catch-all seed
-- below picks up the new 'MY_CLASS' module without naming it twice.
-- ---------------------------------------------------------------------
INSERT INTO permissions (name, module, description)
SELECT 'MY_CLASS_VIEW', 'MY_CLASS', 'View the section you are homeroom teacher of'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MY_CLASS_VIEW');

INSERT INTO permissions (name, module, description)
SELECT 'MY_CLASS_ROSTER_MANAGE', 'MY_CLASS', 'Add and edit students within your own homeroom section'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MY_CLASS_ROSTER_MANAGE');

-- Appointing head boy / head girl / monitor within your own section.
--
-- Separate from MY_CLASS_ROSTER_MANAGE because it is a different kind of act:
-- an organisation may well want the class teacher to maintain student records
-- but reserve naming a head boy for the principal, or the reverse. Two
-- permissions make that a configuration choice instead of a code change.
--
-- Note that the school-wide equivalent on
-- /api/v1/classes/{classId}/officials stays management-only. This grant reaches
-- the caller's own section and nothing else.
INSERT INTO permissions (name, module, description)
SELECT 'MY_CLASS_OFFICIALS_MANAGE', 'MY_CLASS', 'Appoint or end class posts within your own homeroom section'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MY_CLASS_OFFICIALS_MANAGE');

-- ---------------------------------------------------------------------
-- 3. Seed the registry
--
-- Explicit rows first, for the label and ordering a human wants to read;
-- then a catch-all that adds any module present in the permission
-- catalogue but missing here. The catch-all is what keeps this file
-- correct when a later migration introduces a new module and forgets to
-- register it - the module shows up disableable with its raw key as the
-- label rather than being silently unmanageable.
-- ---------------------------------------------------------------------
INSERT INTO org_modules (module_key, label, description, is_enabled, is_core, sort_order)
SELECT * FROM (
  SELECT 'USER'          AS k, 'Users & Accounts'      AS l, 'User accounts and logins'                    AS d, 1 AS e, 1 AS c, 10 AS s UNION ALL
  SELECT 'ROLE',            'Roles & Permissions',    'Role definitions and permission grants',          1, 1, 20 UNION ALL
  SELECT 'SETTINGS',        'Settings',               'School profile and system settings',              1, 1, 30 UNION ALL
  SELECT 'STUDENT',         'Students',               'Student records and directory',                   1, 1, 40 UNION ALL
  SELECT 'TEACHER',         'Teachers',               'Teacher records and directory',                   1, 1, 50 UNION ALL
  SELECT 'STAFF',           'Staff',                  'Non-teaching staff records',                      1, 0, 60 UNION ALL
  SELECT 'ACADEMIC',        'Classes & Subjects',     'Classes, sections, subjects and mapping',         1, 1, 70 UNION ALL
  SELECT 'MY_CLASS',        'My Class',               'Homeroom teacher view of their own section',      1, 0, 80 UNION ALL
  SELECT 'ATTENDANCE',      'Attendance',             'Daily attendance and reports',                    1, 0, 90 UNION ALL
  SELECT 'EXAM',            'Exams & Marks',          'Exams, schedules and marks entry',                1, 0, 100 UNION ALL
  SELECT 'ASSIGNMENT',      'Assignments',            'Assignments and submissions',                     1, 0, 110 UNION ALL
  SELECT 'MATERIAL',        'Study Materials',        'Study material uploads and downloads',            1, 0, 120 UNION ALL
  SELECT 'NOTICE',          'Notices',                'Notice board',                                    1, 0, 130 UNION ALL
  SELECT 'FEE',             'Fees',                   'Fee structures, collection and reports',          1, 0, 140 UNION ALL
  SELECT 'PAYROLL',         'Payroll',                'Salary structures and payroll runs',              1, 0, 150 UNION ALL
  SELECT 'LIBRARY',         'Library',                'Book catalogue and issue/return',                 1, 0, 160 UNION ALL
  SELECT 'TRANSPORT',       'Transport',              'Routes, buses and student assignments',           1, 0, 170 UNION ALL
  SELECT 'HOSTEL',          'Hostel',                 'Hostel rooms, allocations and visitors',          1, 0, 180 UNION ALL
  SELECT 'ADMISSION',       'Admissions',             'Admission enquiries and follow-up',               1, 0, 190 UNION ALL
  SELECT 'COMMUNICATION',   'Communication',          'Notifications and messaging',                     1, 0, 200 UNION ALL
  SELECT 'REPORTS',         'Reports',                'Cross-module reporting',                          1, 0, 210
) seed
WHERE NOT EXISTS (SELECT 1 FROM org_modules m WHERE m.module_key = seed.k);

-- Catch-all: any module in the permission catalogue that the list above
-- does not name. Enabled and non-core, labelled with its own key.
INSERT INTO org_modules (module_key, label, description, is_enabled, is_core, sort_order)
SELECT DISTINCT p.module, p.module, CONCAT('Auto-registered from the permission catalogue'), 1, 0, 900
FROM permissions p
WHERE NOT EXISTS (SELECT 1 FROM org_modules m WHERE m.module_key = p.module);

-- ---------------------------------------------------------------------
-- 4. Grant the MY_CLASS permissions
--
-- CLASS_TEACHER is the role the module exists for. SUPER_ADMIN gets them
-- the same way 10_admin_permissions.sql grants everything else - the
-- administrator is never gated by a missing grant row.
--
-- PRINCIPAL and VICE_PRINCIPAL deliberately do NOT get these: they reach
-- every section through the full Classes module, and a "my class" view
-- means nothing for someone who is homeroom of nothing.
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('MY_CLASS_VIEW', 'MY_CLASS_ROSTER_MANAGE', 'MY_CLASS_OFFICIALS_MANAGE')
WHERE r.name = 'CLASS_TEACHER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('MY_CLASS_VIEW', 'MY_CLASS_ROSTER_MANAGE', 'MY_CLASS_OFFICIALS_MANAGE')
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- 5. Verification
--
-- The first query must return 0. The second lists what a class teacher
-- now holds in the MY_CLASS module, and the third is the role-versus-
-- reality gap that motivated the homeroom guard - informational, and
-- expected to be non-zero on a seeded database.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS core_modules_disabled_should_be_0
FROM org_modules WHERE is_core = 1 AND is_enabled = 0;

SELECT r.name AS role, p.name AS permission
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.module = 'MY_CLASS'
ORDER BY r.name, p.name;

SELECT COUNT(*) AS class_teacher_role_holders_without_homeroom
FROM users u
JOIN roles r ON r.id = u.role_id
LEFT JOIN teachers t ON t.user_id = u.id
WHERE r.name = 'CLASS_TEACHER'
  AND (t.id IS NULL OR NOT EXISTS (SELECT 1 FROM sections s WHERE s.class_teacher_id = t.id));
