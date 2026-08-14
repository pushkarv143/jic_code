package com.greenwood.school.navigation

import androidx.compose.material.icons.Icons
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
import com.greenwood.school.core.common.Role

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
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
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

    const val ATTENDANCE = "attendance"
    const val LEAVE = "leave"

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
 * A menu entry, mirroring `NavItem` in `school-frontend/src/layouts/navConfig.tsx`
 * — same labels, same icons, same role gating.
 */
data class MenuEntry(
    val label: String,
    val route: String,
    val icon: ImageVector,
    /** Empty = visible to every authenticated role, matching the web's optional `roles`. */
    val roles: Set<Role> = emptySet(),
    /**
     * Permission names, any one of which reveals this entry — applied on top of
     * [roles], mirroring `NavItem.permissions` on the web. Empty = no permission
     * requirement.
     */
    val permissions: Set<String> = emptySet(),
) {
    fun isVisibleTo(role: Role): Boolean = roles.isEmpty() || role in roles

    /**
     * @param granted the signed-in role's permission names. An empty set means
     *   "unknown" — a session cached before the backend returned permissions — and
     *   skips the permission check rather than blanking the menu. Display only:
     *   the API enforces the same grants regardless of what is rendered.
     */
    fun isVisibleTo(role: Role, granted: Set<String>): Boolean = when {
        !isVisibleTo(role) -> false
        permissions.isEmpty() || granted.isEmpty() -> true
        else -> permissions.any { it in granted }
    }
}

data class MenuSection(val title: String, val entries: List<MenuEntry>)

private val MANAGEMENT = Role.MANAGEMENT
private val MANAGEMENT_AND_TEACHERS = Role.MANAGEMENT + Role.TEACHING

/**
 * The complete menu, one-for-one with the web sidebar's six groups.
 *
 * On mobile it is surfaced three ways: the four most-used destinations become the
 * bottom bar, "Academics"/"Admin" hubs list their group, and everything else lives
 * under "More". Nothing from the web nav is dropped.
 */
val MENU_SECTIONS: List<MenuSection> = listOf(
    MenuSection(
        title = "Overview",
        entries = listOf(
            MenuEntry("Dashboard", Routes.DASHBOARD, Icons.Outlined.SpaceDashboard),
            MenuEntry("My Children", Routes.MY_CHILDREN, Icons.Outlined.FamilyRestroom, setOf(Role.PARENT)),
        ),
    ),
    MenuSection(
        title = "Academics",
        entries = listOf(
            MenuEntry(
                "Students",
                Routes.STUDENTS,
                Icons.Outlined.School,
                MANAGEMENT_AND_TEACHERS + Role.RECEPTIONIST + Role.STUDENT,
                setOf("STUDENT_VIEW"),
            ),
            // Directory for management; the teacher's own record for a teacher —
            // AppNavHost redirects, mirroring the web's TeachersIndexRoute.
            MenuEntry(
                "Teachers",
                Routes.TEACHERS,
                Icons.Outlined.Badge,
                MANAGEMENT_AND_TEACHERS,
                setOf("TEACHER_VIEW"),
            ),
            MenuEntry("Classes & Sections", Routes.CLASSES, Icons.Outlined.Class, MANAGEMENT_AND_TEACHERS),
            MenuEntry(
                "Attendance",
                Routes.ATTENDANCE,
                Icons.Outlined.EventAvailable,
                MANAGEMENT_AND_TEACHERS + Role.SELF_SERVICE,
                setOf("ATTENDANCE_VIEW"),
            ),
            // No role filter — every role applies for leave, same as the web app.
            MenuEntry("Leave", Routes.LEAVE, Icons.Outlined.EventBusy),
            MenuEntry(
                "Exams & Marks",
                Routes.EXAMS,
                Icons.Outlined.Assignment,
                MANAGEMENT_AND_TEACHERS + Role.SELF_SERVICE,
            ),
            MenuEntry(
                "Assignments",
                Routes.ASSIGNMENTS,
                Icons.Outlined.FactCheck,
                MANAGEMENT_AND_TEACHERS + Role.SELF_SERVICE,
            ),
            MenuEntry(
                "Study Materials",
                Routes.STUDY_MATERIALS,
                Icons.Outlined.LibraryBooks,
                MANAGEMENT_AND_TEACHERS + Role.SELF_SERVICE,
                setOf("MATERIAL_VIEW"),
            ),
            MenuEntry(
                "Online Classes",
                Routes.ONLINE_CLASSES,
                Icons.Outlined.VideoCameraFront,
                MANAGEMENT_AND_TEACHERS + Role.SELF_SERVICE,
            ),
        ),
    ),
    MenuSection(
        title = "Administration",
        entries = listOf(
            MenuEntry("Staff", Routes.STAFF, Icons.Outlined.Groups, MANAGEMENT),
            MenuEntry(
                "Fees",
                Routes.FEES,
                Icons.Outlined.Paid,
                MANAGEMENT + Role.ACCOUNTANT + Role.SELF_SERVICE,
            ),
            // The web app nests these under the Fees route as tabs; on mobile they are
            // separate destinations so the Fees screen stays a single-purpose ledger.
            MenuEntry("Fee Setup", Routes.FEE_SETUP, Icons.Outlined.Paid, MANAGEMENT + Role.ACCOUNTANT),
            MenuEntry("Scholarships", Routes.SCHOLARSHIPS, Icons.Outlined.Paid, MANAGEMENT + Role.ACCOUNTANT),
            MenuEntry("Payroll", Routes.PAYROLL, Icons.Outlined.RequestQuote, MANAGEMENT + Role.ACCOUNTANT),
            MenuEntry("Library", Routes.LIBRARY, Icons.Outlined.MenuBook, MANAGEMENT + Role.LIBRARIAN),
            MenuEntry(
                "Transport",
                Routes.TRANSPORT,
                Icons.Outlined.DirectionsBus,
                MANAGEMENT + Role.RECEPTIONIST + Role.SELF_SERVICE,
            ),
            MenuEntry(
                "Hostel",
                Routes.HOSTEL,
                Icons.Outlined.Apartment,
                MANAGEMENT + Role.RECEPTIONIST + Role.SELF_SERVICE,
            ),
            MenuEntry(
                "Admission Enquiries",
                Routes.ADMISSIONS,
                Icons.Outlined.HowToReg,
                setOf(Role.SUPER_ADMIN, Role.PRINCIPAL, Role.RECEPTIONIST),
            ),
        ),
    ),
    MenuSection(
        title = "Communication",
        entries = listOf(
            MenuEntry("Notice Board", Routes.NOTICES, Icons.Outlined.Campaign),
            MenuEntry("Calendar", Routes.CALENDAR, Icons.Outlined.CalendarMonth),
            MenuEntry("Notifications", Routes.NOTIFICATIONS, Icons.Outlined.Notifications),
        ),
    ),
    MenuSection(
        title = "Insights",
        entries = listOf(
            MenuEntry("Reports", Routes.REPORTS, Icons.Outlined.BarChart, MANAGEMENT + Role.ACCOUNTANT),
        ),
    ),
    MenuSection(
        title = "Account",
        entries = listOf(
            MenuEntry("My Profile", Routes.PROFILE, Icons.Outlined.Person),
            MenuEntry(
                "Users",
                Routes.USERS,
                Icons.Outlined.ManageAccounts,
                setOf(Role.SUPER_ADMIN, Role.PRINCIPAL),
            ),
            MenuEntry(
                "Settings",
                Routes.SETTINGS,
                Icons.Outlined.Settings,
                setOf(Role.SUPER_ADMIN, Role.PRINCIPAL),
            ),
        ),
    ),
)

/**
 * Destinations that have a composable registered in [AppNavHost].
 *
 * [MENU_SECTIONS] above is the complete web-parity menu and is the specification;
 * this set is what is wired up so far. Menus are filtered through it so a tap can
 * never land on a route the graph doesn't know — navigating to an unregistered
 * route throws at runtime. Add the screen, add its route here, and it appears.
 *
 * Remaining work is tracked in `school-android/README.md` > "Implementation status".
 */
val IMPLEMENTED_ROUTES: Set<String> = setOf(
    Routes.DASHBOARD,
    Routes.MY_CHILDREN,
    Routes.STUDENTS,
    Routes.STUDENT_DETAIL,
    Routes.STUDENT_FORM,
    Routes.TEACHERS,
    Routes.TEACHER_DETAIL,
    Routes.STAFF,
    Routes.USERS,
    Routes.CLASSES,
    Routes.CLASS_DETAIL,
    Routes.ATTENDANCE,
    Routes.LEAVE,
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

fun menuForRole(role: Role, granted: Set<String> = emptySet()): List<MenuSection> = MENU_SECTIONS
    .map { section ->
        section.copy(
            entries = section.entries.filter {
                it.isVisibleTo(role, granted) && it.route in IMPLEMENTED_ROUTES
            },
        )
    }
    .filter { it.entries.isNotEmpty() }

/** The full, unfiltered menu — used by the README generator and by tests. */
fun fullMenuForRole(role: Role, granted: Set<String> = emptySet()): List<MenuSection> = MENU_SECTIONS
    .map { section -> section.copy(entries = section.entries.filter { it.isVisibleTo(role, granted) }) }
    .filter { it.entries.isNotEmpty() }

/** The four bottom-bar tabs. Fixed for every role; the hubs adapt their contents. */
data class BottomTab(val route: String, val label: String, val icon: ImageVector)

val BOTTOM_TABS = listOf(
    BottomTab(Routes.DASHBOARD, "Home", Icons.Outlined.SpaceDashboard),
    BottomTab(Routes.ACADEMICS_HUB, "Academics", Icons.Outlined.School),
    BottomTab(Routes.ADMIN_HUB, "Admin", Icons.Outlined.AccountBalanceWallet),
    BottomTab(Routes.MORE_HUB, "More", Icons.Outlined.Apartment),
)
