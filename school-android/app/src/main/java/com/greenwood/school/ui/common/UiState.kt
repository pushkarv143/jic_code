package com.greenwood.school.ui.common

import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.Paged

/**
 * The five states every data-backed screen can be in — the exact set the brief
 * calls for (loading / success / empty / error / offline), with offline and
 * unauthorized folded into [Error] because [AppError] already distinguishes them.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}

val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success)?.data

/** Collapses an empty collection into [UiState.Empty] so screens don't each re-check. */
fun <T : Collection<*>> T.toUiState(): UiState<T> = if (isEmpty()) UiState.Empty else UiState.Success(this)

fun <T> Paged<T>.toUiState(): UiState<Paged<T>> = if (isEmpty) UiState.Empty else UiState.Success(this)

/**
 * Accumulating list state for the infinite-scroll screens (students, fees, books…).
 *
 * Kept separate from [UiState] because a paged list has a second axis: it can be
 * showing content *and* loading the next page *and* holding an append error, all
 * at once. Flattening that into one sealed type produces unreadable screens.
 */
data class PagedListState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val isLastPage: Boolean = false,
    val totalElements: Long = 0,
    /** First load or a pull-to-refresh. */
    val isRefreshing: Boolean = false,
    /** Loading the next page while content is already on screen. */
    val isAppending: Boolean = false,
    /** Fatal for the whole list — shown as a full-screen error. */
    val error: AppError? = null,
    /** Only the append failed — shown as a retry row at the bottom. */
    val appendError: AppError? = null,
) {
    val isInitialLoad: Boolean get() = isRefreshing && items.isEmpty()
    val isEmpty: Boolean get() = items.isEmpty() && !isRefreshing && error == null
    val canLoadMore: Boolean get() = !isLastPage && !isAppending && !isRefreshing && appendError == null

    fun startRefresh(): PagedListState<T> = copy(isRefreshing = true, error = null, appendError = null)

    fun startAppend(): PagedListState<T> = copy(isAppending = true, appendError = null)

    /** Applies a freshly loaded page, replacing on page 0 and appending otherwise. */
    fun applyPage(paged: Paged<T>): PagedListState<T> = copy(
        items = if (paged.page == 0) paged.items else items + paged.items,
        page = paged.page,
        isLastPage = paged.isLast,
        totalElements = paged.totalElements,
        isRefreshing = false,
        isAppending = false,
        error = null,
        appendError = null,
    )

    fun applyError(error: AppError): PagedListState<T> =
        if (items.isEmpty()) {
            copy(isRefreshing = false, isAppending = false, error = error)
        } else {
            // Content is already on screen — don't blow it away for a failed page.
            copy(isRefreshing = false, isAppending = false, appendError = error)
        }
}

/**
 * A one-shot message for the snackbar. Modelled as an event rather than state so
 * it isn't re-shown on rotation.
 */
data class UiMessage(
    val text: String,
    val isError: Boolean = false,
    /** Distinguishes two identical messages so the UI knows to show it again. */
    val id: Long = nextId(),
) {
    companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter

        fun error(error: AppError) = UiMessage(error.userMessage, isError = true)
        fun success(text: String) = UiMessage(text, isError = false)
    }
}
