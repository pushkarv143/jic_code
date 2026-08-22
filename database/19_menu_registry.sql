-- =====================================================================
-- 19_menu_registry.sql
--
-- Moves the navigation menu out of the clients and into the database.
--
-- Until now the menu existed twice: NAV_GROUPS in the web app's
-- navConfig.tsx and MENU_ENTRIES in the Android app's Destinations.kt,
-- each with its own hard-coded `roles` array. Adding a menu meant
-- editing two files in two languages, releasing two clients, and hoping
-- the two arrays agreed. Changing who may see one meant a code change
-- and a deploy - so in practice nobody changed it, and every role saw
-- roughly the same menu whether the entries meant anything to them or
-- not.
--
-- Two tables replace that:
--
-- 1. menus - one row per navigation destination, plus one row per
--    section heading. Self-referencing through parent_id, so a heading
--    is a menu with children and a destination is a menu with a path.
--    That shape is what lets a sub-menu be added later (Payroll > Runs,
--    Payroll > Salary Structures) as data rather than as a release.
--
-- 2. role_menus - which roles see which menus. A join table, no extra
--    columns: presence is the grant and absence is the denial. There is
--    deliberately no "hidden" flag on menus per role, because two ways
--    to say no is one way too many.
--
-- WHAT THIS DOES NOT REPLACE
--
-- A menu row is permission to *see an entry*, never permission to *do
-- anything*. The three gates that already exist still apply on top,
-- and in this order:
--
--   module_key          - is this area switched on for the organisation
--                         at all? (org_modules; overrides everyone,
--                         including SUPER_ADMIN)
--   requires_homeroom   - My Class, for a teacher who actually holds a
--                         section in sections.class_teacher_id
--   required_permission - the role_permissions grant behind the screen
--
-- So role_menus can only ever *narrow* what a role sees. Granting a
-- menu to a role that lacks the permission behind it shows nothing:
-- the entry is still filtered out, and the API would refuse the call
-- regardless. This is why the seed below assigns permissions-backed
-- entries only to roles that hold the permission - a menu the user can
-- click and get a 403 from is worse than no menu.
--
-- ON THE SEED
--
-- The role assignments are lifted from the `roles` arrays that were in
-- navConfig.tsx, so this migration changes no user's menu on the day it
-- runs. What changes is that the assignment is now editable. Six
-- entries have no role restriction today and are seeded to every role,
-- because every role genuinely uses them: Dashboard, Leave (everyone
-- applies for leave), Notice Board, Calendar, Notifications and My
-- Profile.
--
-- Everything else is assigned only where it means something. A
-- LIBRARIAN gets Library and the six universal entries and nothing
-- else - no Students, no Fees, no Exams. A SECURITY_GUARD gets only the
-- universal six. That is the point of the exercise.
--
-- Re-running is safe: CREATE TABLE IF NOT EXISTS, and every INSERT is
-- guarded by a NOT EXISTS on its natural key.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- 1. The menu catalogue
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS menus (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    -- Stable identifier the clients match on. The label can be renamed
    -- and the path can move without breaking either app; this cannot,
    -- which is why it and not the label is what code refers to.
    menu_key            VARCHAR(64)  NOT NULL,
    label               VARCHAR(100) NOT NULL,
    -- NULL for a section heading, which is a grouping and not a
    -- destination. A row with no path and no children shows nothing.
    path                VARCHAR(150) NULL,
    -- Icon name, resolved per client (MUI icon on web, Compose icon on
    -- Android). Names rather than assets so one string serves both.
    icon                VARCHAR(64)  NULL,
    parent_id           BIGINT       NULL,
    -- -> org_modules.module_key. NULL means the entry belongs to no
    -- module and survives any configuration.
    module_key          VARCHAR(50)  NULL,
    -- -> permissions.name. NULL means the entry needs no grant of its
    -- own beyond being signed in.
    required_permission VARCHAR(100) NULL,
    requires_homeroom   TINYINT(1)   NOT NULL DEFAULT 0,
    -- Dot-path into the clients' translation tables, e.g. 'nav.students'.
    -- Two levels, matching the web client's dictionary shape - a deeper path has
    -- nowhere to resolve to. An unresolvable key renders as the key itself, so a
    -- typo here shows up as 'nav.wossname' in the sidebar rather than silently.
    i18n_key            VARCHAR(100) NULL,
    sort_order          INT          NOT NULL DEFAULT 0,
    -- Switches an entry off for every role at once, without deleting the
    -- row or its role assignments. For retiring a screen.
    is_enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_menus_key (menu_key),
    KEY idx_menus_parent (parent_id),
    -- A heading is deleted only with its children, never leaving them
    -- orphaned at the top level where they would render as a flat list.
    CONSTRAINT fk_menus_parent FOREIGN KEY (parent_id) REFERENCES menus (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. Role -> menu
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_menus (
    role_id    BIGINT    NOT NULL,
    menu_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_menus_menu (menu_id),
    CONSTRAINT fk_role_menus_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menus_menu FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. Section headings
--
-- sort_order in tens throughout, so an entry can be slotted between two
-- others without renumbering the rest.
-- ---------------------------------------------------------------------
INSERT INTO menus (menu_key, label, path, icon, parent_id, module_key, required_permission,
                   requires_homeroom, i18n_key, sort_order)
SELECT seed.k, seed.label, NULL, NULL, NULL, NULL, NULL, 0, seed.i18n, seed.ord
FROM (
    SELECT 'SECTION_OVERVIEW'       AS k, 'Overview'       AS label, 'nav.sectionOverview'       AS i18n, 10 AS ord
    UNION ALL SELECT 'SECTION_ACADEMICS',      'Academics',      'nav.sectionAcademics',      20
    UNION ALL SELECT 'SECTION_ADMINISTRATION', 'Administration', 'nav.sectionAdministration', 30
    UNION ALL SELECT 'SECTION_COMMUNICATION',  'Communication',  'nav.sectionCommunication',  40
    UNION ALL SELECT 'SECTION_INSIGHTS',       'Insights',       'nav.sectionInsights',       50
    UNION ALL SELECT 'SECTION_ACCOUNT',        'Account',        'nav.sectionAccount',        60
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM menus m WHERE m.menu_key = seed.k);

-- ---------------------------------------------------------------------
-- 4. The destinations
--
-- module_key, required_permission and requires_homeroom are copied from
-- the navConfig entries they replace, so the effective menu is
-- unchanged on the day this runs.
-- ---------------------------------------------------------------------
INSERT INTO menus (menu_key, label, path, icon, parent_id, module_key, required_permission,
                   requires_homeroom, i18n_key, sort_order)
SELECT seed.k, seed.label, seed.path, seed.icon, parent.id, seed.module_key,
       seed.perm, seed.homeroom, seed.i18n, seed.ord
FROM (
    -- Overview
    SELECT 'DASHBOARD' AS k, 'Dashboard' AS label, '/app/dashboard' AS path,
           'DashboardOutlined' AS icon, 'SECTION_OVERVIEW' AS parent_key,
           NULL AS module_key, NULL AS perm, 0 AS homeroom,
           'nav.dashboard' AS i18n, 10 AS ord
    UNION ALL SELECT 'MY_CHILDREN', 'My Children', '/app/parent',
           'FamilyRestroomOutlined', 'SECTION_OVERVIEW', NULL, NULL, 0, 'nav.myChildren', 20

    -- Academics
    UNION ALL SELECT 'STUDENTS', 'Students', '/app/students',
           'SchoolOutlined', 'SECTION_ACADEMICS', 'STUDENT', 'STUDENT_VIEW', 0, 'nav.students', 10
    UNION ALL SELECT 'TEACHERS', 'Teachers', '/app/teachers',
           'BadgeOutlined', 'SECTION_ACADEMICS', 'TEACHER', 'TEACHER_VIEW', 0, 'nav.teachers', 20
    -- Above Classes & Subjects on purpose: for a class teacher this is the
    -- screen they want - theirs, and editable.
    UNION ALL SELECT 'MY_CLASS', 'My Class', '/app/my-class',
           'ClassOutlined', 'SECTION_ACADEMICS', 'MY_CLASS', 'MY_CLASS_VIEW', 1, 'nav.myClass', 30
    UNION ALL SELECT 'CLASSES', 'Classes & Subjects', '/app/classes',
           'ClassOutlined', 'SECTION_ACADEMICS', 'ACADEMIC', NULL, 0, 'nav.classesSubjects', 40
    UNION ALL SELECT 'MY_TIMETABLE', 'My Timetable', '/app/my-timetable',
           'CalendarMonthOutlined', 'SECTION_ACADEMICS', NULL, NULL, 0, 'nav.myTimetable', 50
    UNION ALL SELECT 'ATTENDANCE', 'Attendance', '/app/attendance',
           'EventAvailableOutlined', 'SECTION_ACADEMICS', 'ATTENDANCE', 'ATTENDANCE_VIEW', 0, 'nav.attendance', 60
    UNION ALL SELECT 'LEAVE', 'Leave', '/app/leave',
           'EventBusyOutlined', 'SECTION_ACADEMICS', NULL, NULL, 0, 'nav.leave', 70
    UNION ALL SELECT 'EXAMS', 'Exams & Marks', '/app/exams',
           'AssignmentOutlined', 'SECTION_ACADEMICS', 'EXAM', NULL, 0, 'nav.examsMarks', 80
    UNION ALL SELECT 'ASSIGNMENTS', 'Assignments', '/app/assignments',
           'FactCheckOutlined', 'SECTION_ACADEMICS', 'ASSIGNMENT', NULL, 0, 'nav.assignments', 90
    UNION ALL SELECT 'STUDY_MATERIALS', 'Study Materials', '/app/study-materials',
           'LibraryBooksOutlined', 'SECTION_ACADEMICS', 'MATERIAL', 'MATERIAL_VIEW', 0, 'nav.studyMaterials', 100
    UNION ALL SELECT 'ONLINE_CLASSES', 'Online Classes', '/app/online-classes',
           'VideoCameraFrontOutlined', 'SECTION_ACADEMICS', NULL, NULL, 0, 'nav.onlineClasses', 110

    -- Administration
    UNION ALL SELECT 'STAFF', 'Staff', '/app/staff',
           'GroupsOutlined', 'SECTION_ADMINISTRATION', 'STAFF', NULL, 0, 'nav.staff', 10
    UNION ALL SELECT 'FEES', 'Fees', '/app/fees',
           'PaidOutlined', 'SECTION_ADMINISTRATION', 'FEE', NULL, 0, 'nav.fees', 20
    -- Two screens the Android app already listed as its own menu entries while
    -- the web reached them only through the Fees layout's tabs. Registered here
    -- so both clients get them from the same place and both are governed by
    -- role_menus - an entry only one client knew about was a hole in the model.
    -- Assigned below to the roles the web route guard already allows.
    UNION ALL SELECT 'FEE_SETUP', 'Fee Setup', '/app/fees/setup',
           'PaidOutlined', 'SECTION_ADMINISTRATION', 'FEE', NULL, 0, 'nav.feeSetup', 22
    UNION ALL SELECT 'SCHOLARSHIPS', 'Scholarships', '/app/fees/scholarships',
           'PaidOutlined', 'SECTION_ADMINISTRATION', 'FEE', NULL, 0, 'nav.scholarships', 24
    UNION ALL SELECT 'PAYROLL', 'Payroll', '/app/payroll/runs',
           'RequestQuoteOutlined', 'SECTION_ADMINISTRATION', 'PAYROLL', NULL, 0, 'nav.payroll', 30
    UNION ALL SELECT 'LIBRARY', 'Library', '/app/library/books',
           'MenuBookOutlined', 'SECTION_ADMINISTRATION', 'LIBRARY', NULL, 0, 'nav.library', 40
    UNION ALL SELECT 'TRANSPORT', 'Transport', '/app/transport/assignments',
           'DirectionsBusOutlined', 'SECTION_ADMINISTRATION', 'TRANSPORT', NULL, 0, 'nav.transport', 50
    UNION ALL SELECT 'HOSTEL', 'Hostel', '/app/hostel/residents',
           'ApartmentOutlined', 'SECTION_ADMINISTRATION', 'HOSTEL', NULL, 0, 'nav.hostel', 60
    UNION ALL SELECT 'ADMISSION', 'Admission Enquiries', '/app/admission',
           'HowToRegOutlined', 'SECTION_ADMINISTRATION', 'ADMISSION', NULL, 0, 'nav.admissionEnquiries', 70

    -- Communication
    UNION ALL SELECT 'NOTICES', 'Notice Board', '/app/notices',
           'CampaignOutlined', 'SECTION_COMMUNICATION', 'NOTICE', NULL, 0, 'nav.noticeBoard', 10
    -- Calendar carries no module: a general-purpose surface with no
    -- permission block of its own to switch off.
    UNION ALL SELECT 'CALENDAR', 'Calendar', '/app/calendar',
           'CalendarMonthOutlined', 'SECTION_COMMUNICATION', NULL, NULL, 0, 'nav.calendar', 20
    UNION ALL SELECT 'NOTIFICATIONS', 'Notifications', '/app/communication',
           'NotificationsOutlined', 'SECTION_COMMUNICATION', 'COMMUNICATION', NULL, 0, 'nav.notifications', 30
    -- Seeded disabled, and assigned to nobody. The page is a client-side
    -- placeholder over seeded conversations - there is no chat controller
    -- and no message is ever sent - so it is here as a row to switch on
    -- when a backend exists, not as a menu to offer today.
    UNION ALL SELECT 'CHAT', 'Chat', '/app/chat',
           'ChatOutlined', 'SECTION_COMMUNICATION', NULL, NULL, 0, 'nav.chat', 40

    -- Insights
    UNION ALL SELECT 'REPORTS', 'Reports', '/app/reports/overview',
           'BarChartOutlined', 'SECTION_INSIGHTS', 'REPORTS', NULL, 0, 'nav.reports', 10

    -- Account
    UNION ALL SELECT 'PROFILE', 'My Profile', '/app/profile',
           'PersonOutlineOutlined', 'SECTION_ACCOUNT', NULL, NULL, 0, 'nav.myProfile', 10
    UNION ALL SELECT 'USERS', 'Users', '/app/users',
           'ManageAccountsOutlined', 'SECTION_ACCOUNT', 'USER', 'USER_VIEW', 0, 'nav.users', 20
    UNION ALL SELECT 'SETTINGS', 'Settings', '/app/settings',
           'SettingsOutlined', 'SECTION_ACCOUNT', 'SETTINGS', NULL, 0, 'nav.settings', 30
) AS seed
JOIN menus parent ON parent.menu_key = seed.parent_key
WHERE NOT EXISTS (SELECT 1 FROM menus m WHERE m.menu_key = seed.k);

-- Chat is off. Kept as a separate statement so re-running the migration
-- does not silently re-enable it if someone has switched it on.
UPDATE menus SET is_enabled = 0
WHERE menu_key = 'CHAT' AND created_at = updated_at;

-- ---------------------------------------------------------------------
-- 5. Section headings go to every role
--
-- A heading renders only when it has a visible child, so assigning them
-- broadly costs nothing and means adding a menu to a role never
-- requires remembering to add its heading too.
-- ---------------------------------------------------------------------
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE m.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- ---------------------------------------------------------------------
-- 6. The universal six
--
-- Every role, because every role uses them: everyone applies for leave,
-- reads the notice board, has a profile.
-- ---------------------------------------------------------------------
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE m.menu_key IN ('DASHBOARD', 'LEAVE', 'NOTICES', 'CALENDAR', 'NOTIFICATIONS', 'PROFILE')
  AND NOT EXISTS (
      SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- ---------------------------------------------------------------------
-- 7. Everything else, role by role
--
-- Read this as the answer to "what is this role for?". A LIBRARIAN runs
-- the library; they have no business on the fees screen, and before this
-- migration they were offered one anyway.
-- ---------------------------------------------------------------------
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM (
    -- SUPER_ADMIN and PRINCIPAL run the school and see all of it.
    -- MY_CLASS is included for both: it is gated on holding a homeroom
    -- section, so it stays hidden unless they actually hold one.
              SELECT 'SUPER_ADMIN' AS role_name, 'STUDENTS' AS menu_key
    UNION ALL SELECT 'SUPER_ADMIN', 'TEACHERS'
    UNION ALL SELECT 'SUPER_ADMIN', 'MY_CLASS'
    UNION ALL SELECT 'SUPER_ADMIN', 'CLASSES'
    UNION ALL SELECT 'SUPER_ADMIN', 'ATTENDANCE'
    UNION ALL SELECT 'SUPER_ADMIN', 'EXAMS'
    UNION ALL SELECT 'SUPER_ADMIN', 'ASSIGNMENTS'
    UNION ALL SELECT 'SUPER_ADMIN', 'STUDY_MATERIALS'
    UNION ALL SELECT 'SUPER_ADMIN', 'ONLINE_CLASSES'
    UNION ALL SELECT 'SUPER_ADMIN', 'STAFF'
    UNION ALL SELECT 'SUPER_ADMIN', 'FEES'
    UNION ALL SELECT 'SUPER_ADMIN', 'FEE_SETUP'
    UNION ALL SELECT 'SUPER_ADMIN', 'SCHOLARSHIPS'
    UNION ALL SELECT 'SUPER_ADMIN', 'PAYROLL'
    UNION ALL SELECT 'SUPER_ADMIN', 'LIBRARY'
    UNION ALL SELECT 'SUPER_ADMIN', 'TRANSPORT'
    UNION ALL SELECT 'SUPER_ADMIN', 'HOSTEL'
    UNION ALL SELECT 'SUPER_ADMIN', 'ADMISSION'
    UNION ALL SELECT 'SUPER_ADMIN', 'REPORTS'
    UNION ALL SELECT 'SUPER_ADMIN', 'USERS'
    UNION ALL SELECT 'SUPER_ADMIN', 'SETTINGS'

    UNION ALL SELECT 'PRINCIPAL', 'STUDENTS'
    UNION ALL SELECT 'PRINCIPAL', 'TEACHERS'
    UNION ALL SELECT 'PRINCIPAL', 'CLASSES'
    UNION ALL SELECT 'PRINCIPAL', 'ATTENDANCE'
    UNION ALL SELECT 'PRINCIPAL', 'EXAMS'
    UNION ALL SELECT 'PRINCIPAL', 'ASSIGNMENTS'
    UNION ALL SELECT 'PRINCIPAL', 'STUDY_MATERIALS'
    UNION ALL SELECT 'PRINCIPAL', 'ONLINE_CLASSES'
    UNION ALL SELECT 'PRINCIPAL', 'STAFF'
    UNION ALL SELECT 'PRINCIPAL', 'FEES'
    UNION ALL SELECT 'PRINCIPAL', 'FEE_SETUP'
    UNION ALL SELECT 'PRINCIPAL', 'SCHOLARSHIPS'
    UNION ALL SELECT 'PRINCIPAL', 'PAYROLL'
    UNION ALL SELECT 'PRINCIPAL', 'LIBRARY'
    UNION ALL SELECT 'PRINCIPAL', 'TRANSPORT'
    UNION ALL SELECT 'PRINCIPAL', 'HOSTEL'
    UNION ALL SELECT 'PRINCIPAL', 'ADMISSION'
    UNION ALL SELECT 'PRINCIPAL', 'REPORTS'
    UNION ALL SELECT 'PRINCIPAL', 'USERS'
    UNION ALL SELECT 'PRINCIPAL', 'SETTINGS'

    -- VICE_PRINCIPAL: academics and the operational modules, but not
    -- Users, Settings or Admission Enquiries - matching the roles arrays
    -- that were in navConfig.
    UNION ALL SELECT 'VICE_PRINCIPAL', 'STUDENTS'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'TEACHERS'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'CLASSES'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'ATTENDANCE'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'EXAMS'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'ASSIGNMENTS'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'STUDY_MATERIALS'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'ONLINE_CLASSES'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'STAFF'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'FEES'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'PAYROLL'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'LIBRARY'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'TRANSPORT'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'HOSTEL'
    UNION ALL SELECT 'VICE_PRINCIPAL', 'REPORTS'

    -- TEACHER: what they teach, and their own timetable. MY_CLASS is
    -- assigned because a plain TEACHER can be put in
    -- sections.class_teacher_id and genuinely is a class teacher; the
    -- homeroom gate decides whether it appears.
    UNION ALL SELECT 'TEACHER', 'STUDENTS'
    UNION ALL SELECT 'TEACHER', 'TEACHERS'
    UNION ALL SELECT 'TEACHER', 'MY_CLASS'
    UNION ALL SELECT 'TEACHER', 'CLASSES'
    UNION ALL SELECT 'TEACHER', 'MY_TIMETABLE'
    UNION ALL SELECT 'TEACHER', 'ATTENDANCE'
    UNION ALL SELECT 'TEACHER', 'EXAMS'
    UNION ALL SELECT 'TEACHER', 'ASSIGNMENTS'
    UNION ALL SELECT 'TEACHER', 'STUDY_MATERIALS'
    UNION ALL SELECT 'TEACHER', 'ONLINE_CLASSES'

    UNION ALL SELECT 'CLASS_TEACHER', 'STUDENTS'
    UNION ALL SELECT 'CLASS_TEACHER', 'TEACHERS'
    UNION ALL SELECT 'CLASS_TEACHER', 'MY_CLASS'
    UNION ALL SELECT 'CLASS_TEACHER', 'CLASSES'
    UNION ALL SELECT 'CLASS_TEACHER', 'MY_TIMETABLE'
    UNION ALL SELECT 'CLASS_TEACHER', 'ATTENDANCE'
    UNION ALL SELECT 'CLASS_TEACHER', 'EXAMS'
    UNION ALL SELECT 'CLASS_TEACHER', 'ASSIGNMENTS'
    UNION ALL SELECT 'CLASS_TEACHER', 'STUDY_MATERIALS'
    UNION ALL SELECT 'CLASS_TEACHER', 'ONLINE_CLASSES'

    -- ACCOUNTANT: money. Not students, not exams.
    UNION ALL SELECT 'ACCOUNTANT', 'FEES'
    UNION ALL SELECT 'ACCOUNTANT', 'FEE_SETUP'
    UNION ALL SELECT 'ACCOUNTANT', 'SCHOLARSHIPS'
    UNION ALL SELECT 'ACCOUNTANT', 'PAYROLL'
    UNION ALL SELECT 'ACCOUNTANT', 'REPORTS'

    -- LIBRARIAN: the library, and nothing else.
    UNION ALL SELECT 'LIBRARIAN', 'LIBRARY'

    -- RECEPTIONIST: the front desk - who is enquiring, who is enrolled,
    -- and the transport and hostel questions parents ring up about.
    UNION ALL SELECT 'RECEPTIONIST', 'STUDENTS'
    UNION ALL SELECT 'RECEPTIONIST', 'ADMISSION'
    UNION ALL SELECT 'RECEPTIONIST', 'TRANSPORT'
    UNION ALL SELECT 'RECEPTIONIST', 'HOSTEL'

    -- STUDENT: their own record and their own school life.
    UNION ALL SELECT 'STUDENT', 'STUDENTS'
    UNION ALL SELECT 'STUDENT', 'MY_TIMETABLE'
    UNION ALL SELECT 'STUDENT', 'ATTENDANCE'
    UNION ALL SELECT 'STUDENT', 'EXAMS'
    UNION ALL SELECT 'STUDENT', 'ASSIGNMENTS'
    UNION ALL SELECT 'STUDENT', 'STUDY_MATERIALS'
    UNION ALL SELECT 'STUDENT', 'ONLINE_CLASSES'
    UNION ALL SELECT 'STUDENT', 'FEES'
    UNION ALL SELECT 'STUDENT', 'TRANSPORT'
    UNION ALL SELECT 'STUDENT', 'HOSTEL'

    -- PARENT: the same, seen through their children.
    UNION ALL SELECT 'PARENT', 'MY_CHILDREN'
    UNION ALL SELECT 'PARENT', 'MY_TIMETABLE'
    UNION ALL SELECT 'PARENT', 'ATTENDANCE'
    UNION ALL SELECT 'PARENT', 'EXAMS'
    UNION ALL SELECT 'PARENT', 'ASSIGNMENTS'
    UNION ALL SELECT 'PARENT', 'STUDY_MATERIALS'
    UNION ALL SELECT 'PARENT', 'ONLINE_CLASSES'
    UNION ALL SELECT 'PARENT', 'FEES'
    UNION ALL SELECT 'PARENT', 'TRANSPORT'
    UNION ALL SELECT 'PARENT', 'HOSTEL'

    -- SECURITY_GUARD gets the universal six from section 6 and nothing
    -- here. Not an oversight: there is no screen in this product that a
    -- gate guard needs, and offering them the student directory would be
    -- a data-protection problem rather than a convenience.
) AS grant_seed
JOIN roles r ON r.name = grant_seed.role_name
JOIN menus m ON m.menu_key = grant_seed.menu_key
WHERE NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- ---------------------------------------------------------------------
-- 8. Verification
-- ---------------------------------------------------------------------
SELECT 'menus' AS check_name,
       COUNT(*)                                        AS total,
       SUM(parent_id IS NULL)                          AS headings,
       SUM(parent_id IS NOT NULL)                      AS destinations,
       SUM(is_enabled = 0)                             AS switched_off
FROM menus;

-- Menus per role, headings excluded - the number a user would actually
-- see before the module, homeroom and permission gates narrow it.
SELECT r.name                          AS role_name,
       COUNT(m.id)                     AS menus_assigned
FROM roles r
LEFT JOIN role_menus rm ON rm.role_id = r.id
LEFT JOIN menus m ON m.id = rm.menu_id AND m.parent_id IS NOT NULL AND m.is_enabled = 1
GROUP BY r.id, r.name
ORDER BY menus_assigned DESC, r.name;

-- Any menu whose required_permission does not exist would be invisible
-- to everyone for a reason no screen explains. Expect zero rows.
SELECT m.menu_key, m.required_permission AS missing_permission
FROM menus m
WHERE m.required_permission IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = m.required_permission);

-- Any menu granted to a role that does not hold the permission behind
-- it. These are not errors - SUPER_ADMIN overrides permissions, and a
-- grant can be added later - but they are the rows to look at first when
-- someone reports a menu they cannot see.
SELECT r.name AS role_name, m.menu_key, m.required_permission
FROM role_menus rm
JOIN roles r ON r.id = rm.role_id
JOIN menus m ON m.id = rm.menu_id
WHERE m.required_permission IS NOT NULL
  AND r.name <> 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      JOIN permissions p ON p.id = rp.permission_id
      WHERE rp.role_id = r.id AND p.name = m.required_permission
  )
ORDER BY r.name, m.menu_key;
