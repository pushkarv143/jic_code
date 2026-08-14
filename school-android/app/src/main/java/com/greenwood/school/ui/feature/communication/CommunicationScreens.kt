package com.greenwood.school.ui.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.AdmissionEnquiryDto
import com.greenwood.school.data.remote.dto.BirthdayPersonDto
import com.greenwood.school.data.remote.dto.CalendarEventDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.domain.repository.CommunicationRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.StatusChip
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

/* ------------------------------------------------------------------------- */
/* Notice board                                                               */
/* ------------------------------------------------------------------------- */

@Composable
fun NoticeBoardScreen(
    onBack: (() -> Unit)? = null,
    viewModel: NoticeBoardViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    var expandedId by remember { mutableStateOf<Long?>(null) }

    PagedListScaffold(
        title = "Notice Board",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        emptyTitle = "No notices",
        emptyMessage = "Notices published by the school appear here.",
        key = { it.id },
    ) { notice ->
        NoticeCard(
            notice = notice,
            isExpanded = expandedId == notice.id,
            // Tap to expand rather than navigating: a notice is a paragraph, and a
            // whole detail screen for it would be a wasted transition.
            onToggle = { expandedId = if (expandedId == notice.id) null else notice.id },
        )
    }
}

@Composable
private fun NoticeCard(notice: NoticeDto, isExpanded: Boolean, onToggle: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onToggle) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text(
                    notice.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                notice.targetRole?.let { StatusChip(it) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                notice.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    notice.publishedByName,
                    Formatters.dateTime(notice.publishedAt),
                    notice.expiryDate?.let { "expires ${Formatters.date(it)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@HiltViewModel
class NoticeBoardViewModel @Inject constructor(
    private val communicationRepository: CommunicationRepository,
) : ViewModel() {

    private val loader = PagedLoader(viewModelScope) { page, size ->
        communicationRepository.getNotices(page = page, size = size)
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

/* ------------------------------------------------------------------------- */
/* Calendar — events + birthdays                                              */
/* ------------------------------------------------------------------------- */

@Composable
fun CalendarScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Calendar",
                subtitle = Formatters.monthYear(state.month, state.year),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Events", "Birthdays").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }

            MonthStepper(
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                label = Formatters.monthYear(state.month, state.year),
            )

            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                tab == 0 -> EventsTab(state.events)
                else -> BirthdaysTab(state.students, state.teachers)
            }
        }
    }
}

@Composable
private fun MonthStepper(onPrevious: () -> Unit, onNext: () -> Unit, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("‹ Previous") }
        Spacer(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onNext) { Text("Next ›") }
    }
}

@Composable
private fun EventsTab(events: List<CalendarEventDto>) {
    if (events.isEmpty()) {
        EmptyView(title = "No events this month")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events.size, key = { events[it].id }) { index ->
            val event = events[index]
            EntityRowCard(
                title = event.title,
                subtitle = event.description,
                metadata = Formatters.date(event.eventDate),
                leadingInitials = Formatters.parseDate(event.eventDate)?.dayOfMonth?.toString() ?: "?",
                trailing = { StatusChip(event.eventType) },
            )
        }
    }
}

@Composable
private fun BirthdaysTab(students: List<BirthdayPersonDto>, teachers: List<BirthdayPersonDto>) {
    if (students.isEmpty() && teachers.isEmpty()) {
        EmptyView(title = "No birthdays this month")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (students.isNotEmpty()) {
            item(key = "students-header") { GroupHeader("Students") }
            items(students.size, key = { "s-${students[it].id}" }) { index ->
                BirthdayRow(students[index])
            }
        }
        if (teachers.isNotEmpty()) {
            item(key = "teachers-header") { GroupHeader("Teachers") }
            items(teachers.size, key = { "t-${teachers[it].id}" }) { index ->
                BirthdayRow(teachers[index])
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun BirthdayRow(person: BirthdayPersonDto) {
    EntityRowCard(
        title = person.displayName,
        subtitle = person.subtitle,
        metadata = Formatters.date(person.dateOfBirth),
        leadingInitials = Formatters.initials(person.displayName),
    )
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val communicationRepository: CommunicationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun previousMonth() = shiftMonth(-1)

    fun nextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Int) {
        val current = LocalDate.of(_state.value.year, _state.value.month, 1).plusMonths(delta.toLong())
        _state.update { it.copy(year = current.year, month = current.monthValue) }
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val current = _state.value
            val monthStart = LocalDate.of(current.year, current.month, 1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)

            coroutineScope {
                val events = async {
                    communicationRepository.getEvents(
                        startDate = Formatters.apiDate(monthStart),
                        endDate = Formatters.apiDate(monthEnd),
                    )
                }
                val birthdays = async { communicationRepository.getBirthdays(current.month).getOrNull() }

                when (val eventResult = events.await()) {
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            events = eventResult.data.sortedBy { event -> event.eventDate },
                            students = birthdays.await()?.students.orEmpty(),
                            teachers = birthdays.await()?.teachers.orEmpty(),
                        )
                    }

                    is ApiResult.Failure ->
                        _state.update { it.copy(isLoading = false, error = eventResult.error) }
                }
            }
        }
    }
}

data class CalendarUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val events: List<CalendarEventDto> = emptyList(),
    val students: List<BirthdayPersonDto> = emptyList(),
    val teachers: List<BirthdayPersonDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

/* ------------------------------------------------------------------------- */
/* Notifications                                                              */
/* ------------------------------------------------------------------------- */

@Composable
fun NotificationsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Notifications",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        emptyTitle = "No notifications",
        emptyMessage = "Messages sent to you by the school appear here.",
        key = { it.id },
    ) { notification ->
        EntityRowCard(
            title = notification.subject,
            subtitle = notification.message,
            metadata = listOfNotNull(
                Formatters.humanizeEnum(notification.type),
                Formatters.dateTime(notification.sentAt),
            ).joinToString(" · "),
            leadingInitials = Formatters.initials(notification.subject),
            trailing = { StatusChip(notification.status) },
        )
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val communicationRepository: CommunicationRepository,
) : ViewModel() {

    private val loader = PagedLoader(viewModelScope) { page, size ->
        communicationRepository.getMyNotifications(page, size)
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
}

/* ------------------------------------------------------------------------- */
/* Admission enquiries                                                        */
/* ------------------------------------------------------------------------- */

@Composable
fun AdmissionEnquiriesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AdmissionEnquiriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    PagedListScaffold(
        title = "Admission Enquiries",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        hasActiveFilters = state.status != null,
        message = state.message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = "No enquiries",
        key = { it.id },
        filterSheet = { dismiss ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Filter by status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FilterChipRow(
                    options = Constants.ADMISSION_STATUSES,
                    selected = state.status,
                    onSelected = viewModel::onStatusSelected,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) { enquiry ->
        EnquiryRow(
            enquiry = enquiry,
            onApprove = { viewModel.setStatus(enquiry, "APPROVED") },
            onReject = { viewModel.setStatus(enquiry, "REJECTED") },
        )
    }
}

@Composable
private fun EnquiryRow(
    enquiry: AdmissionEnquiryDto,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    EntityRowCard(
        title = enquiry.studentName,
        subtitle = "Class ${enquiry.classApplying} · ${enquiry.parentName}",
        metadata = listOfNotNull(enquiry.phone, enquiry.email, Formatters.date(enquiry.appliedAt))
            .joinToString(" · "),
        leadingInitials = Formatters.initials(enquiry.studentName),
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusChip(enquiry.status)
                if (enquiry.status == "PENDING") {
                    Row {
                        TextButton(onClick = onApprove) { Text("Approve") }
                        TextButton(onClick = onReject) { Text("Reject") }
                    }
                }
            }
        },
    )
}

@HiltViewModel
class AdmissionEnquiriesViewModel @Inject constructor(
    private val communicationRepository: CommunicationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdmissionEnquiriesUiState())
    val state: StateFlow<AdmissionEnquiriesUiState> = _state.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        communicationRepository.getEnquiries(status = _state.value.status, page = page, size = size)
    }
    val list = loader.state

    init {
        loader.refresh()
    }

    fun onStatusSelected(status: String?) {
        _state.update { it.copy(status = status) }
        loader.refresh()
    }

    fun setStatus(enquiry: AdmissionEnquiryDto, status: String) {
        viewModelScope.launch {
            when (val result = communicationRepository.updateEnquiryStatus(enquiry.id, status)) {
                is ApiResult.Success -> {
                    loader.replaceWhere({ it.id == enquiry.id }, result.data)
                    _state.update {
                        it.copy(message = UiMessage.success("Enquiry ${status.lowercase()}."))
                    }
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class AdmissionEnquiriesUiState(
    val status: String? = null,
    val message: UiMessage? = null,
)
