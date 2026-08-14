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
```

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
