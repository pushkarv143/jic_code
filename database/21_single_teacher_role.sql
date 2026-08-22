-- =====================================================================
-- 21_single_teacher_role.sql
--
-- Collapses TEACHER and CLASS_TEACHER into one role, and makes "is also
-- a class teacher" an attribute of the teacher rather than a role of
-- its own.
--
-- WHY
--
-- The two roles were never two kinds of person. CLASS_TEACHER held
-- exactly TEACHER's permissions plus six, so it was TEACHER-plus-a-bit
-- modelled as a separate identity - and the identity did not match
-- reality. 44 users carried the CLASS_TEACHER role while only 17
-- actually appeared in sections.class_teacher_id, so 27 people were
-- being granted homeroom privileges over a homeroom they did not have,
-- and the 7 users on the plain TEACHER role could never be given one
-- without an administrator also remembering to change their role.
--
-- Being a class teacher is a duty a teacher picks up and puts down at
-- the start of a year. A role is the wrong shape for it.
--
-- WHAT REPLACES IT
--
-- 1. teachers.is_class_teacher - the flag. Server-maintained: it is set
--    when a teacher is assigned to sections.class_teacher_id and
--    cleared when they hold no section any more. Deliberately not
--    hand-editable, because two sources of truth for the same fact is
--    how they drift; sections.class_teacher_id stays authoritative for
--    *which* section, and this column answers the cheaper question
--    "any at all?" that the login path needs on every request.
--
-- 2. class_teacher_permissions - the six permissions the flag grants,
--    as data rather than a constant in Java. When the flag is true the
--    login path adds these to whatever the TEACHER role holds; when it
--    is false the teacher has plain TEACHER permissions and nothing
--    more. The base role never changes.
--
--       ATTENDANCE_REPORT          - the register they take
--       MY_CLASS_VIEW              - their own section
--       MY_CLASS_ROSTER_MANAGE     - edit students already in it
--       MY_CLASS_OFFICIALS_MANAGE  - appoint head boy/girl within it
--       NOTICE_PUBLISH             - notices to their own class
--       STUDENT_UPDATE             - correct a record on their roster
--
--    These are exactly the six CLASS_TEACHER held and TEACHER did not,
--    computed rather than typed: the seed below reads them out of
--    role_permissions before the role is removed, so this migration
--    cannot disagree with the state it is replacing.
--
-- 3. Every CLASS_TEACHER user moves to TEACHER, and the CLASS_TEACHER
--    role row is deleted. Its role_permissions and role_menus rows go
--    with it by cascade; the two roles were assigned identical menus,
--    so no menu assignment changes.
--
-- EFFECT ON THE 44
--
-- The 17 who hold a section keep every permission they had. The 27 who
-- do not lose the six - which is the correction this migration exists
-- to make, not a regression. Nothing they could legitimately reach is
-- withdrawn: the endpoints behind those six are all scoped to the
-- caller's own homeroom section server-side, so for a teacher without
-- one they returned 403 already.
--
-- Re-running is safe, and re-running is also the way to re-sync the
-- flag if it ever drifts from sections.class_teacher_id.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. The flag
--
-- Guarded by information_schema rather than a bare ALTER: MySQL has no
-- ADD COLUMN IF NOT EXISTS, and a second run would otherwise fail here
-- and leave the rest of the migration unapplied.
-- ---------------------------------------------------------------------
SET @has_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'teachers' AND column_name = 'is_class_teacher'
);

SET @ddl := IF(@has_column = 0,
    'ALTER TABLE teachers ADD COLUMN is_class_teacher TINYINT(1) NOT NULL DEFAULT 0 AFTER designation_id',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Re-synced on every run, so this file doubles as the repair for drift.
UPDATE teachers t
SET t.is_class_teacher = EXISTS (SELECT 1 FROM sections s WHERE s.class_teacher_id = t.id);

-- ---------------------------------------------------------------------
-- 2. The permissions the flag grants
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_teacher_permissions (
    permission_id BIGINT    NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_id),
    CONSTRAINT fk_ctp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Computed from the roles being merged, not typed out: whatever
-- CLASS_TEACHER held and TEACHER did not is exactly what the flag must
-- now grant. Runs before the role is deleted, and is a no-op on a second
-- run when the role is already gone.
INSERT INTO class_teacher_permissions (permission_id)
SELECT DISTINCT rp.permission_id
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id AND r.name = 'CLASS_TEACHER'
WHERE rp.permission_id NOT IN (
        SELECT rp2.permission_id FROM role_permissions rp2
        JOIN roles r2 ON r2.id = rp2.role_id AND r2.name = 'TEACHER')
  AND rp.permission_id NOT IN (SELECT permission_id FROM class_teacher_permissions);

-- Belt and braces for a fresh install, where 06_seed_reference_data.sql
-- may have seeded the two roles identically and the delta above comes
-- out empty. Named explicitly, still guarded, so the flag is never
-- left granting nothing.
INSERT INTO class_teacher_permissions (permission_id)
SELECT p.id FROM permissions p
WHERE p.name IN (
        'ATTENDANCE_REPORT',
        'MY_CLASS_VIEW',
        'MY_CLASS_ROSTER_MANAGE',
        'MY_CLASS_OFFICIALS_MANAGE',
        'NOTICE_PUBLISH',
        'STUDENT_UPDATE')
  AND p.id NOT IN (SELECT permission_id FROM class_teacher_permissions);

-- ---------------------------------------------------------------------
-- 3. Move the users, then retire the role
--
-- Order matters: users.role_id is NOT NULL with a foreign key, so the
-- role cannot be deleted while anyone still points at it.
-- ---------------------------------------------------------------------
UPDATE users u
JOIN roles teacher ON teacher.name = 'TEACHER'
JOIN roles class_teacher ON class_teacher.name = 'CLASS_TEACHER'
SET u.role_id = teacher.id
WHERE u.role_id = class_teacher.id;

-- role_permissions and role_menus cascade from roles.id, so both go with
-- it. The two roles carried identical menu assignments, so nothing a
-- teacher sees changes.
DELETE FROM roles WHERE name = 'CLASS_TEACHER';

-- ---------------------------------------------------------------------
-- 4. Verification
-- ---------------------------------------------------------------------

-- The role must be gone. Expect 0.
SELECT COUNT(*) AS class_teacher_role_rows FROM roles WHERE name = 'CLASS_TEACHER';

-- Nobody may be left pointing at a role that no longer exists. Expect 0
-- (the foreign key guarantees it, but a migration that trusts the schema
-- to have been applied is a migration that fails in the field).
SELECT COUNT(*) AS orphaned_users FROM users u
LEFT JOIN roles r ON r.id = u.role_id WHERE r.id IS NULL;

-- The flag against the assignment it mirrors. Both numbers must match:
-- 17 teachers hold a section, so 17 carry the flag.
SELECT (SELECT COUNT(*) FROM teachers WHERE is_class_teacher = 1)                  AS flagged,
       (SELECT COUNT(DISTINCT class_teacher_id) FROM sections
        WHERE class_teacher_id IS NOT NULL)                                        AS holding_a_section,
       (SELECT COUNT(*) FROM teachers)                                             AS teachers_total;

-- Any teacher whose flag disagrees with their section assignment. Expect
-- zero rows; a row here is drift, and re-running this file repairs it.
SELECT t.id, t.employee_id, t.is_class_teacher,
       EXISTS (SELECT 1 FROM sections s WHERE s.class_teacher_id = t.id) AS holds_section
FROM teachers t
WHERE t.is_class_teacher <> EXISTS (SELECT 1 FROM sections s WHERE s.class_teacher_id = t.id);

-- What the flag grants. Expect the six named above.
SELECT p.name, p.module
FROM class_teacher_permissions ctp
JOIN permissions p ON p.id = ctp.permission_id
ORDER BY p.module, p.name;

-- How many teachers now sit on the single role, and how many of them
-- carry the flag.
SELECT r.name                                   AS role_name,
       COUNT(*)                                 AS users,
       SUM(COALESCE(t.is_class_teacher, 0))     AS with_class_teacher_flag
FROM users u
JOIN roles r ON r.id = u.role_id
LEFT JOIN teachers t ON t.user_id = u.id
WHERE r.name = 'TEACHER'
GROUP BY r.id, r.name;
