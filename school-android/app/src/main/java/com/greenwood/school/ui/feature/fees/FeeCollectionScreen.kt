package com.greenwood.school.ui.feature.fees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.common.DocumentOpener
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Role
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.common.isSelfService
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.dto.DuesSummaryDto
import com.greenwood.school.data.remote.dto.FeePaymentRequestDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.StudentFeeDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.FeeRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppCard
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.DateField
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.EnumDropdownField
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.FilterChipRow
import com.greenwood.school.ui.components.PagedListScaffold
import com.greenwood.school.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fee collection — the ledger of per-student dues plus the "collect payment" flow.
 *
 * A student or parent sees only their own rows and no payment sheet; the backend
 * enforces this too, but scoping the request by `studentId` means they never hit a
 * 403 in the first place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCollectionScreen(
    onBack: (() -> Unit)? = null,
    viewModel: FeeCollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    var payingFor by remember { mutableStateOf<StudentFeeDto?>(null) }
    val sheetState = rememberModalBottomSheetState()

    PagedListScaffold(
        title = if (state.canCollect) "Fees" else "My Fees",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        hasActiveFilters = state.hasActiveFilters,
        message = state.message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = "No fee records",
        emptyMessage = "Dues appear here once they've been generated for the year.",
        key = { it.id },
        header = { state.summary?.let { DuesSummaryCard(it) } },
        filterSheet = { dismiss ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Filter fees", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (state.canCollect) {
                    DropdownField(
                        label = "Class",
                        options = state.classes,
                        selected = state.selectedClass,
                        onSelected = viewModel::onClassSelected,
                        optionLabel = { it.className },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text("Status", style = MaterialTheme.typography.labelLarge)
                FilterChipRow(
                    options = Constants.STUDENT_FEE_STATUSES,
                    selected = state.status,
                    onSelected = viewModel::onStatusSelected,
                )
                Row {
                    TextButton(
                        onClick = {
                            viewModel.onClassSelected(null)
                            viewModel.onStatusSelected(null)
                        },
                    ) { Text("Clear all") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = dismiss) { Text("Done") }
                }
                Spacer(Modifier.height(16.dp))
            }
        },
    ) { fee ->
        FeeRow(
            fee = fee,
            canCollect = state.canCollect && fee.balance > 0,
            onCollect = { payingFor = fee },
            onOpenReceipt = { viewModel.openLatestReceipt(fee) },
        )
    }

    payingFor?.let { fee ->
        ModalBottomSheet(onDismissRequest = { payingFor = null }, sheetState = sheetState) {
            CollectPaymentSheet(
                fee = fee,
                isSubmitting = state.isSubmitting,
                onSubmit = { amount, mode, date, transactionId ->
                    viewModel.collect(fee, amount, mode, date, transactionId) { payingFor = null }
                },
            )
        }
    }
}

@Composable
private fun DuesSummaryCard(summary: DuesSummaryDto) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp)) {
            SummaryFigure("Collected", Formatters.currency(summary.totalCollected), Modifier.weight(1f))
            SummaryFigure("Outstanding", Formatters.currency(summary.totalOutstanding), Modifier.weight(1f))
            SummaryFigure("Total due", Formatters.currency(summary.totalDue), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeeRow(
    fee: StudentFeeDto,
    canCollect: Boolean,
    onCollect: () -> Unit,
    onOpenReceipt: () -> Unit,
) {
    EntityRowCard(
        title = fee.studentName ?: fee.feeCategoryName ?: "Fee #${fee.id}",
        subtitle = listOfNotNull(fee.feeCategoryName, fee.className, fee.sectionName)
            .joinToString(" · ")
            .ifBlank { null },
        metadata = "Due ${Formatters.date(fee.dueDate)} · " +
            "${Formatters.currency(fee.amountPaid)} of ${Formatters.currency(fee.amountDue)}",
        leadingInitials = Formatters.initials(fee.studentName ?: fee.feeCategoryName.orEmpty()),
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusChip(fee.status)
                if (canCollect) {
                    TextButton(onClick = onCollect) { Text("Collect") }
                }
            }
        },
        // Any paid-into row has a receipt worth opening; a wholly unpaid one doesn't.
        onClick = if (fee.amountPaid > 0) onOpenReceipt else null,
    )
}

@Composable
private fun CollectPaymentSheet(
    fee: StudentFeeDto,
    isSubmitting: Boolean,
    onSubmit: (amount: Double, mode: String, date: LocalDate, transactionId: String?) -> Unit,
) {
    var amount by remember { mutableStateOf(fee.balance.toString()) }
    var mode by remember { mutableStateOf(Constants.PAYMENT_MODES.first()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var transactionId by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Collect payment", style = MaterialTheme.typography.titleMedium)
        Text(
            "${fee.studentName ?: ""} · ${fee.feeCategoryName ?: ""}".trim(' ', '·'),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Balance ${Formatters.currency(fee.balance)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        AppTextField(
            value = amount,
            onValueChange = { amount = it; amountError = null },
            label = "Amount",
            required = true,
            error = amountError,
            keyboardType = KeyboardType.Decimal,
        )

        EnumDropdownField(
            label = "Payment mode",
            options = Constants.PAYMENT_MODES,
            selected = mode,
            onSelected = { mode = it },
            required = true,
        )

        DateField(label = "Payment date", value = date, onValueChange = { date = it }, required = true)

        // Only meaningful for non-cash modes; the backend accepts it as optional.
        if (mode != "CASH") {
            AppTextField(
                value = transactionId,
                onValueChange = { transactionId = it },
                label = "Transaction / cheque reference",
            )
        }

        Button(
            onClick = {
                val parsed = amount.toDoubleOrNull()
                amountError = Validators.positiveNumber(amount, "Amount")
                    ?: if (parsed != null && parsed > fee.balance) {
                        "Amount cannot exceed the ${Formatters.currency(fee.balance)} balance"
                    } else {
                        null
                    }
                if (amountError == null && parsed != null) {
                    onSubmit(parsed, mode, date, transactionId.takeIf { it.isNotBlank() })
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (isSubmitting) "Saving…" else "Record payment") }

        Spacer(Modifier.height(16.dp))
    }
}

@HiltViewModel
class FeeCollectionViewModel @Inject constructor(
    private val feeRepository: FeeRepository,
    private val academicRepository: AcademicRepository,
    private val documentOpener: DocumentOpener,
    sessionManager: SessionManager,
) : ViewModel() {

    private val user = sessionManager.currentUser
    private val role = Role.from(user?.role)

    /** Students and parents read their own ledger; they never collect payments. */
    private val ownStudentId: Long? = user?.studentId.takeIf { role.isSelfService }
    private val canCollect = !role.isSelfService

    private val _state = MutableStateFlow(FeeCollectionUiState(canCollect = canCollect))
    val state: StateFlow<FeeCollectionUiState> = _state.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        val current = _state.value
        feeRepository.getStudentFees(
            studentId = ownStudentId,
            classId = current.selectedClass?.id,
            status = current.status,
            page = page,
            size = size,
        )
    }
    val list = loader.state

    init {
        loader.refresh()
        if (canCollect) {
            viewModelScope.launch {
                _state.update { it.copy(classes = academicRepository.getClasses().getOrNull().orEmpty()) }
            }
        }
        loadSummary()
    }

    fun onClassSelected(schoolClass: SchoolClassDto?) {
        _state.update { it.copy(selectedClass = schoolClass) }
        loader.refresh()
        loadSummary()
    }

    fun onStatusSelected(status: String?) {
        _state.update { it.copy(status = status) }
        loader.refresh()
    }

    fun collect(
        fee: StudentFeeDto,
        amount: Double,
        mode: String,
        date: LocalDate,
        transactionId: String?,
        onDone: () -> Unit,
    ) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = feeRepository.collectPayment(
                FeePaymentRequestDto(
                    studentFeeId = fee.id,
                    amount = amount,
                    paymentDate = Formatters.apiDate(date),
                    paymentMode = mode,
                    transactionId = transactionId,
                ),
            )
            when (result) {
                is ApiResult.Success -> {
                    // The endpoint returns the recalculated dues row; swap it in so the
                    // balance and status update without refetching the page.
                    result.data.updatedStudentFee?.let { updated ->
                        loader.replaceWhere({ it.id == fee.id }, updated)
                    }
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            message = UiMessage.success(
                                "Payment of ${Formatters.currency(amount)} recorded.",
                            ),
                        )
                    }
                    loadSummary()
                    onDone()
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(isSubmitting = false, message = UiMessage.error(result.error)) }
            }
        }
    }

    /**
     * Opens the PDF receipt for a dues row's most recent payment.
     *
     * The list payload doesn't carry payments (only `GET /student-fees/{id}` does),
     * so fetch the single row first, then download the receipt for its latest
     * payment — one extra call, only when the user actually asks for a receipt.
     */
    fun openLatestReceipt(fee: StudentFeeDto) {
        viewModelScope.launch {
            val detail = feeRepository.getStudentFee(fee.id).getOrNull()
            val payment = detail?.feePayments?.maxByOrNull { it.id }
            if (payment == null) {
                _state.update {
                    it.copy(message = UiMessage("No receipt available for this record yet.", isError = true))
                }
                return@launch
            }
            when (val result = feeRepository.downloadReceipt(payment.id)) {
                is ApiResult.Success -> if (!documentOpener.open(result.data)) {
                    _state.update {
                        it.copy(message = UiMessage("No app on this device can open a PDF.", isError = true))
                    }
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    private fun loadSummary() {
        if (!canCollect) return
        viewModelScope.launch {
            val summary = feeRepository.getDuesSummary(classId = _state.value.selectedClass?.id).getOrNull()
            _state.update { it.copy(summary = summary) }
        }
    }

    fun refresh() {
        loader.refresh()
        loadSummary()
    }

    fun loadMore() = loader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class FeeCollectionUiState(
    val canCollect: Boolean = false,
    val classes: List<SchoolClassDto> = emptyList(),
    val selectedClass: SchoolClassDto? = null,
    val status: String? = null,
    val summary: DuesSummaryDto? = null,
    val isSubmitting: Boolean = false,
    val message: UiMessage? = null,
) {
    val hasActiveFilters: Boolean get() = selectedClass != null || status != null
}
