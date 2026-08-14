package com.greenwood.school.ui.common

import com.greenwood.school.core.common.Constants
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.Paged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the load / append / refresh cycle of one paged list so each ViewModel is
 * left with only the part that differs: its filters and its fetch call.
 *
 * Every list screen in the app went through the same four steps by hand before
 * this existed — mark refreshing, call page N, fold the result into
 * [PagedListState], guard against overlapping loads. Doing it once here is what
 * keeps ~15 list ViewModels down to a dozen lines each.
 *
 * @param fetch called with a zero-based page index; read your current filter
 *        state inside the lambda so a filter change is picked up automatically
 *        on the next [refresh].
 */
class PagedLoader<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = Constants.DEFAULT_PAGE_SIZE,
    private val fetch: suspend (page: Int, size: Int) -> ApiResult<Paged<T>>,
) {

    private val _state = MutableStateFlow(PagedListState<T>())
    val state: StateFlow<PagedListState<T>> = _state.asStateFlow()

    /**
     * Cancelled before each new load. Without this, a fast filter change can land
     * its stale response after the newer one and overwrite the list.
     */
    private var job: Job? = null

    /** Reloads from page 0, replacing whatever is on screen. */
    fun refresh() {
        job?.cancel()
        _state.update { it.startRefresh() }
        job = load(page = 0)
    }

    /** Appends the next page. No-op while a load is in flight or the list is exhausted. */
    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        _state.update { it.startAppend() }
        job = load(page = current.page + 1)
    }

    /** Retries only the failed append, keeping the rows already on screen. */
    fun retryAppend() {
        val current = _state.value
        if (current.appendError == null) return
        _state.update { it.startAppend() }
        job = load(page = current.page + 1)
    }

    /**
     * Drops one row locally after a successful delete, so the list doesn't have to
     * round-trip the whole page back just to lose a single item.
     */
    fun removeWhere(predicate: (T) -> Boolean) = _state.update { current ->
        val remaining = current.items.filterNot(predicate)
        current.copy(
            items = remaining,
            totalElements = (current.totalElements - (current.items.size - remaining.size)).coerceAtLeast(0),
        )
    }

    /** Replaces one row in place after an edit (status change, mark-paid, approve…). */
    fun replaceWhere(predicate: (T) -> Boolean, replacement: T) = _state.update { current ->
        current.copy(items = current.items.map { if (predicate(it)) replacement else it })
    }

    private fun load(page: Int) = scope.launch {
        when (val result = fetch(page, pageSize)) {
            is ApiResult.Success -> _state.update { it.applyPage(result.data) }
            is ApiResult.Failure -> _state.update { it.applyError(result.error) }
        }
    }
}
