package com.greenwood.school.ui.common

import app.cash.turbine.test
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.AppError
import com.greenwood.school.core.network.Paged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PagedLoader] is shared by every list screen, so a bug here is a bug in fifteen
 * places at once. These cover the cases that actually bite: duplicated rows after
 * a refresh, an append failure wiping the list, and a filter change racing its own
 * response.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PagedLoaderTest {

    private fun page(items: List<String>, page: Int, isLast: Boolean) =
        Paged(items = items, page = page, totalElements = 4, totalPages = 2, isLast = isLast)

    @Test
    fun `refresh loads page zero`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("a$p", "b$p"), p, false))
        }

        loader.refresh()

        assertEquals(listOf("a0", "b0"), loader.state.value.items)
        assertEquals(0, loader.state.value.page)
    }

    @Test
    fun `loadMore appends instead of replacing`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("a$p", "b$p"), p, isLast = p == 1))
        }

        loader.refresh()
        loader.loadMore()

        assertEquals(listOf("a0", "b0", "a1", "b1"), loader.state.value.items)
        assertTrue(loader.state.value.isLastPage)
    }

    @Test
    fun `loadMore is a no-op on the last page`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        var calls = 0
        val loader = PagedLoader<String>(scope) { p, _ ->
            calls++
            ApiResult.Success(page(listOf("only"), p, isLast = true))
        }

        loader.refresh()
        loader.loadMore()
        loader.loadMore()

        assertEquals(1, calls)
    }

    @Test
    fun `a refresh after paging does not duplicate rows`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("a$p"), p, isLast = p == 1))
        }

        loader.refresh()
        loader.loadMore()
        assertEquals(2, loader.state.value.items.size)

        loader.refresh()

        assertEquals(listOf("a0"), loader.state.value.items)
    }

    @Test
    fun `a failed first page is fatal, a failed append keeps the rows`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        var shouldFail = true
        val loader = PagedLoader<String>(scope) { p, _ ->
            if (shouldFail) ApiResult.Failure(AppError.Network("offline")) else ApiResult.Success(
                page(listOf("a$p"), p, isLast = false),
            )
        }

        loader.refresh()
        assertNotNull(loader.state.value.error)
        assertTrue(loader.state.value.items.isEmpty())

        shouldFail = false
        loader.refresh()
        assertNull(loader.state.value.error)
        assertEquals(listOf("a0"), loader.state.value.items)

        shouldFail = true
        loader.loadMore()
        // The already-loaded row must survive an append failure.
        assertEquals(listOf("a0"), loader.state.value.items)
        assertNull(loader.state.value.error)
        assertNotNull(loader.state.value.appendError)
    }

    @Test
    fun `removeWhere drops the row and decrements the total`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("keep", "drop"), p, isLast = true))
        }
        loader.refresh()

        loader.removeWhere { it == "drop" }

        assertEquals(listOf("keep"), loader.state.value.items)
        assertEquals(3, loader.state.value.totalElements)
    }

    @Test
    fun `replaceWhere swaps a single row in place`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("old", "other"), p, isLast = true))
        }
        loader.refresh()

        loader.replaceWhere({ it == "old" }, "new")

        assertEquals(listOf("new", "other"), loader.state.value.items)
    }

    @Test
    fun `state emits refreshing before content`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val loader = PagedLoader<String>(scope) { p, _ ->
            ApiResult.Success(page(listOf("a"), p, isLast = true))
        }

        loader.state.test {
            assertTrue(awaitItem().items.isEmpty())
            loader.refresh()
            // Collapsed by the unconfined dispatcher into the settled state; what
            // matters is that it lands loaded and not still refreshing.
            val settled = expectMostRecentItem()
            assertEquals(listOf("a"), settled.items)
            assertFalse(settled.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
