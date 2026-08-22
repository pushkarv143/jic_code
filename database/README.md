# School Management System - Database

MySQL 8.0+ schema, indexes, views, triggers, procedures and seed/sample
data for the School Management System, built strictly to
`../SCHEMA_CONTRACT.md` so table/column names line up with the Spring
Boot backend and React frontend being built from the same contract.

## Prerequisites

- MySQL Server 8.0 or later (the schema uses `ROW_NUMBER() OVER (...)`
  window functions in `07_sample_data.sql`, which require MySQL 8.0+).
- A MySQL client (the `mysql` CLI is assumed below; any GUI client that
  can run `.sql` files against the target server works equally well).
- A user with rights to `CREATE DATABASE`, `CREATE`, `INSERT`, `TRIGGER`,
  `CREATE ROUTINE`, `EXECUTE` on the target server (e.g. `root`).

## How to run (in this exact order)

```bash
mysql -u root -p                          < 00_create_database.sql
mysql -u root -p school_management_system  < 01_schema.sql
mysql -u root -p school_management_system  < 02_indexes.sql
mysql -u root -p school_management_system  < 03_views.sql
mysql -u root -p school_management_system  < 04_triggers.sql
mysql -u root -p school_management_system  < 05_procedures.sql
mysql -u root -p school_management_system  < 06_seed_reference_data.sql
mysql -u root -p school_management_system  < 07_sample_data.sql
mysql -u root -p school_management_system  < 08_rbac_demo_data.sql
mysql -u root -p school_management_system  < 09_study_materials.sql
mysql -u root -p school_management_system  < 10_admin_permissions.sql
mysql -u root -p school_management_system  < 15_role_config_and_my_class.sql
mysql -u root -p school_management_system  < 16_timetable_permission.sql
mysql -u root -p school_management_system  < 17_roll_number_uniqueness.sql
mysql -u root -p school_management_system  < 18_my_class_edit_only.sql
mysql -u root -p school_management_system  < 19_menu_registry.sql
mysql -u root -p school_management_system  < 20_privilege_module.sql
mysql -u root -p school_management_system  < 21_single_teacher_role.sql
mysql -u root -p school_management_system  < 22_admin_provisioned_accounts.sql
```

> Files `11_`-`14_` are incremental migrations that predate this list and are
> not described below. Run them in filename order between `10_` and `15_` on an
> existing database; a fresh install picks their schema up from `01_schema.sql`.
>
> `19_menu_registry.sql` is the one file a fresh install must run even so: the
> `menus` and `role_menus` tables are not in `01_schema.sql`, and without them
> every user signs in to an empty sidebar. The clients have no built-in menu to
> fall back on, which is the point of the change.

Each file also issues its own `USE school_management_system;`, so piping
all eleven files through a single `mysql -u root -p < 00_create_database.sql
< 01_schema.sql ...` style invocation is not necessary - running them one
at a time, in numeric order, against a fresh server is the supported path.
`07_sample_data.sql` in particular calls several stored procedures that
loop hundreds of times; it can take anywhere from a few seconds to a
couple of minutes depending on hardware.

To start over, simply `DROP DATABASE school_management_system;` and
re-run 00 through 10.

Files 08-10 are also safe to run against an existing database: each is
guarded so re-running it is a no-op rather than an error. Re-run
`10_admin_permissions.sql` after adding any new permission, so the
administrator picks it up.

## File-by-file summary

| File | Purpose |
|---|---|
| `00_create_database.sql` | Creates the `school_management_system` database (utf8mb4/utf8mb4_unicode_ci). |
| `01_schema.sql` | Every table from the contract, in FK-safe creation order, InnoDB, with all FOREIGN KEY / UNIQUE / ENUM constraints. |
| `02_indexes.sql` | 22 additional performance indexes beyond the implicit PK/FK/UNIQUE ones (with a one-line rationale comment each). |
| `03_views.sql` | 7 reporting views: `vw_student_summary`, `vw_fee_collection_summary`, `vw_attendance_summary`, `vw_teacher_workload`, `vw_library_overdue`, `vw_class_section_strength`, `vw_student_fee_due`. |
| `04_triggers.sql` | Library copy-count triggers on `book_issues`, a fee-payment roll-forward trigger on `fee_payments` that recomputes `student_fees.status`, and hostel occupancy-count triggers on `hostel_students`. |
| `05_procedures.sql` | `sp_calculate_student_attendance_percentage`, `sp_generate_fee_receipt_number`, `sp_promote_students` - the permanent, application-facing stored procedures. |
| `06_seed_reference_data.sql` | Roles, permissions + RBAC mapping, academic years, departments, designations, 20 classes / 54 sections / subjects, fee categories & structures, exam types, grades, book categories, transport (drivers/buses/routes/pickup points), 2 hostels + 40 rooms, school info, system settings, 8 demo login users, sample notices/events/admission enquiries. |
| `07_sample_data.sql` | Procedural bulk generator (WHILE loops + name-pool lookup tables) producing 50 teachers, 30 staff, 500 students (with guardians, medical details), 2000 attendance records and 1000 fee records with real payments. |
| `08_rbac_demo_data.sql` | RBAC demo fixtures: `student.demo`, `student.other` (a different section, the control case), `parent.demo` linked to `student.demo` only, `classteacher.demo` as homeroom teacher of one section, and `class_subject_teacher` rows for `teacher.demo` — without which row-level scoping leaves the demo teacher with an empty student list. Re-runnable. |
| `09_study_materials.sql` | Migration for an already-deployed database: creates `study_materials` and its indexes if absent, adds the `MATERIAL_*` permissions and role grants, and seeds three demo materials (published/draft/other-section) that between them demonstrate both visibility rules. A fresh install gets the table from `01_schema.sql` and the permissions from `06_seed_reference_data.sql`, so this is a no-op there. Re-runnable. |
| `10_admin_permissions.sql` | Guarantees `SUPER_ADMIN` holds **every** permission (written as "all permissions not already granted", so it also picks up permissions added by later modules), and reconciles `PRINCIPAL`/`VICE_PRINCIPAL` grants with the role gates their endpoints already enforce. Ends with a verification query that must return 0 missing admin permissions. Re-runnable. |
| `15_role_config_and_my_class.sql` | Makes authorization configurable at runtime instead of at deploy time. Creates `org_modules`, a registry of the functional areas this school runs, keyed on `permissions.module` — switching a row off strips every permission in that module from every role, in the API as well as the menu (`CustomUserDetailsService` applies the same filter when it builds a caller's authorities). `is_core` marks the modules the API refuses to disable, so an installation can never lose the ability to re-enable anything. Also adds the `MY_CLASS` module and its three permissions (`MY_CLASS_VIEW`, `MY_CLASS_ROSTER_MANAGE`, `MY_CLASS_OFFICIALS_MANAGE`), granted to `CLASS_TEACHER` and `SUPER_ADMIN`. These are deliberately narrower than `STUDENT_CREATE`/`STUDENT_UPDATE`: they reach only the section the holder appears in as `sections.class_teacher_id`. Ends with three verification queries, the last of which reports how many `CLASS_TEACHER` role holders have no homeroom assignment (27 on the seeded database — which is why `HomeroomGuard` keys on the assignment and not the role). Re-runnable. |
| `16_timetable_permission.sql` | Gives the timetable its own permission, `TIMETABLE_MANAGE`, held by `SUPER_ADMIN` alone. `/api/v1/timetable/**` writes were previously gated by the hardcoded list `hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')`, which admitted three roles where the school wants one and — being a role list rather than a grant — could not be reconfigured: revoking something on the Roles & Permissions screen would hide the controls and leave the API still accepting the requests. Filed under the `ACADEMIC` module, which is core, so the permission cannot be switched off by disabling a module out from under it. Reads are untouched: a teacher still gets their own week from `/timetable/me`. Grant it to `PRINCIPAL` if you want the office to help build the week. Ends with a verification query listing every role that holds it. Re-runnable. |
| `18_my_class_edit_only.sql` | Narrows what a homeroom teacher may do on their own roster. `MY_CLASS_ROSTER_MANAGE` was granted to add *and* edit students; admitting a student is now the office's job alone, so the description becomes "Edit the records of students already in your own homeroom section" and `MyClassController` has no POST. The permission is deliberately **not** renamed: the name is in `role_permissions`, in issued JWT authorities and in both clients, and renaming it to match a narrowed meaning would break every signed-in session for a wording change. Re-runnable. |
| `19_menu_registry.sql` | Moves the navigation menu out of the clients and into the database. Creates `menus` — one row per destination plus one per section heading, self-referencing through `parent_id`, carrying the path, icon name, i18n key, sort order and the three gates (`module_key`, `required_permission`, `requires_homeroom`) — and `role_menus`, a plain join table where presence is the grant. Seeds 36 menus (6 headings, 30 destinations, Chat switched off) and assigns them from the `roles` arrays that used to live in the web client's `navConfig.tsx` and the Android client's `Destinations.kt`, so no user's menu changes on the day it runs — what changes is that the assignment is now editable from **Settings → Roles & Permissions → Menus**. A menu row grants *visibility only*: the module, homeroom and permission gates still apply on top, so assigning a menu to a role that lacks the permission behind it shows nothing. Ends with four verification queries, including one listing exactly those cases. Re-runnable. |
| `20_privilege_module.sql` | Gives menu administration a module of its own. `19_` hung the menu endpoints off the ROLE grants, on the reasoning that assigning a menu and granting a permission are two halves of one job — true of the work, not of the authority: ROLE_MANAGE is held by the principal to adjust what a teacher may do, while deciding what appears on every role's navigation shapes what the whole organisation sees. Adds the `PRIVILEGE` module (core, so the registry refuses to switch it off — the module gate is the one gate SUPER_ADMIN does not bypass, and disabling it would hide the Privileges screen from the only account that can configure menus), the `PRIVILEGE_VIEW` / `PRIVILEGE_MANAGE` permissions granted to **SUPER_ADMIN alone**, and the Privileges menu entry under Account assigned to the same. Permissions rather than a hard-coded role, so an organisation can delegate from the role editor. Also retires `MenuController`'s reference to `ROLE_READ`, which is not a permission that exists — the catalogue answered 403 to a principal holding `ROLE_VIEW` and only ever succeeded through the SUPER_ADMIN override. Re-runnable. |
| `21_single_teacher_role.sql` | Collapses TEACHER and CLASS_TEACHER into one role, and makes "is also a class teacher" an attribute rather than an identity. The two were never two kinds of person: CLASS_TEACHER held exactly TEACHER's permissions **plus six**, and 44 users carried it while only 17 appeared in `sections.class_teacher_id` — so 27 people held homeroom privileges over a homeroom they did not have, and the 7 on plain TEACHER could not be given one without an administrator also changing their role. Adds `teachers.is_class_teacher` (server-maintained from the section rows, re-synced on every run of this file, so re-running is also the repair for drift) and `class_teacher_permissions`, seeded by **computing** what CLASS_TEACHER held and TEACHER did not — so the migration cannot disagree with the state it replaces. Moves all 44 users to TEACHER and deletes the role; its `role_permissions` and `role_menus` cascade, and since both roles carried identical menus nothing a teacher sees changes. The 17 keep every permission; the 27 lose the six, which is the correction rather than a regression — the endpoints behind them are homeroom-scoped server-side and already answered 403 for a teacher without one. Ends with six verification queries including a drift check that must return zero rows. Re-runnable. |
| `22_admin_provisioned_accounts.sql` | Student accounts are created by the school, not by the student. The login page offered "Create a Student / Parent Account", so anyone who found the URL could make one and wait to be approved — the wrong direction for a school that already knows who its students are and admits them through the Add Student form. Adds `users.must_change_password`, true for an account still on the password the system generated for it; while it is set the API refuses every request except changing that password and signing out, so a generated password can be used for exactly one thing — replacing itself. Existing accounts are backfilled to 0: they chose their own passwords, and forcing 502 students to reset one they already picked is a support incident rather than a security improvement. No column stores the generated password — it is emailed once and kept only as a bcrypt hash, so a lost one is reset through forgot-password rather than looked up. Nothing about parents is bundled in: `parents` and `student_parents` are both empty and no user holds the PARENT role, so no working parent login is being taken away. Re-runnable. |
| `17_roll_number_uniqueness.sql` | Makes a roll number identify exactly one student in a class. `07_sample_data.sql` numbered students 1..N *within each section*; `14_single_section_a.sql` then collapsed every class onto one section without renumbering, so three former sections' students landed in one section each still carrying rolls 1, 2, 3 — class 1 had three students on every roll number. Nothing enforced it either way: `roll_number` had no unique key at any level. This renumbers every student 1..N per class in `admission_number` order (deterministic, and follows the order they joined) and adds `UNIQUE KEY uq_students_class_roll (class_id, roll_number)`. Scoped to the class, not the section: `admission_number` is already unique school-wide and is the school-level identifier, while a roll number is a position within a class — and since `classes` carry `academic_year_id`, the key is naturally scoped to one year. Ends with two verification queries that must both return 0. Re-runnable. |

## Demo logins

All seeded passwords use the exact bcrypt hashes specified in
`SCHEMA_CONTRACT.md` (verified against Spring Security's
`BCryptPasswordEncoder` - do not regenerate them).

| Role | Username | Password |
|---|---|---|
| SUPER_ADMIN | `admin` | `Admin@123` |
| PRINCIPAL | `principal` | `Password@123` |
| VICE_PRINCIPAL | `viceprincipal` | `Password@123` |
| TEACHER (demo, has a real `teachers` row) | `teacher.demo` | `Password@123` |
| Bulk-generated teachers | `teacher1` ... `teacher50` | `Password@123` |
| ACCOUNTANT (demo) | `accountant.demo` | `Password@123` |
| Bulk-generated staff | `staff1` ... `staff30` | `Password@123` |
| LIBRARIAN (demo) | `librarian.demo` | `Password@123` |
| RECEPTIONIST (demo) | `receptionist.demo` | `Password@123` |
| SECURITY_GUARD (demo) | `security.demo` | `Password@123` |
| Bulk-generated students | `student1` ... `student500` | `Password@123` |

All non-super-admin accounts share the single `Password@123` bcrypt
hash `$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa`.
The super admin uses `Admin@123` ->
`$2b$10$Ih9JH8QwmYKKqexbzqQRhOz6b8WE3i1QYFOABFL8F6l9pXFOqEP.q`.

## Row counts produced by 07_sample_data.sql

| Table | Rows added by 07 | Notes |
|---|---|---|
| `teachers` / `users` (TEACHER/CLASS_TEACHER) | 50 | Plus the 1 demo teacher from 06 = 51 total. ~43 of the 54 sections get a `class_teacher_id`, and those teachers' `role_id` is switched from TEACHER to CLASS_TEACHER. |
| `staff` / `users` | 30 | Split roughly evenly across ACCOUNTANT/LIBRARIAN/RECEPTIONIST/SECURITY_GUARD; plus 4 demo staff from 06 = 34 total. |
| `class_subject_teacher` | ~151 | 2-4 subjects per teacher, no duplicate (section, subject) pairs. |
| `students` / `users` | 500 | Round-robin across the 54 sections (~9-10 students/section given 500/54), sequential `roll_number` per section, `admission_number` = `ADM2026####`. |
| `guardians` | ~750 | Every student gets 1 primary guardian; ~50% get a 2nd (non-primary) guardian. |
| `student_medical_details` | ~250 | Roughly half the students (odd-numbered). |
| `student_attendance` | 2000 | 40 school days (Jun-Jul 2026, Sundays skipped) x a rotating, non-overlapping window of 50 of the 500 students per day - guarantees the `UNIQUE(student_id, attendance_date)` constraint is never violated. ~90% PRESENT, ~5% ABSENT, ~5% LATE/HALF_DAY/LEAVE. |
| `student_fees` | 1000 | 500 students x (Tuition Fee + Exam Fee). |
| `fee_payments` | ~700 | 45% of the 1000 `student_fees` rows are paid in full, 25% partially (half the due amount) - each gets a `fee_payments` row with a unique receipt number from `sp_generate_fee_receipt_number`. The remaining 30% are left unpaid, marked OVERDUE or UNPAID depending on whether `due_date` has passed. |

## Notable design choices / deviations from a literal reading of the contract

- **`created_by` / `updated_by`**: the contract's general note says every
  table gets these two nullable FK-to-`users` columns "unless noted
  otherwise," but every per-table column list in the module sections
  (except `users` itself) omits them. To keep round 1 shippable, they
  were added only to `users` (as explicitly listed). Adding them
  everywhere is a mechanical follow-up for a later round.
- **`is_deleted`**: added exactly where the contract's per-table lists
  call for it (`teachers`, `staff`, `students`, `classes`, `subjects`,
  `books`), plus `buses`, which the contract's general note names
  explicitly as a soft-deletable table even though the Transport
  module's column list doesn't spell it out.
- **Pure join tables** (`role_permissions`, `class_subject_teacher`,
  `student_parents`) have no `created_at`/`updated_at` - every other
  table has both.
- **`parents` / `student_parents`**: tables exist in `01_schema.sql`
  (per the contract) but are not seeded in round 1. The bulk sample-data
  spec only asked for `guardians` (contact info), not portal-login
  parent accounts; wiring up parent logins is a natural round-2 task.
- **Fee amounts are flat per category**, not scaled by class level
  (e.g. Tuition Fee is 20000 for Nursery and for Class 12 alike). This
  keeps `06_seed_reference_data.sql` readable; a later round can vary
  amounts by class.
- **`admission_number` format**: contract's example was `ADM2024####`;
  since the "current" academic year as of today (2026-08-06) is
  2026-2027, generated admission numbers use `ADM2026####` instead,
  which is the same pattern with the year updated to match "now."
- **Section sizes are small (~9-10 students)**: a direct consequence of
  spreading exactly 500 students across 20 classes / 54 sections as
  specified, not a bug - a real school this size would either have far
  fewer sections or many more students.
