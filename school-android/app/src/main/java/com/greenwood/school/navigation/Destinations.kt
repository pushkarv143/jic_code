package com.greenwood.school.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.VideoCameraFront
import androidx.compose.ui.graphics.vector.ImageVector
import com.greenwood.school.data.remote.dto.MyAccessDto

/**
 * Every navigable destination, as a plain route string.
 *
 * Kept as constants + builder functions rather than type-safe nav args so the
 * graph reads the same way the web app's `AppRouter.tsx` does, and so deep links
 * can be added later without reshaping the graph.
 */
object Routes {

    /* Auth graph */
    const val AUTH_GRAPH = "auth"
    const val LOGIN = "login"
    const val FIRST_LOGIN_PASSWORD = "first-login-password"
    const val FORGOT_PASSWORD = "forgot-password"
    const val OTP_LOGIN = "otp-login"
    const val RESET_PASSWORD = "reset-password"
    const val PUBLIC_ENQUIRY = "public-admission-enquiry"

    /* Main graph */
    const val MAIN_GRAPH = "main"
    const val DASHBOARD = "dashboard"
    const val ACADEMICS_HUB = "academics"
    const val ADMIN_HUB = "administration"
    const val MORE_HUB = "more"

    const val STUDENTS = "students"
    const val STUDENT_DETAIL = "students/{studentId}"
    const val STUDENT_FORM = "student-form?studentId={studentId}"

    const val TEACHERS = "teachers"
    const val TEACHER_DETAIL = "teachers/{teacherId}"
    const val TEACHER_FORM = "teacher-form?teacherId={teacherId}"

    const val STAFF = "staff"
    const val USERS = "users"

    const val CLASSES = "classes"
    const val CLASS_DETAIL = "classes/{classId}"

    /** The homeroom teacher's own section. No id in the route: the server resolves it. */
    const val MY_CLASS = "my-class"

    const val ATTENDANCE = "attendance"
    const val LEAVE = "leave"

    /**
     * The signed-in user's own week. One route for teachers, students and parents:
     * the server decides whose timetable it is, so there is no id to pass.
     */
    const val MY_TIMETABLE = "my-timetable"

    const val EXAMS = "exams"
    const val EXAM_DETAIL = "exams/{examId}"
    const val MARKS_ENTRY = "exams/{examId}/schedules/{scheduleId}/marks"
    const val EXAM_RESULTS = "exams/{examId}/results"
    const val REPORT_CARD = "report-card/{studentId}?examId={examId}"

    const val ASSIGNMENTS = "assignments"
    const val ASSIGNMENT_DETAIL = "assignments/{assignmentId}"
    const val STUDY_MATERIALS = "study-materials"
    const val ONLINE_CLASSES = "online-classes"

    const val FEES = "fees"
    const val FEE_SETUP = "fees/setup"
    const val SCHOLARSHIPS = "fees/scholarships"
    const val STUDENT_FEE_DETAIL = "fees/{studentFeeId}"

    const val PAYROLL = "payroll"
    const val SALARY_STRUCTURES = "payroll/structures"
    const val SALARY_SLIP = "payroll/{payrollId}/slip"

    const val LIBRARY = "library"
    const val TRANSPORT = "transport"
    const val HOSTEL = "hostel"
    const val ADMISSIONS = "admission-enquiries"

    const val NOTICES = "notices"
    const val CALENDAR = "calendar"
    const val NOTIFICATIONS = "notifications"

    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "profile/change-password"
    const val SEARCH = "search"
    const val MY_CHILDREN = "my-children"

    /* Argument keys */
    const val ARG_STUDENT_ID = "studentId"
    const val ARG_TEACHER_ID = "teacherId"
    const val ARG_CLASS_ID = "classId"
    const val ARG_EXAM_ID = "examId"
    const val ARG_SCHEDULE_ID = "scheduleId"
    const val ARG_ASSIGNMENT_ID = "assignmentId"
    const val ARG_STUDENT_FEE_ID = "studentFeeId"
    const val ARG_PAYROLL_ID = "payrollId"

    /* Route builders — the only place ids get interpolated into a path. */
    fun studentDetail(id: Long) = "students/$id"
    fun studentForm(id: Long? = null) = "student-form?studentId=${id ?: -1}"
    fun teacherDetail(id: Long) = "teachers/$id"
    fun teacherForm(id: Long? = null) = "teacher-form?teacherId=${id ?: -1}"
    fun classDetail(id: Long) = "classes/$id"
    fun examDetail(id: Long) = "exams/$id"
    fun marksEntry(examId: Long, scheduleId: Long) = "exams/$examId/schedules/$scheduleId/marks"
    fun examResults(examId: Long) = "exams/$examId/results"
    fun reportCard(studentId: Long, examId: Long) = "report-card/$studentId?examId=$examId"
    fun assignmentDetail(id: Long) = "assignments/$id"
    fun studentFeeDetail(id: Long) = "fees/$id"
    fun salarySlip(id: Long) = "payroll/$id/slip"
}

/**
 * What this app can *render* for one menu: its Compose route and its icon.
 *
 * <p>Not what it may *show* — that is the server's answer now. This used to carry
 * `roles`, `permissions`, `module` and `requiresHomeroom` arrays duplicating the
 * web client's nav config, so deciding who saw a menu meant editing two files in
 * two languages and shipping two apps. The menu now lives in `menus` /
 * `role_menus`, arrives inside `GET /me/access` already filtered, and all this app
 * supplies is the half a server cannot know: where a key navigates to, and what it
 * looks like.
 *
 * <p>[key] is the join. It matches `menus.menu_key` and is deliberately neither
 * the label nor the path: a label gets renamed, and the path is the *web* route,
 * which means nothing here.
 */
data class MenuEntry(
    /** Matches `menus.menu_key`. */
    val key: String,
    /** Fallback label, used only when rendering without a server menu. */
    val label: String,
    val route: String,
    val icon: ImageVector,
)

/**
 * A group of menu entries under one heading.
 *
 * [key] matches `menus.menu_key` on the heading row ('SECTION_ACADEMICS', ...) and
 * is what decides which bottom-bar tab this section lands in. Not the [title]: that
 * is the server's label now, and routing a tab by a string an administrator can
 * rename would break the hubs the first time someone edited it.
 */
data class MenuSection(val key: String, val title: String, val entries: List<MenuEntry>)

/**
 * Every menu this app can navigate to, by `menus.menu_key`.
 *
 * <p>A key the server sends that is absent here is skipped: the phone does not
 * implement every screen the web app has, and this map is now what says so —
 * the job `IMPLEMENTED_ROUTES` used to do for the menu. A key present here but
 * never sent simply never renders, which is what an unassigned menu means.
 */
val MENU_CATALOGUE: Map<String, MenuEntry> = listOf(
    MenuEntry("DASHBOARD", "Dashboard", Routes.DASHBOARD, Icons.Outlined.SpaceDashboard),
    MenuEntry("MY_CHILDREN", "My Children", Routes.MY_CHILDREN, Icons.Outlined.FamilyRestroom),
    MenuEntry("STUDENTS", "Students", Routes.STUDENTS, Icons.Outlined.School),
    MenuEntry("TEACHERS", "Teachers", Routes.TEACHERS, Icons.Outlined.Badge),
    MenuEntry("MY_CLASS", "My Class", Routes.MY_CLASS, Icons.Outlined.Class),
    MenuEntry("CLASSES", "Classes & Subjects", Routes.CLASSES, Icons.Outlined.Class),
    MenuEntry("MY_TIMETABLE", "My Timetable", Routes.MY_TIMETABLE, Icons.Outlined.CalendarMonth),
    MenuEntry("ATTENDANCE", "Attendance", Routes.ATTENDANCE, Icons.Outlined.EventAvailable),
    MenuEntry("LEAVE", "Leave", Routes.LEAVE, Icons.Outlined.EventBusy),
    MenuEntry("EXAMS", "Exams & Marks", Routes.EXAMS, Icons.Outlined.Assignment),
    MenuEntry("ASSIGNMENTS", "Assignments", Routes.ASSIGNMENTS, Icons.Outlined.FactCheck),
    MenuEntry("STUDY_MATERIALS", "Study Materials", Routes.STUDY_MATERIALS, Icons.Outlined.LibraryBooks),
    MenuEntry("ONLINE_CLASSES", "Online Classes", Routes.ONLINE_CLASSES, Icons.Outlined.VideoCameraFront),
    MenuEntry("STAFF", "Staff", Routes.STAFF, Icons.Outlined.Groups),
    MenuEntry("FEES", "Fees", Routes.FEES, Icons.Outlined.Paid),
    MenuEntry("FEE_SETUP", "Fee Setup", Routes.FEE_SETUP, Icons.Outlined.Paid),
    MenuEntry("SCHOLARSHIPS", "Scholarships", Routes.SCHOLARSHIPS, Icons.Outlined.Paid),
    MenuEntry("PAYROLL", "Payroll", Routes.PAYROLL, Icons.Outlined.RequestQuote),
    MenuEntry("LIBRARY", "Library", Routes.LIBRARY, Icons.Outlined.MenuBook),
    MenuEntry("TRANSPORT", "Transport", Routes.TRANSPORT, Icons.Outlined.DirectionsBus),
    MenuEntry("HOSTEL", "Hostel", Routes.HOSTEL, Icons.Outlined.Apartment),
    MenuEntry("ADMISSION", "Admission Enquiries", Routes.ADMISSIONS, Icons.Outlined.HowToReg),
    MenuEntry("NOTICES", "Notice Board", Routes.NOTICES, Icons.Outlined.Campaign),
    MenuEntry("CALENDAR", "Calendar", Routes.CALENDAR, Icons.Outlined.CalendarMonth),
    MenuEntry("NOTIFICATIONS", "Notifications", Routes.NOTIFICATIONS, Icons.Outlined.Notifications),
    MenuEntry("REPORTS", "Reports", Routes.REPORTS, Icons.Outlined.BarChart),
    MenuEntry("PROFILE", "My Profile", Routes.PROFILE, Icons.Outlined.Person),
    MenuEntry("USERS", "Users", Routes.USERS, Icons.Outlined.ManageAccounts),
    MenuEntry("SETTINGS", "Settings", Routes.SETTINGS, Icons.Outlined.Settings),
    // CHAT is absent on purpose. The web page is a placeholder over seeded
    // conversations with no controller behind it, and the menu row is seeded
    // disabled — so even if it were sent, there is nothing here to navigate to.
).associateBy { it.key }

/**
 * The menu, as the server resolved it for this user.
 *
 * <p>The server has already applied every gate — the role's assignment, whether
 * the module is switched on, whether the user holds a homeroom section, and the
 * permission behind the screen. Nothing is re-decided here. What this does is
 * translate: server label and order, local route and icon.
 *
 * <p>Two things are dropped, both silently and both correctly. A section left with
 * no renderable entry goes with them, because a heading over an empty list reads as
 * a failure to load. And an entry whose key this app has no screen for is skipped —
 * the phone does not implement everything the web app does, and offering a menu
 * that navigates nowhere is worse than omitting it.
 *
 * <p>Returns empty when [access] is null, which is the window before the first
 * fetch lands, and empty for a role assigned nothing. Neither falls back to a
 * built-in menu: that fallback is what this replaced. The shell holds the splash
 * until access settles, so the empty window is not drawn.
 */
fun menuFromAccess(access: MyAccessDto?): List<MenuSection> =
    access?.menus.orEmpty()
        .map { section ->
            MenuSection(
                key = section.menuKey,
                title = section.label,
                entries = section.children.mapNotNull { node ->
                    MENU_CATALOGUE[node.menuKey]?.copy(label = node.label)
                },
            )
        }
        .filter { it.entries.isNotEmpty() }


/**
 * Every route the navigation graph actually registers.
 *
 * <p>AppNavHost guards each navigate() with this, because navigating to a route
 * the graph does not know throws at runtime. It is the belt to two braces: a menu
 * entry can only exist for a key in [MENU_CATALOGUE], and every entry there points
 * at a route below — but a deep link or a stale saved state can still ask for one
 * that is not wired up.
 *
 * <p>It no longer filters the menu. That filtering is [MENU_CATALOGUE]: a key the
 * server sends with no local screen has nowhere to go, so it never becomes an
 * entry in the first place.
 *
 * <p>Remaining work is tracked in `school-android/README.md` > "Implementation status".
 */
val IMPLEMENTED_ROUTES: Set<String> = setOf(
    Routes.DASHBOARD,
    Routes.MY_CHILDREN,
    Routes.STUDENTS,
    Routes.STUDENT_DETAIL,
    Routes.STUDENT_FORM,
    Routes.TEACHERS,
    Routes.TEACHER_DETAIL,
    Routes.TEACHER_FORM,
    Routes.STAFF,
    Routes.USERS,
    Routes.CLASSES,
    Routes.MY_CLASS,
    Routes.CLASS_DETAIL,
    Routes.ATTENDANCE,
    Routes.LEAVE,
    Routes.MY_TIMETABLE,
    Routes.EXAMS,
    Routes.EXAM_DETAIL,
    Routes.MARKS_ENTRY,
    Routes.ASSIGNMENTS,
    Routes.ASSIGNMENT_DETAIL,
    Routes.STUDY_MATERIALS,
    Routes.ONLINE_CLASSES,
    Routes.FEES,
    Routes.FEE_SETUP,
    Routes.SCHOLARSHIPS,
    Routes.PAYROLL,
    Routes.LIBRARY,
    Routes.TRANSPORT,
    Routes.HOSTEL,
    Routes.ADMISSIONS,
    Routes.NOTICES,
    Routes.CALENDAR,
    Routes.NOTIFICATIONS,
    Routes.REPORTS,
    Routes.PROFILE,
    Routes.SETTINGS,
    Routes.SEARCH,
)

/**
 * The four bottom-bar tabs. Fixed for every role; the hubs adapt their contents.
 *
 * [selectedIcon] is the filled counterpart of [icon]. Material 3 navigation bars
 * switch outlined to filled on selection — with the pill indicator alone, the
 * active tab is distinguishable only by a background tint, which is the first
 * thing to disappear for a user with low vision or a dimmed screen.
 */
data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

val BOTTOM_TABS = listOf(
    BottomTab(Routes.DASHBOARD, "Home", Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard),
    BottomTab(Routes.ACADEMICS_HUB, "Academics", Icons.Outlined.School, Icons.Filled.School),
    BottomTab(
        Routes.ADMIN_HUB,
        "Admin",
        Icons.Outlined.AccountBalanceWallet,
        Icons.Filled.AccountBalanceWallet,
    ),
    BottomTab(Routes.MORE_HUB, "More", Icons.Outlined.Apartment, Icons.Filled.Apartment),
)
