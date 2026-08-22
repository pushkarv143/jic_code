# Greenwood School Management — Android client

A native Android client for the **existing** School Management System backend
(`school-backend`, Spring Boot 3 / Java 21). It talks to the same deployed
`/api/v1` REST API the React web app uses, with the same JWT authentication, the
same roles and permissions, and the same business rules.

**No backend code was added or changed for this app.** It is purely a second
client of the API that already exists.

---

## Table of contents

1. [Status — read this first](#status--read-this-first)
2. [Tech stack](#tech-stack)
3. [Architecture](#architecture)
4. [Build & run](#build--run)
5. [Environments](#environments)
6. [Authentication](#authentication)
7. [Error handling](#error-handling)
8. [Web → Android feature mapping](#web--android-feature-mapping)
9. [API inventory](#api-inventory)
10. [Testing](#testing)
11. [Backend / API gaps](#backend--api-gaps)
12. [Implementation status](#implementation-status)

---

## Status — read this first

Two things you need to know before you open this in Android Studio.

**1. This project has never been compiled.** It was authored on a machine with no
Android SDK and no Gradle installation, so `./gradlew assembleDebug` has not been
run against it. The dependency set is a known-good, mutually compatible matrix
(Kotlin 2.0.21 / AGP 8.7.2 / KSP 2.0.21-1.0.25 / Hilt 2.52 / Compose BOM
2024.10.01), but expect to fix a handful of import and signature errors on the
first build. Treat the first `assembleDebug` as part of the setup.

**2. The Gradle wrapper JAR is not committed.** A binary can't be authored as
text. Generate it once:

```bash
cd school-android
gradle wrapper --gradle-version 8.9      # if you have Gradle installed
# — or just open the folder in Android Studio, which regenerates it for you
```

**3. Every module in the web sidebar now has a screen.** See
[Implementation status](#implementation-status) for the full list and for the
handful of sub-features deliberately left to the web console.

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.10.01), Material 3 |
| DI | Hilt 2.52 (KSP) |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| JSON | kotlinx.serialization 1.7.3 |
| Async | Coroutines 1.9 + Flow |
| Navigation | Navigation Compose 2.8.4 |
| Local storage | DataStore (session), Room 2.6.1 (offline cache) |
| Images | Coil 2.7 |
| Min / target SDK | 26 / 35 |

No library is present that isn't used.

## Architecture

```
Composable screen
      ↓  state (StateFlow) / events (function refs)
ViewModel                       ui/feature/<module>/
      ↓
Repository interface            domain/repository/
      ↓
Repository implementation       data/repository/
      ↓
Retrofit API  ·  Room DAO       data/remote/api/  ·  data/local/
```

```
com.greenwood.school
├── core
│   ├── common       Formatters, Validators, Role, Constants, DocumentOpener, dispatcher qualifiers
│   ├── network      ApiResult, AppError, ErrorMapper, AuthInterceptor, TokenAuthenticator, ConnectivityObserver
│   ├── security     KeystoreCrypto (AES-GCM, Android Keystore)
│   └── session      SessionManager, AuthEventBus
├── data
│   ├── local        Room cache (notices + dashboard snapshot)
│   ├── remote
│   │   ├── api      15 Retrofit interfaces covering every endpoint the web app uses
│   │   └── dto      Wire models, one file per domain
│   └── repository   15 implementations + BaseRepository
├── domain
│   └── repository   15 contracts the ViewModels depend on
├── di               AppModule, NetworkModule, RepositoryModule
├── navigation       Routes, MENU_SECTIONS, AppNavHost
└── ui
    ├── theme        Colour/type/shape ported from the web MUI theme
    ├── components   StateHost, StatCard, EntityRowCard, PagedLazyColumn, form fields…
    ├── common       UiState, PagedListState, UiMessage
    └── feature      One package per module
```

**Two deliberate deviations from the textbook layout**, both documented at the
point they occur:

- **No separate `domain/model` layer.** The API contract has ~90 models and this
  app is the only consumer; duplicating each one plus a mapper would add roughly
  4,000 lines of code that can only ever be a 1:1 copy. Instead the wire DTOs are
  immutable Kotlin data classes exposed through the domain repository interfaces.
  If a second data source ever appears, that's the moment to introduce the split.
- **Use cases only where there is logic.** A `GetStudentsUseCase` that forwards one
  call to one repository is indirection, not architecture. Business logic that does
  exist (session lifecycle, token refresh, dashboard composition, paging
  accumulation) lives in `SessionManager`, `TokenAuthenticator`, the dashboard
  ViewModel and `PagedListState` respectively.

## Build & run

```bash
cd school-android

# Debug build against the deployed QA/prod API
./gradlew assembleProdDebug

# Debug build against a backend on your machine (emulator → 10.0.2.2:8080)
./gradlew assembleDevDebug

# Install
./gradlew installProdDebug

# Unit tests
./gradlew testProdDebugUnitTest

# Instrumented tests (needs a running device/emulator)
./gradlew connectedProdDebugAndroidTest

# Lint
./gradlew lintProdDebug
```

Release signing reads an untracked `keystore.properties` at the project root:

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

Without that file the release build falls back to debug signing so CI still
produces an artifact. **Nothing about the keystore is committed.**

## Environments

Three product flavours, each with its own `applicationId` so all three can be
installed side by side.

| Flavour | applicationId | Default API base URL |
|---|---|---|
| `dev` | `com.greenwood.school.dev` | `http://10.0.2.2:8080/api/v1/` |
| `qa` | `com.greenwood.school.qa` | `http://132.226.191.38:8080/api/v1/` |
| `prod` | `com.greenwood.school` | `http://132.226.191.38:8080/api/v1/` |

The URL is declared **only** in `app/build.gradle.kts` and reaches the code as
`BuildConfig.BASE_URL`. Nothing else hard-codes a host. Override per build:

```bash
./gradlew assembleDevDebug -PdevApiUrl=http://192.168.1.20:8080/api/v1/
./gradlew assembleProdRelease -PprodApiUrl=https://api.greenwood.example/api/v1/
```

> **The trailing slash is required** — Retrofit resolves relative paths against
> the base URL, and every API method uses a relative path (`auth/login`, not
> `/auth/login`).

### Cleartext HTTP

The deployed backend currently serves plain HTTP on an IP address. Android 9+
blocks cleartext by default, so `res/xml/network_security_config.xml` permits it
for exactly three hosts (`132.226.191.38`, `10.0.2.2`, `localhost`) and denies it
everywhere else. **This is a stopgap, not a design choice** — JWTs are crossing
the network unencrypted. See [gap #1](#backend--api-gaps).

## Authentication

Identical to the web client's `axiosInstance.ts` flow:

```
POST /auth/login  →  { accessToken (15 min), refreshToken (7 days), user }
        ↓
AuthInterceptor adds  Authorization: Bearer <accessToken>  to every non-public call
        ↓
any 401 → TokenAuthenticator → POST /auth/refresh-token → retry the original call
        ↓
refresh fails → session cleared → AuthEventBus → MainActivity → Login (back stack cleared)
```

- **Single-flight refresh.** Concurrent 401s take a lock; one refresh goes out and
  the rest replay with the new token — the same guarantee the web client gets from
  its `isRefreshing` + `pendingQueue`.
- **No recursion.** The refresh call uses a bare OkHttp client with no interceptors
  and no authenticator.
- **Storage.** The token pair is AES-256-GCM encrypted with a non-exportable
  Android Keystore key before it is written to DataStore. `allowBackup=false` and
  the backup rules exclude DataStore and the database.
- **Logging.** HTTP body logging is debug-only, the `Authorization` header is
  redacted by OkHttp, and `password` / `newPassword` / `accessToken` /
  `refreshToken` are stripped from bodies before any line is emitted.
- **No secrets in source.** No username, password, token or key is hard-coded.

`/auth/login`, `/auth/register`, `/auth/refresh-token`, `/auth/forgot-password`,
`/auth/reset-password` and `/public/admission-enquiries` are the only unauthenticated
paths, and `AuthInterceptor` skips them.

### Authorization — what the user may actually do

Separate from authentication, and read from the server rather than inferred:

```
sign in / restore session / silent token refresh
        ↓
AccessStore  →  GET /me/access  →  { role, permissions, enabledModules, homeroom }
        ↓
menus (menuForRole) + in-screen controls + MainActivity's splash gate
```

`UserDto.permissions` from the login response is a snapshot written to disk, so it
cannot see a grant an administrator changes mid-session. `AccessStore` re-reads
`/me/access` on **sign-in**, on **app start**, on **every silent token refresh** (the
`TokenAuthenticator` is synchronous, so it publishes `AuthEvent.TokensRefreshed` and a
collector does the fetch) and on **sign-out**, where it drops the grants rather than
leaving them for whoever signs in next.

Three independent things narrow what a user sees, and the app keeps them distinct:

| Gate | Source | Notes |
|---|---|---|
| **Module** | `enabledModules` | Checked first, and the one gate `SUPER_ADMIN` does **not** bypass — it is a statement about the organisation, not the user. Unknown keys read as *enabled*, matching the backend. |
| **Homeroom** | `homeroom` / `classTeacherOfOwnSection` | Not a permission — a row in `sections`. Gates My Class. 44 users hold the `CLASS_TEACHER` role and 17 hold an assignment, so the role would advertise an empty screen to 27 of them. |
| **Permission** | `permissions` | **Strict**: an entry declaring a permission is hidden unless the user holds it. There is no "empty set means unknown, so show it" fallback — that briefly offered actions a role does not have. |

`MainActivity` holds the splash until the first `/me/access` answers, so no screen is
ever composed from a guess. It latches, so a later background re-read updates the menu
in place instead of throwing the user back to the splash.

None of this is a security boundary — every endpoint re-checks the same grant on every
request. It only decides what the interface *offers*, so a control is never shown that
the API would refuse.

## Error handling

Every failure becomes one `AppError` case in `core/network/ErrorMapper.kt`:

| HTTP / condition | `AppError` | What the user sees |
|---|---|---|
| 400, 422 | `Validation` | The server's message + per-field errors under each field |
| 401 | `Unauthorized` | Session expired → sign-in |
| 403 | `Forbidden` | The server's own wording (e.g. "Your account is not active…") |
| 404 | `NotFound` | "We couldn't find what you were looking for." |
| 409 | `Conflict` | The server's duplicate message |
| 5xx | `Server` | Generic copy — **the server's message is never shown** |
| `UnknownHostException`, `IOException` | `Network` | Offline state + Retry |
| `SocketTimeoutException` | `Timeout` | Timeout state + Retry |

Where the backend writes a useful sentence (it does for 400/401/403/404/409) the
app shows **that** sentence, so web and Android say the same thing. 5xx messages
are replaced because they can leak internals.

`StateHost` renders loading / empty / error, picks an icon and wording per error
case, and only offers Retry when a retry could actually help — a 403 gets no
Retry button.

## Web → Android feature mapping

| Web page | Android | APIs |
|---|---|---|
| `LoginPage` | `LoginScreen` | `POST /auth/login` |
| `RegisterPage` | `RegisterScreen` | `POST /auth/register` |
| `ForgotPasswordPage` + `ResetPasswordPage` | `ForgotPasswordScreen` (two steps, one screen) | `POST /auth/forgot-password`, `/auth/reset-password` |
| `DashboardPage` (8 role variants) | `DashboardScreen`, role-branched tile set | `/analytics/dashboard`, `/reports/*`, `/payroll/dashboard`, `/library/dashboard`, `/notices`, `/events`, `/attendance/students/{id}/summary`, `/student-fees` |
| Sidebar (6 groups, 20 items) | Bottom bar (Home · Academics · Admin · More) + hub menus from the same `MENU_SECTIONS` list | — |
| `StudentListPage` (11-column DataGrid) | `StudentListScreen` — cards, debounced search, filter bottom sheet, infinite scroll | `GET /students` |
| `StudentProfilePage` (4 tabs) | `StudentDetailScreen` — stacked sections, one request | `GET /students/{id}` |
| `StudentFormPage` | `StudentFormScreen` — sectioned single column, guardian captured inline on create | `POST/PUT /students` |
| `TeacherListPage` / `TeacherProfilePage` | `TeacherListScreen`, `TeacherDetailScreen` | `/teachers`, `/teachers/{id}` |
| `StaffPage`, `UserListPage` | `StaffListScreen`, `UserListScreen` (activate/deactivate switch) | `/staff`, `/users`, `/users/{id}/activate\|deactivate` |
| `ClassListPage` + `ClassDetailPage` | `ClassListScreen`, `ClassDetailScreen` (Sections/Subjects/Teachers tabs) | `/classes`, `/classes/{id}/sections`, `/subjects`, `/class-subject-teacher` |
| `MarkAttendancePage`, `AttendanceReportPage`, `MonthlyAttendancePage`, `TeacherAttendancePage` (4 routes) | one `AttendanceScreen` with 4 tabs; students/parents get a single self-service tab | `/attendance/**` |
| `LeaveListPage` + apply modal | `LeaveScreen` (My leave / All applications) + apply bottom sheet | `/leave-applications/**` |
| `ExamSetupPage`, `MarksEntryPage`, `ResultsPage` | `ExamListScreen` → `ExamDetailScreen` (Schedule/Results) → `MarksEntryScreen` | `/exams`, `/exams/{id}/schedules`, `/marks/roster`, `/marks/entry`, `/exams/{id}/results` |
| `AssignmentListPage` | `AssignmentListScreen` + `AssignmentDetailScreen` | `/assignments/**` |
| `OnlineClassesPage` | `OnlineClassesScreen` — Join hands the link to the installed meeting app | `/online-classes` |
| `FeeCollectionPage` + payment modal | `FeeCollectionScreen` + `CollectPaymentSheet`; tapping a paid row opens its PDF receipt | `/student-fees`, `POST /fee-payments`, `/fee-payments/{id}/receipt/pdf`, `/fees/dues-summary` |
| `FeeSetupPage`, `ScholarshipsPage` | `FeeSetupScreen` (Categories/Structures), `ScholarshipsScreen` | `/fee-categories`, `/fee-structures`, `/scholarships` |
| `PayrollRunsPage`, `SalaryStructuresPage`, `SalarySlipPage` | `PayrollScreen` (Runs/Structures) — slips download and open as PDFs | `/payroll/**`, `/salary-structures` |
| `BooksPage`, `IssueReturnPage`, `OverdueBooksPage` | `LibraryScreen` (Catalogue/Issued/Overdue) with the dashboard counters pinned above | `/books`, `/book-issues`, `/library/dashboard` |
| `FleetPage`, `StudentAssignmentsPage` | `TransportScreen` (Assignments/Routes/Buses/Drivers) | `/student-transport`, `/routes`, `/buses`, `/drivers` |
| `ResidentsPage`, `RoomsPage`, `VisitorsPage`, `HostelFeesPage` | `HostelScreen`, 4 tabs | `/hostel-students`, `/hostels/{id}/rooms`, `/hostel-visitors`, `/hostel-fees` |
| `AdmissionEnquiriesPage` | `AdmissionEnquiriesScreen` with inline approve/reject | `/admission-enquiries` |
| `NoticeBoardPage` | `NoticeBoardScreen` — tap to expand, no detail route | `/notices` |
| `CalendarPage` | `CalendarScreen` (Events/Birthdays, month stepper) | `/events`, `/calendar/birthdays` |
| `NotificationsPage` | `NotificationsScreen` | `/notifications/my` |
| `ReportsLayout` (8 chart pages) | one `ReportsScreen`, scrollable tabs, charts as proportion bars | `/reports/*` |
| `SettingsPage` | `SettingsScreen` — school info editable, system settings read-only | `/settings/school-info`, `/settings/system` |
| Top-bar search dropdown | `SearchScreen` (full screen — a dropdown has nowhere to render over a keyboard) | `/search/global` |
| `ParentDashboardPage` | `MyChildrenScreen` → student detail | `/parents/me/children` |
| `ProfilePage` + profile menu | `ProfileScreen` (details, change password, sign out) | `/auth/me`, `/auth/change-password`, `/auth/logout` |
| Public marketing pages (Landing/About/Academics/Facilities/Faculty/Gallery/Contact) | **Excluded by design** — brochure content, not app surface | — |
| `ChatPage` | **Excluded** — no chat controller exists in the backend; the web page is a frontend-only mock | — |

Design decisions carried across every screen:

- **Tables → cards.** A DataGrid with 11 columns is unusable on a phone. Each row
  keeps the two or three fields that *identify* the record; the rest moves to the
  detail screen.
- **Toolbar filters → bottom sheet.** Filters would otherwise eat the vertical
  space the list needs.
- **Numbered pager → infinite scroll,** prefetching four rows before the end.
- **Modals → bottom sheets**, full-screen on small displays.
- **Charts → the aggregate figure + a link into Reports.** A twelve-series bar
  chart on a 5-inch screen is noise.
- Brand colours, terminology, menu labels and status wording are taken verbatim
  from the web app.

## API inventory

Base URL `…/api/v1/`. Success bodies are wrapped in
`{ success, message, data, timestamp }`; list bodies wrap `data` in
`{ content, pageNumber, pageSize, totalElements, totalPages, last }`; errors are
`{ timestamp, status, error, message, path, validationErrors? }`.

> **Paging is not uniform.** `/students`, `/teachers`, `/staff`, `/users` and
> `/classes` take `page`/`size`/`sortBy`/`sortDirection`. Everything else uses
> Spring `Pageable` — `page`/`size`/`sort=field,dir`. Both forms are modelled.

| Retrofit interface | Endpoints covered |
|---|---|
| `AuthApi` | `auth/login`, `refresh-token`, `logout`, `register`, `forgot-password`, `reset-password`, `change-password`, `me` |
| `AcademicApi` | `academic-years` (+`set-current`), `departments`, `designations`, `classes`, `classes/{id}/sections`, `sections/{id}` (+`assign-class-teacher`), `classes/{id}/subjects`, `subjects/{id}`, `class-subject-teacher` |
| `StudentApi` | `students` CRUD, `{id}/status`, `{id}/photo`, `{id}/guardians[/{gid}]`, `{id}/medical-details`, `{id}/documents[/{docId}]`, `promote`, `{id}/transfer`, `{id}/mark-alumni`, `{id}/id-card/pdf`, `export/excel`, `import/excel` |
| `PeopleApi` | `teachers` CRUD + `status` + `id-card/pdf` + `export/excel`, `staff`, `users` (+`activate`/`deactivate`), `parents/me/children` |
| `AttendanceApi` | `attendance/students[/mark|/report|/{id}/summary|/monthly]`, `attendance/teachers[/mark|/report]`, `leave-applications[/my|/{id}|/approve|/reject]` |
| `FeeApi` | `fee-categories`, `fee-structures`, `student-fees[/{id}|/generate]`, `fee-payments[/{id}/receipt[/pdf]]`, `fees/dues-summary`, `scholarships` |
| `ExamApi` | `exam-types`, `exams` (+`results`), `exams/{id}/schedules`, `exam-schedules/{id}`, `marks[/roster|/entry|/report-card/{id}[/pdf]]` |
| `ClassroomApi` | `assignments` (multipart) `[/{id}/submissions[/my]|/submit]`, `assignment-submissions/{id}/grade`, `online-classes` |
| `LibraryApi` | `book-categories`, `books`, `book-issues[/issue|/{id}/return|/overdue]`, `library/dashboard` |
| `TransportApi` | `drivers`, `buses`, `routes[/{id}/pickup-points]`, `pickup-points/{id}`, `student-transport` |
| `HostelApi` | `hostels[/{id}/rooms]`, `hostel-rooms/{id}`, `hostel-students[/{id}/vacate]`, `hostel-visitors[/{id}/checkout]`, `hostel-fees[/{id}/mark-paid]` |
| `PayrollApi` | `salary-structures`, `payroll[/generate|/{id}/mark-paid|/{id}/salary-slip[/pdf]|/dashboard]` |
| `CommunicationApi` | `notices` (multipart), `events`, `calendar/birthdays`, `notifications[/my|/send]`, `public/admission-enquiries`, `admission-enquiries[/{id}/status]` |
| `ReportApi` | `analytics/dashboard`, `reports/{students-summary, teachers-summary, attendance-summary, fee-collection[/export/excel], payroll-summary, library-summary, transport-summary}` |
| `SettingsApi` | `settings/school-info`, `settings/system`, `roles[/{id}/permissions]`, `audit-logs`, `search/global` |

Example — sign-in, verified against the live server:

```http
POST /api/v1/auth/login
{ "username": "admin", "password": "Admin@123" }

200 OK
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9…",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9…",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": 1, "username": "admin", "role": "SUPER_ADMIN",
              "studentId": null, "teacherId": null, … }
  }
}
```

`user.studentId` / `teacherId` / `classId` / `sectionId` are populated only on
`/auth/login`, `/auth/refresh-token` and `/auth/me`. They are what lets the
self-service screens answer "which student am I" without an extra round trip.

## Testing

Unit tests (`app/src/test/`):

| Suite | Covers |
|---|---|
| `ErrorMapperTest` | Every HTTP status against real `GlobalExceptionHandler` bodies; 403 account-status wording preserved; 5xx message suppressed; unparseable bodies; network vs timeout; retryability |
| `LoginViewModelTest` | Empty-form validation, success path, 403 disabled account, error cleared on typing, double-submit guard |
| `ValidatorsTest` | Password policy against both seeded passwords, each character-class rule, confirm-match, amount rules |
| `FormattersTest` | Indian lakh currency grouping, date fallbacks, enum humanisation, nullable name handling |
| `PagedListStateTest` | Page-0 replace vs append, refresh de-dup, fatal-vs-append error handling, in-flight guard |
| `PagedLoaderTest` | The loader shared by all 15 list screens: append, no-op past the last page, refresh de-dup, append failure keeps rows, local remove/replace |
| `RoleTest` | All 11 seeded roles, graceful degradation for unknown roles, groupings match the web `navConfig` |
| `MenuTest` | Role gating matches the web sidebar per role; every menu entry has a registered route; form routes can't be mistaken for detail routes |

Instrumented (`app/src/androidTest/`): `LoginScreenTest` covers the three login
outcomes through the real Compose UI with a stub repository.

```bash
./gradlew testProdDebugUnitTest
./gradlew connectedProdDebugAndroidTest
```

## Backend / API gaps

None of these block the app — it works against the API exactly as deployed today.

| # | API | Current | Required change | Reason | Back-compat |
|---|---|---|---|---|---|
| 1 | **All** | `http://132.226.191.38:8080` (cleartext) | Terminate TLS in front of the API; then set `prodApiUrl` to `https://…` and delete the host from `network_security_config.xml` | JWTs currently cross the network unencrypted, and Android only allows this via an explicit exception | None — additive |
| 2 | `GET /notifications/my` | No read/unread state | Add `readAt` + `PATCH /notifications/{id}/read` + an `unreadCount` field | A mobile badge otherwise needs a full page fetch to count | Additive |
| 3 | Push delivery | `PUSH` rows are persisted and marked SENT but never delivered | `POST /devices/register {token, platform}` + FCM dispatch in `NotificationServiceImpl` | The channel exists in the schema but does nothing | Additive |
| 4 | Student/parent self-service reads | Several list endpoints exclude STUDENT/PARENT via `@PreAuthorize` | Widen `StudentAccessGuard` coverage so a student can read their own rows | Otherwise self-service tabs 403 | Widening only |
| 5 | Dashboard | 6 parallel calls | Optional `GET /dashboard/mobile` rollup | 6 round-trips is slow on mobile data; the app parallelises and caches meanwhile | Additive |
| 6 | `POST /students/{id}/photo` | Returns `{photoUrl}` | Documentation only | `school-frontend/src/api/studentsApi.ts` types this as `Student`, which is wrong — the API is right | — |

## Implementation status

**Complete** — infrastructure and data layer, 100% of the API surface the web app uses:

- Gradle build, 3 flavours, signing config, ProGuard rules, manifest, launcher icon
- Theme (colour/type/shape ported from the MUI theme), light + dark
- Shared component library: `StateHost`, `StatCard`, `EntityRowCard`, `SectionCard`,
  `DetailRow`, `Avatar`, `AppTextField`, `PasswordField`, `DropdownField`,
  `DateField`, `SearchField`, `FilterChipRow`, `StatusChip`, `ConfirmDialog`,
  `PagedLazyColumn`, `OfflineBanner`, `LoadMoreFooter`
- Networking: interceptor, single-flight refresh authenticator, error mapper,
  connectivity observer, encrypted session store
- 15 Retrofit interfaces · ~90 DTOs · 15 repository contracts + implementations
- Hilt wiring, Room cache schema, navigation graph with role-filtered menus
- Tests listed above

**Screens built — 34 destinations, covering every module in the web sidebar:**

| Area | Screens |
|---|---|
| Auth | Login, Register, Forgot/Reset password |
| Shell | Dashboard (role-aware), Academics / Admin / More hubs, Search |
| People | Student list, Student detail, Student form, Teacher list, Teacher detail, Staff, Users, My Children |
| Academics | Classes, Class detail, Attendance (4 tabs), Leave, Exams, Exam detail, Marks entry, Assignments, Assignment detail, Online classes |
| Administration | Fees, Fee setup, Scholarships, Payroll, Library, Transport, Hostel, Admission enquiries |
| Communication & insights | Notice board, Calendar, Notifications, Reports |
| Account | Profile (change password, sign out), Settings |

**Deliberately left to the web console** — each of these is a poor fit for a phone
and none of them blocks a mobile workflow:

| Not on mobile | Why |
|---|---|
| Student Excel import/export, bulk promote/transfer | Bulk operations over a spreadsheet; the API is wired (`StudentRepository.importExcel`/`exportExcel`/`promote`) but there is no sane phone UI for reviewing 40 skipped rows |
| Teacher/user create & edit forms | Account provisioning with password setting — an admin desk task |
| Academic setup writes (years, departments, designations, classes, sections, subjects) | Configured once a year; read views are present via Classes |
| Notice / assignment / online-class authoring | All three are multipart composers with attachments; reading them is the mobile need |
| Audit logs | Forensic tool over raw JSON diffs |
| System settings editing | Free-form key/value pairs whose semantics live server-side; shown read-only |
| Public marketing pages, chat | Brochure content; chat has no backend controller |

Repository methods exist for **all** of the above, so adding any of them is UI work
only. `navigation/Destinations.kt` holds the complete menu; `IMPLEMENTED_ROUTES`
filters it to what the graph registers, and `MenuTest` fails the build if a menu
entry ever points at an unregistered route.

### The pattern to follow

- **Paged list** — `TeacherListScreen`: a `PagedLoader` in the ViewModel plus
  `PagedListScaffold` in the screen. About 60 lines end to end.
- **Unpaged list** — `ClassListScreen` with `SimpleListScaffold`.
- **Detail** — `StudentDetailScreen`: `UiState<T>` + `StateHost` + `SectionCard`/`DetailRow`.
- **Form** — `StudentFormScreen`: one state class holding values and per-field
  errors, client validation via `Validators`, and the backend's `validationErrors`
  map merged onto the same fields on failure.

To add a screen: write the ViewModel and composable, register it in `AppNavHost`,
add the route to `IMPLEMENTED_ROUTES`, and — if it belongs in the menu — add a
`MenuEntry` with the same role set the web `navConfig.tsx` uses.

---

For backend and web setup see the repository root
[`README.md`](../README.md) and [`SCHEMA_CONTRACT.md`](../SCHEMA_CONTRACT.md).
