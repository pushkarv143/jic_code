package com.greenwood.school.ui.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.AttendanceSummaryReportDto
import com.greenwood.school.data.remote.dto.FeeCollectionReportDto
import com.greenwood.school.data.remote.dto.LibrarySummaryReportDto
import com.greenwood.school.data.remote.dto.PayrollSummaryReportDto
import com.greenwood.school.data.remote.dto.StudentsSummaryReportDto
import com.greenwood.school.data.remote.dto.TeachersSummaryReportDto
import com.greenwood.school.data.remote.dto.TransportSummaryReportDto
import com.greenwood.school.domain.repository.ReportRepository
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.DetailRow
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.SectionCard
import com.greenwood.school.ui.theme.BrandIndigo
import com.greenwood.school.ui.theme.StatusPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * All seven reports behind one scrollable tab row.
 *
 * The web version draws Recharts bar/line/pie charts. On a phone those become
 * horizontal proportion bars: the same comparison, readable at a glance, with the
 * exact figure printed next to each row instead of hidden behind a tooltip.
 */
@Composable
fun ReportsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Students", "Teachers", "Attendance", "Fees", "Payroll", "Library", "Transport")

    Scaffold(topBar = { AppTopBar(title = "Reports", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }

            if (state.isLoading) {
                FullScreenLoader("Loading reports…")
                return@Column
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    when (tab) {
                        0 -> StudentsReport(state.students)
                        1 -> TeachersReport(state.teachers)
                        2 -> AttendanceReport(state.attendance)
                        3 -> FeesReport(state.fees)
                        4 -> PayrollReport(state.payroll)
                        5 -> LibraryReport(state.library)
                        else -> TransportReport(state.transport)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentsReport(report: StudentsSummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Overview") {
            DetailRow("Active students", Formatters.number(report.totalActive))
        }
        if (report.byClass.isNotEmpty()) {
            SectionCard(title = "By class") {
                val max = report.byClass.maxOf { it.count }.coerceAtLeast(1)
                report.byClass.forEach { row ->
                    ProportionBar(row.className, Formatters.number(row.count), row.count.toFloat() / max)
                }
            }
        }
        if (report.byStatus.isNotEmpty()) {
            SectionCard(title = "By status") {
                report.byStatus.forEach { row ->
                    DetailRow(Formatters.humanizeEnum(row.status), Formatters.number(row.count))
                }
            }
        }
    }
}

@Composable
private fun TeachersReport(report: TeachersSummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Overview") {
            DetailRow("Active teachers", Formatters.number(report.totalActive))
        }
        if (report.byDepartment.isNotEmpty()) {
            SectionCard(title = "By department") {
                val max = report.byDepartment.maxOf { it.count }.coerceAtLeast(1)
                report.byDepartment.forEach { row ->
                    ProportionBar(row.departmentName, Formatters.number(row.count), row.count.toFloat() / max)
                }
            }
        }
    }
}

@Composable
private fun AttendanceReport(report: AttendanceSummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Overview") {
            DetailRow("Average attendance", Formatters.percent(report.averagePercentage))
        }
        if (report.byClass.isNotEmpty()) {
            SectionCard(title = "By class") {
                report.byClass.forEach { row ->
                    ProportionBar(
                        label = row.className,
                        value = Formatters.percent(row.percentage),
                        // Already a percentage, so the bar is the value itself.
                        fraction = (row.percentage / 100.0).toFloat(),
                        color = if (row.percentage >= HEALTHY_ATTENDANCE) {
                            StatusPalette.positive
                        } else {
                            StatusPalette.caution
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeesReport(report: FeeCollectionReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Collection") {
            DetailRow("Total due", Formatters.currency(report.totalDue))
            DetailRow("Collected", Formatters.currency(report.totalCollected))
            DetailRow("Outstanding", Formatters.currency(report.totalOutstanding))
        }
        if (report.byCategory.isNotEmpty()) {
            SectionCard(title = "By category") {
                val max = report.byCategory.maxOf { it.collected }.coerceAtLeast(1.0)
                report.byCategory.forEach { row ->
                    ProportionBar(
                        row.categoryName,
                        Formatters.currency(row.collected),
                        (row.collected / max).toFloat(),
                    )
                }
            }
        }
        if (report.byMonth.isNotEmpty()) {
            SectionCard(title = "By month") {
                val max = report.byMonth.maxOf { it.collected }.coerceAtLeast(1.0)
                report.byMonth.forEach { row ->
                    ProportionBar(row.month, Formatters.currency(row.collected), (row.collected / max).toFloat())
                }
            }
        }
    }
}

@Composable
private fun PayrollReport(report: PayrollSummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Overview") {
            DetailRow("Paid", Formatters.currency(report.totalPaidAmount))
            DetailRow("Pending", Formatters.currency(report.totalPendingAmount))
        }
        if (report.byMonth.isNotEmpty()) {
            SectionCard(title = "By month") {
                val max = report.byMonth.maxOf { it.paidAmount }.coerceAtLeast(1.0)
                report.byMonth.forEach { row ->
                    ProportionBar(row.month, Formatters.currency(row.paidAmount), (row.paidAmount / max).toFloat())
                }
            }
        }
    }
}

@Composable
private fun LibraryReport(report: LibrarySummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Overview") {
            DetailRow("Titles", Formatters.number(report.totalBooks))
            DetailRow("Issued", Formatters.number(report.totalIssued))
            DetailRow("Overdue", Formatters.number(report.totalOverdue))
        }
        if (report.byCategory.isNotEmpty()) {
            SectionCard(title = "By category") {
                val max = report.byCategory.maxOf { it.count }.coerceAtLeast(1)
                report.byCategory.forEach { row ->
                    ProportionBar(row.categoryName, Formatters.number(row.count), row.count.toFloat() / max)
                }
            }
        }
    }
}

@Composable
private fun TransportReport(report: TransportSummaryReportDto?) {
    if (report == null) {
        EmptyView(title = "Report unavailable")
        return
    }
    SectionCard(title = "Overview") {
        DetailRow("Buses", Formatters.number(report.totalBuses))
        DetailRow("Routes", Formatters.number(report.totalRoutes))
        DetailRow("Students using transport", Formatters.number(report.studentsUsingTransport))
    }
}

/**
 * A labelled proportion bar — the mobile substitute for a chart series.
 *
 * [fraction] is relative to the largest value in the series, so the widest bar
 * always fills the row and small values stay visible rather than collapsing to
 * a sliver.
 */
@Composable
private fun ProportionBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color = BrandIndigo,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.15f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            coroutineScope {
                // All seven fire together: the tab row is instant afterwards, and the
                // reports are small aggregate payloads rather than record dumps.
                val students = async { reportRepository.getStudentsSummary().getOrNull() }
                val teachers = async { reportRepository.getTeachersSummary().getOrNull() }
                val attendance = async { reportRepository.getAttendanceSummary().getOrNull() }
                val fees = async { reportRepository.getFeeCollection().getOrNull() }
                // year is required by the endpoint; omitting it failed the request and left
                // the payroll tab permanently blank.
                val payroll = async { reportRepository.getPayrollSummary(LocalDate.now().year).getOrNull() }
                val library = async { reportRepository.getLibrarySummary().getOrNull() }
                val transport = async { reportRepository.getTransportSummary().getOrNull() }

                _state.value = ReportsUiState(
                    isLoading = false,
                    students = students.await(),
                    teachers = teachers.await(),
                    attendance = attendance.await(),
                    fees = fees.await(),
                    payroll = payroll.await(),
                    library = library.await(),
                    transport = transport.await(),
                )
            }
        }
    }
}

data class ReportsUiState(
    val isLoading: Boolean = true,
    val students: StudentsSummaryReportDto? = null,
    val teachers: TeachersSummaryReportDto? = null,
    val attendance: AttendanceSummaryReportDto? = null,
    val fees: FeeCollectionReportDto? = null,
    val payroll: PayrollSummaryReportDto? = null,
    val library: LibrarySummaryReportDto? = null,
    val transport: TransportSummaryReportDto? = null,
)

private const val HEALTHY_ATTENDANCE = 75.0
