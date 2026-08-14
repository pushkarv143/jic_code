package com.greenwood.school.ui.feature.fees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.greenwood.school.core.common.Formatters
import com.greenwood.school.core.common.Validators
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.getOrNull
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.FeeCategoryDto
import com.greenwood.school.data.remote.dto.FeeStructureDto
import com.greenwood.school.data.remote.dto.FeeStructureRequestDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.ScholarshipDto
import com.greenwood.school.data.remote.dto.ScholarshipRequestDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.FeeRepository
import com.greenwood.school.ui.common.PagedLoader
import com.greenwood.school.ui.common.UiMessage
import com.greenwood.school.ui.components.AppTextField
import com.greenwood.school.ui.components.AppTopBar
import com.greenwood.school.ui.components.ConfirmDialog
import com.greenwood.school.ui.components.DateField
import com.greenwood.school.ui.components.DropdownField
import com.greenwood.school.ui.components.EmptyView
import com.greenwood.school.ui.components.EntityRowCard
import com.greenwood.school.ui.components.EnumDropdownField
import com.greenwood.school.ui.components.ErrorView
import com.greenwood.school.ui.components.FullScreenLoader
import com.greenwood.school.ui.components.PagedListScaffold
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
/* Fee setup — categories and per-class structures.                           */
/* ------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeSetupScreen(
    onBack: (() -> Unit)? = null,
    viewModel: FeeSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showStructureSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Fee setup", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (tab == 0) showCategorySheet = true else showStructureSheet = true },
            ) {
                Icon(Icons.Outlined.Add, "Add")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Categories", "Structures").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }

            when {
                state.isLoading -> FullScreenLoader()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                tab == 0 -> if (state.categories.isEmpty()) {
                    EmptyView(title = "No fee categories", message = "Add one with the + button.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.categories.size, key = { state.categories[it].id }) { index ->
                            val category = state.categories[index]
                            EntityRowCard(
                                title = category.name,
                                subtitle = category.description,
                                leadingInitials = Formatters.initials(category.name),
                                trailing = {
                                    IconButton(onClick = { pendingDelete = "category" to category.id }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                else -> if (state.structures.isEmpty()) {
                    EmptyView(title = "No fee structures", message = "Add one with the + button.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.structures.size, key = { state.structures[it].id }) { index ->
                            val structure = state.structures[index]
                            EntityRowCard(
                                title = structure.feeCategoryName ?: "Fee",
                                subtitle = "${structure.className.orEmpty()} · ${structure.academicYearName.orEmpty()}",
                                metadata = "Due ${Formatters.date(structure.dueDate)}",
                                leadingInitials = Formatters.initials(structure.feeCategoryName.orEmpty()),
                                trailing = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(
                                            Formatters.currency(structure.amount),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        IconButton(onClick = { pendingDelete = "structure" to structure.id }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, sheetState = sheetState) {
            NameDescriptionSheet(
                title = "New fee category",
                isSubmitting = state.isSubmitting,
                onSubmit = { name, description ->
                    viewModel.createCategory(name, description) { showCategorySheet = false }
                },
            )
        }
    }

    if (showStructureSheet) {
        ModalBottomSheet(onDismissRequest = { showStructureSheet = false }, sheetState = sheetState) {
            FeeStructureSheet(
                categories = state.categories,
                classes = state.classes,
                years = state.years,
                isSubmitting = state.isSubmitting,
                onSubmit = { request -> viewModel.createStructure(request) { showStructureSheet = false } },
            )
        }
    }

    pendingDelete?.let { (kind, id) ->
        ConfirmDialog(
            title = "Delete this ${if (kind == "category") "category" else "structure"}?",
            message = "This cannot be undone.",
            onConfirm = {
                if (kind == "category") viewModel.deleteCategory(id) else viewModel.deleteStructure(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun NameDescriptionSheet(
    title: String,
    isSubmitting: Boolean,
    onSubmit: (name: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        AppTextField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "Name",
            required = true,
            error = nameError,
        )
        AppTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            singleLine = false,
            minLines = 2,
        )
        Button(
            onClick = {
                nameError = Validators.required(name, "Name")
                if (nameError == null) onSubmit(name.trim(), description.trim().takeIf { it.isNotBlank() })
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Save") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FeeStructureSheet(
    categories: List<FeeCategoryDto>,
    classes: List<SchoolClassDto>,
    years: List<AcademicYearDto>,
    isSubmitting: Boolean,
    onSubmit: (FeeStructureRequestDto) -> Unit,
) {
    var category by remember { mutableStateOf<FeeCategoryDto?>(null) }
    var schoolClass by remember { mutableStateOf<SchoolClassDto?>(null) }
    var year by remember { mutableStateOf(years.firstOrNull { it.isCurrent } ?: years.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("New fee structure", style = MaterialTheme.typography.titleMedium)

        DropdownField(
            label = "Category",
            options = categories,
            selected = category,
            onSelected = { category = it },
            optionLabel = { it.name },
            required = true,
            error = errors["category"],
        )
        DropdownField(
            label = "Class",
            options = classes,
            selected = schoolClass,
            onSelected = { schoolClass = it },
            optionLabel = { it.className },
            required = true,
            error = errors["class"],
        )
        DropdownField(
            label = "Academic year",
            options = years,
            selected = year,
            onSelected = { year = it },
            optionLabel = { it.yearName },
            required = true,
            error = errors["year"],
        )
        AppTextField(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            required = true,
            error = errors["amount"],
            keyboardType = KeyboardType.Decimal,
        )
        DateField(label = "Due date", value = dueDate, onValueChange = { dueDate = it }, required = true)

        Button(
            onClick = {
                errors = buildMap {
                    if (category == null) put("category", "Select a category")
                    if (schoolClass == null) put("class", "Select a class")
                    if (year == null) put("year", "Select an academic year")
                    Validators.positiveNumber(amount, "Amount")?.let { put("amount", it) }
                }
                if (errors.isEmpty()) {
                    onSubmit(
                        FeeStructureRequestDto(
                            classId = schoolClass!!.id,
                            academicYearId = year!!.id,
                            feeCategoryId = category!!.id,
                            amount = amount.toDouble(),
                            dueDate = Formatters.apiDate(dueDate),
                        ),
                    )
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Save") }
        Spacer(Modifier.height(16.dp))
    }
}

@HiltViewModel
class FeeSetupViewModel @Inject constructor(
    private val feeRepository: FeeRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeeSetupUiState())
    val state: StateFlow<FeeSetupUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val categories = async { feeRepository.getCategories() }
                val structures = async { feeRepository.getStructures() }
                val classes = async { academicRepository.getClasses().getOrNull().orEmpty() }
                val years = async { academicRepository.getAcademicYears().getOrNull().orEmpty() }

                when (val categoryResult = categories.await()) {
                    is ApiResult.Success -> _state.value = FeeSetupUiState(
                        categories = categoryResult.data,
                        structures = structures.await().getOrNull().orEmpty(),
                        classes = classes.await(),
                        years = years.await(),
                        isLoading = false,
                    )

                    is ApiResult.Failure ->
                        _state.value = FeeSetupUiState(isLoading = false, error = categoryResult.error)
                }
            }
        }
    }

    fun createCategory(name: String, description: String?, onDone: () -> Unit) = submit(onDone) {
        feeRepository.createCategory(name, description)
    }

    fun createStructure(request: FeeStructureRequestDto, onDone: () -> Unit) = submit(onDone) {
        feeRepository.createStructure(request)
    }

    fun deleteCategory(id: Long) = delete("Category deleted.") { feeRepository.deleteCategory(id) }

    fun deleteStructure(id: Long) = delete("Structure deleted.") { feeRepository.deleteStructure(id) }

    private fun delete(successMessage: String, block: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = UiMessage.success(successMessage)) }
                    // A delete can cascade (a category in use blocks its structures), so
                    // reload both lists rather than patching one of them locally.
                    load()
                }

                is ApiResult.Failure -> _state.update { it.copy(message = UiMessage.error(result.error)) }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun submit(onDone: () -> Unit, block: suspend () -> ApiResult<*>) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, message = UiMessage.success("Saved.")) }
                    load()
                    onDone()
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(isSubmitting = false, message = UiMessage.error(result.error)) }
            }
        }
    }
}

data class FeeSetupUiState(
    val categories: List<FeeCategoryDto> = emptyList(),
    val structures: List<FeeStructureDto> = emptyList(),
    val classes: List<SchoolClassDto> = emptyList(),
    val years: List<AcademicYearDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val message: UiMessage? = null,
)

/* ------------------------------------------------------------------------- */
/* Scholarships                                                               */
/* ------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarshipsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ScholarshipsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    PagedListScaffold(
        title = "Scholarships",
        state = list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage,
        emptyTitle = "No scholarships awarded",
        onAdd = { showSheet = true },
        key = { it.id },
    ) { scholarship ->
        EntityRowCard(
            title = scholarship.studentName ?: "Student #${scholarship.studentId}",
            subtitle = scholarship.title,
            metadata = listOfNotNull(scholarship.admissionNumber, scholarship.academicYearName)
                .joinToString(" · ")
                .ifBlank { null },
            leadingInitials = Formatters.initials(scholarship.studentName.orEmpty()),
            trailing = {
                Text(
                    if (scholarship.type == "PERCENTAGE") {
                        Formatters.percent(scholarship.amount, decimals = 0)
                    } else {
                        Formatters.currency(scholarship.amount)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            },
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            ScholarshipSheet(
                years = state.years,
                isSubmitting = state.isSubmitting,
                onSubmit = { request -> viewModel.create(request) { showSheet = false } },
            )
        }
    }
}

@Composable
private fun ScholarshipSheet(
    years: List<AcademicYearDto>,
    isSubmitting: Boolean,
    onSubmit: (ScholarshipRequestDto) -> Unit,
) {
    var studentId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Constants.SCHOLARSHIP_TYPES.first()) }
    var year by remember { mutableStateOf(years.firstOrNull { it.isCurrent } ?: years.firstOrNull()) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Award a scholarship", style = MaterialTheme.typography.titleMedium)

        // The backend keys scholarships by student id; there is no name-search
        // endpoint scoped to this screen, so the id is entered directly (it is on
        // every student card and detail screen).
        AppTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = "Student ID",
            required = true,
            error = errors["studentId"],
            keyboardType = KeyboardType.Number,
            supportingText = "Shown on the student's profile",
        )
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title",
            required = true,
            error = errors["title"],
        )
        EnumDropdownField(
            label = "Type",
            options = Constants.SCHOLARSHIP_TYPES,
            selected = type,
            onSelected = { type = it },
            required = true,
        )
        AppTextField(
            value = amount,
            onValueChange = { amount = it },
            label = if (type == "PERCENTAGE") "Percentage" else "Amount",
            required = true,
            error = errors["amount"],
            keyboardType = KeyboardType.Decimal,
        )
        DropdownField(
            label = "Academic year",
            options = years,
            selected = year,
            onSelected = { year = it },
            optionLabel = { it.yearName },
            required = true,
            error = errors["year"],
        )

        Button(
            onClick = {
                errors = buildMap {
                    if (studentId.toLongOrNull() == null) put("studentId", "Enter a valid student ID")
                    Validators.required(title, "Title")?.let { put("title", it) }
                    Validators.positiveNumber(amount, "Amount")?.let { put("amount", it) }
                    if (year == null) put("year", "Select an academic year")
                }
                if (errors.isEmpty()) {
                    onSubmit(
                        ScholarshipRequestDto(
                            studentId = studentId.toLong(),
                            title = title.trim(),
                            amount = amount.toDouble(),
                            type = type,
                            academicYearId = year!!.id,
                        ),
                    )
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Award") }
        Spacer(Modifier.height(16.dp))
    }
}

@HiltViewModel
class ScholarshipsViewModel @Inject constructor(
    private val feeRepository: FeeRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScholarshipsUiState())
    val state: StateFlow<ScholarshipsUiState> = _state.asStateFlow()

    private val loader = PagedLoader(viewModelScope) { page, size ->
        feeRepository.getScholarships(page = page, size = size)
    }
    val list = loader.state

    init {
        loader.refresh()
        viewModelScope.launch {
            _state.update { it.copy(years = academicRepository.getAcademicYears().getOrNull().orEmpty()) }
        }
    }

    fun create(request: ScholarshipRequestDto, onDone: () -> Unit) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            when (val result = feeRepository.createScholarship(request)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, message = UiMessage.success("Scholarship awarded."))
                    }
                    loader.refresh()
                    onDone()
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(isSubmitting = false, message = UiMessage.error(result.error)) }
            }
        }
    }

    fun refresh() = loader.refresh()
    fun loadMore() = loader.loadMore()
    fun consumeMessage() = _state.update { it.copy(message = null) }
}

data class ScholarshipsUiState(
    val years: List<AcademicYearDto> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: UiMessage? = null,
)
