package com.greenwood.school.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.greenwood.school.core.common.Role
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.ui.feature.attendance.AttendanceScreen
import com.greenwood.school.ui.feature.auth.ForgotPasswordScreen
import com.greenwood.school.ui.feature.auth.LoginScreen
import com.greenwood.school.ui.feature.auth.OtpLoginScreen
import com.greenwood.school.ui.feature.auth.RegisterScreen
import com.greenwood.school.ui.feature.classes.ClassDetailScreen
import com.greenwood.school.ui.feature.classes.ClassListScreen
import com.greenwood.school.ui.feature.classroom.AssignmentDetailScreen
import com.greenwood.school.ui.feature.classroom.AssignmentListScreen
import com.greenwood.school.ui.feature.classroom.OnlineClassesScreen
import com.greenwood.school.ui.feature.communication.AdmissionEnquiriesScreen
import com.greenwood.school.ui.feature.communication.CalendarScreen
import com.greenwood.school.ui.feature.communication.NoticeBoardScreen
import com.greenwood.school.ui.feature.communication.NotificationsScreen
import com.greenwood.school.ui.feature.dashboard.DashboardScreen
import com.greenwood.school.ui.feature.exams.ExamDetailScreen
import com.greenwood.school.ui.feature.exams.ExamListScreen
import com.greenwood.school.ui.feature.exams.MarksEntryScreen
import com.greenwood.school.ui.feature.facilities.HostelScreen
import com.greenwood.school.ui.feature.facilities.TransportScreen
import com.greenwood.school.ui.feature.fees.FeeCollectionScreen
import com.greenwood.school.ui.feature.fees.FeeSetupScreen
import com.greenwood.school.ui.feature.fees.ScholarshipsScreen
import com.greenwood.school.ui.feature.hub.HubScreen
import com.greenwood.school.ui.feature.leave.LeaveScreen
import com.greenwood.school.ui.components.AppFooter
import com.greenwood.school.ui.feature.library.LibraryScreen
import com.greenwood.school.ui.feature.materials.StudyMaterialsScreen
import com.greenwood.school.ui.feature.payroll.PayrollScreen
import com.greenwood.school.ui.feature.people.MyChildrenScreen
import com.greenwood.school.ui.feature.people.StaffListScreen
import com.greenwood.school.ui.feature.people.TeacherDetailScreen
import com.greenwood.school.ui.feature.people.TeacherFormScreen
import com.greenwood.school.ui.feature.people.TeacherListScreen
import com.greenwood.school.ui.feature.people.UserListScreen
import com.greenwood.school.ui.feature.profile.ProfileScreen
import com.greenwood.school.ui.feature.reports.ReportsScreen
import com.greenwood.school.ui.feature.search.SearchScreen
import com.greenwood.school.ui.feature.settings.SettingsScreen
import com.greenwood.school.ui.feature.students.StudentDetailScreen
import com.greenwood.school.ui.feature.students.StudentFormScreen
import com.greenwood.school.ui.feature.students.StudentListScreen

/**
 * Top-level graph: an unauthenticated `auth` graph and an authenticated `main`
 * graph, with the start destination chosen from the restored session. This is the
 * Android counterpart of `ProtectedRoute` in the web router — the guard is
 * structural (you cannot be in the main graph without a session) rather than a
 * per-screen check.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    isSignedIn: Boolean,
    currentUser: UserDto?,
) {
    // The footer is anchored here, outside the NavHost, so it survives every
    // navigation — auth screens and the signed-in shell alike — rather than each
    // screen having to include it. weight(1f) gives the NavHost the remaining
    // height, which keeps the footer pinned to the bottom instead of being pushed
    // off-screen by a tall screen's content.
    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (isSignedIn) Routes.MAIN_GRAPH else Routes.AUTH_GRAPH,
            modifier = Modifier.weight(1f),
        ) {
            authGraph(navController)

            composable(Routes.MAIN_GRAPH) {
                MainShell(
                    role = Role.from(currentUser?.role),
                    permissions = currentUser?.permissions.orEmpty().toSet(),
                    ownStudentId = currentUser?.studentId,
                    ownTeacherId = currentUser?.teacherId,
                    onSignedOut = {
                        navController.navigate(Routes.AUTH_GRAPH) {
                            popUpTo(Routes.MAIN_GRAPH) { inclusive = true }
                        }
                    },
                )
            }
        }

        AppFooter()
    }
}

private fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(startDestination = Routes.LOGIN, route = Routes.AUTH_GRAPH) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    navController.navigate(Routes.MAIN_GRAPH) {
                        // Clear the auth graph so Back from the dashboard exits the app
                        // rather than returning to Login.
                        popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onNavigateToOtpLogin = { navController.navigate(Routes.OTP_LOGIN) },
            )
        }
        composable(Routes.OTP_LOGIN) {
            OtpLoginScreen(
                onSignedIn = {
                    navController.navigate(Routes.MAIN_GRAPH) {
                        // Same as the password path: clear the auth graph so Back
                        // from the dashboard exits rather than returning to sign-in.
                        popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
                    }
                },
                onBack = navController::popBackStack,
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onBack = navController::popBackStack)
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = navController::popBackStack)
        }
    }
}

/**
 * The signed-in shell: bottom bar plus a nested nav host.
 *
 * A nested host (rather than one flat graph) is what lets a detail screen hide the
 * bottom bar and own the full height, while tab switches keep their own back stacks.
 */
@Composable
private fun MainShell(
    role: Role,
    permissions: Set<String>,
    ownStudentId: Long?,
    ownTeacherId: Long?,
    onSignedOut: () -> Unit,
) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = remember(currentRoute) { currentRoute in BOTTOM_TABS.map { it.route } }

    // The role half of WRITE_ROLES on TeacherController; each call site pairs it with
    // the specific permission it needs.
    val canWriteTeachers = role in Role.MANAGEMENT

    /**
     * Guarded navigation. Navigating to a route with no registered composable throws,
     * so every menu- and dashboard-driven jump goes through here. Menus are already
     * filtered by [IMPLEMENTED_ROUTES]; this is the belt to that braces.
     */
    val go: (String) -> Unit = { route -> if (route in IMPLEMENTED_ROUTES) nav.navigate(route) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                NavigationBar {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Routes.DASHBOARD) {

                /* ---- Home + hubs -------------------------------------------- */

                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenNotices = { go(Routes.NOTICES) },
                        onOpenCalendar = { go(Routes.CALENDAR) },
                        onOpenSearch = { go(Routes.SEARCH) },
                        onOpenFees = { go(Routes.FEES) },
                        onOpenAttendance = { go(Routes.ATTENDANCE) },
                    )
                }

                listOf(Routes.ACADEMICS_HUB, Routes.ADMIN_HUB, Routes.MORE_HUB).forEach { hubRoute ->
                    composable(hubRoute) {
                        HubScreen(
                            role = role,
                            tabRoute = hubRoute,
                            onNavigate = go,
                            onSignOut = if (hubRoute == Routes.MORE_HUB) onSignedOut else null,
                            permissions = permissions,
                        )
                    }
                }

                /* ---- Students ------------------------------------------------ */

                composable(Routes.STUDENTS) {
                    // A STUDENT has no directory to browse — the same menu entry opens
                    // their own record instead, mirroring the web's StudentsIndexRoute.
                    // /students/{id} already refuses any id but their own, so this is a
                    // shortcut, not the access decision.
                    //
                    // It has to be a redirect rather than rendering the screen here:
                    // StudentDetailViewModel reads its id from the route arguments, and
                    // this route has none, so rendering inline threw. Same shape as the
                    // teacher redirect below.
                    val redirectToOwnRecord = role == Role.STUDENT && ownStudentId != null
                    LaunchedEffect(redirectToOwnRecord) {
                        if (redirectToOwnRecord) {
                            nav.navigate(Routes.studentDetail(ownStudentId)) {
                                popUpTo(Routes.STUDENTS) { inclusive = true }
                            }
                        }
                    }
                    if (redirectToOwnRecord) return@composable

                    StudentListScreen(
                        onOpenStudent = { nav.navigate(Routes.studentDetail(it)) },
                        onBack = nav::popBackStack,
                        // WRITE_ROLES on StudentController — anyone else would be
                        // rejected after filling in the form, so don't offer it.
                        // STUDENT_CREATE is checked too so revoking the grant hides
                        // the FAB without needing an app release.
                        onAddStudent = if (role in Role.MANAGEMENT &&
                            (permissions.isEmpty() || "STUDENT_CREATE" in permissions)
                        ) {
                            { nav.navigate(Routes.studentForm()) }
                        } else {
                            null
                        },
                    )
                }

                composable(
                    route = Routes.STUDENT_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_STUDENT_ID) { type = NavType.LongType }),
                ) { entry ->
                    val studentId = entry.arguments?.getLong(Routes.ARG_STUDENT_ID) ?: 0L
                    StudentDetailScreen(
                        onBack = nav::popBackStack,
                        // WRITE_ROLES on StudentController is management-only, the same
                        // guard the add button uses.
                        onEdit = if (role in Role.MANAGEMENT &&
                            (permissions.isEmpty() || "STUDENT_UPDATE" in permissions)
                        ) {
                            { nav.navigate(Routes.studentForm(studentId)) }
                        } else {
                            null
                        },
                    )
                }

                composable(
                    route = Routes.STUDENT_FORM,
                    arguments = listOf(
                        navArgument(Routes.ARG_STUDENT_ID) {
                            type = NavType.LongType
                            // -1 means "new student"; see StudentFormViewModel.
                            defaultValue = -1L
                        },
                    ),
                ) {
                    StudentFormScreen(onSaved = nav::popBackStack, onBack = nav::popBackStack)
                }

                /* ---- People --------------------------------------------------- */

                composable(Routes.TEACHERS) {
                    // A teacher opening "Teachers" wants their own record, not the staff
                    // directory. TeacherDetailScreen reads its id from the nav argument
                    // rather than a parameter, so this is a redirect rather than a direct
                    // render — popUpTo keeps Back from bouncing between the two.
                    val redirectToOwnRecord = role in Role.TEACHING && ownTeacherId != null
                    LaunchedEffect(redirectToOwnRecord) {
                        if (redirectToOwnRecord) {
                            nav.navigate(Routes.teacherDetail(ownTeacherId)) {
                                popUpTo(Routes.TEACHERS) { inclusive = true }
                            }
                        }
                    }

                    if (!redirectToOwnRecord) {
                        TeacherListScreen(
                            onOpenTeacher = { nav.navigate(Routes.teacherDetail(it)) },
                            onBack = nav::popBackStack,
                            // WRITE_ROLES on TeacherController is management-only, so
                            // anyone else would be rejected after filling in the form.
                            // TEACHER_CREATE is checked too, so revoking the grant hides
                            // the button without needing an app release.
                            onAddTeacher = if (canWriteTeachers &&
                                (permissions.isEmpty() || "TEACHER_CREATE" in permissions)
                            ) {
                                { nav.navigate(Routes.teacherForm()) }
                            } else {
                                null
                            },
                        )
                    }
                }

                composable(
                    route = Routes.TEACHER_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_TEACHER_ID) { type = NavType.LongType }),
                ) { entry ->
                    val teacherId = entry.arguments?.getLong(Routes.ARG_TEACHER_ID) ?: 0L
                    TeacherDetailScreen(
                        onBack = nav::popBackStack,
                        onEdit = if (canWriteTeachers &&
                            (permissions.isEmpty() || "TEACHER_UPDATE" in permissions)
                        ) {
                            { nav.navigate(Routes.teacherForm(teacherId)) }
                        } else {
                            null
                        },
                    )
                }

                composable(
                    route = Routes.TEACHER_FORM,
                    arguments = listOf(
                        navArgument(Routes.ARG_TEACHER_ID) {
                            type = NavType.LongType
                            // -1 means "new teacher"; see TeacherFormViewModel.
                            defaultValue = -1L
                        },
                    ),
                ) {
                    TeacherFormScreen(onSaved = nav::popBackStack, onBack = nav::popBackStack)
                }

                composable(Routes.STAFF) { StaffListScreen(onBack = nav::popBackStack) }
                composable(Routes.USERS) { UserListScreen(onBack = nav::popBackStack) }

                composable(Routes.MY_CHILDREN) {
                    MyChildrenScreen(
                        onOpenStudent = { nav.navigate(Routes.studentDetail(it)) },
                        onBack = nav::popBackStack,
                    )
                }

                /* ---- Academics ------------------------------------------------- */

                composable(Routes.CLASSES) {
                    ClassListScreen(
                        onOpenClass = { nav.navigate(Routes.classDetail(it)) },
                        onBack = nav::popBackStack,
                    )
                }

                composable(
                    route = Routes.CLASS_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_CLASS_ID) { type = NavType.LongType }),
                ) {
                    ClassDetailScreen(onBack = nav::popBackStack)
                }

                composable(Routes.ATTENDANCE) { AttendanceScreen(onBack = nav::popBackStack) }
                composable(Routes.LEAVE) { LeaveScreen(onBack = nav::popBackStack) }

                composable(Routes.EXAMS) {
                    ExamListScreen(
                        onOpenExam = { nav.navigate(Routes.examDetail(it)) },
                        onBack = nav::popBackStack,
                    )
                }

                composable(
                    route = Routes.EXAM_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_EXAM_ID) { type = NavType.LongType }),
                ) {
                    ExamDetailScreen(
                        onOpenMarksEntry = { examId, scheduleId ->
                            nav.navigate(Routes.marksEntry(examId, scheduleId))
                        },
                        onBack = nav::popBackStack,
                    )
                }

                composable(
                    route = Routes.MARKS_ENTRY,
                    arguments = listOf(
                        navArgument(Routes.ARG_EXAM_ID) { type = NavType.LongType },
                        navArgument(Routes.ARG_SCHEDULE_ID) { type = NavType.LongType },
                    ),
                ) {
                    MarksEntryScreen(onBack = nav::popBackStack)
                }

                composable(Routes.ASSIGNMENTS) {
                    AssignmentListScreen(
                        onOpenAssignment = { nav.navigate(Routes.assignmentDetail(it)) },
                        onBack = nav::popBackStack,
                    )
                }

                composable(
                    route = Routes.ASSIGNMENT_DETAIL,
                    arguments = listOf(navArgument(Routes.ARG_ASSIGNMENT_ID) { type = NavType.LongType }),
                ) {
                    AssignmentDetailScreen(onBack = nav::popBackStack)
                }

                composable(Routes.STUDY_MATERIALS) { StudyMaterialsScreen(onBack = nav::popBackStack) }

                composable(Routes.ONLINE_CLASSES) { OnlineClassesScreen(onBack = nav::popBackStack) }

                /* ---- Administration --------------------------------------------- */

                composable(Routes.FEES) { FeeCollectionScreen(onBack = nav::popBackStack) }

                composable(Routes.FEE_SETUP) { FeeSetupScreen(onBack = nav::popBackStack) }
                composable(Routes.SCHOLARSHIPS) { ScholarshipsScreen(onBack = nav::popBackStack) }
                composable(Routes.PAYROLL) { PayrollScreen(onBack = nav::popBackStack) }
                composable(Routes.LIBRARY) { LibraryScreen(onBack = nav::popBackStack) }
                composable(Routes.TRANSPORT) { TransportScreen(onBack = nav::popBackStack) }
                composable(Routes.HOSTEL) { HostelScreen(onBack = nav::popBackStack) }
                composable(Routes.ADMISSIONS) { AdmissionEnquiriesScreen(onBack = nav::popBackStack) }

                /* ---- Communication + insights ------------------------------------- */

                composable(Routes.NOTICES) { NoticeBoardScreen(onBack = nav::popBackStack) }
                composable(Routes.CALENDAR) { CalendarScreen(onBack = nav::popBackStack) }
                composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = nav::popBackStack) }
                composable(Routes.REPORTS) { ReportsScreen(onBack = nav::popBackStack) }

                /* ---- Account ------------------------------------------------------- */

                composable(Routes.PROFILE) {
                    ProfileScreen(onSignedOut = onSignedOut, onBack = nav::popBackStack)
                }

                composable(Routes.SETTINGS) { SettingsScreen(onBack = nav::popBackStack) }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        onOpenStudent = { nav.navigate(Routes.studentDetail(it)) },
                        onOpenTeacher = { nav.navigate(Routes.teacherDetail(it)) },
                        onBack = nav::popBackStack,
                    )
                }
            }
        }
    }
}
