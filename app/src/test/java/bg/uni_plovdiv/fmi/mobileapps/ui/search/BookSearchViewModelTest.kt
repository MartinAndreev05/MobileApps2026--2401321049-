package bg.uni_plovdiv.fmi.mobileapps.ui.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkError
import bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkException
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import bg.uni_plovdiv.fmi.mobileapps.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class BookSearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dune = OnlineBook("/works/1", "Dune", "Frank Herbert", null, 1965)
    private val repository: BookRepository = mock()
    private val viewModel = BookSearchViewModel(repository)

    @Test
    fun initialState_isIdle() {
        assertEquals(SearchUiState.Idle, viewModel.state.value)
    }

    @Test
    fun tooShortQuery_isIgnored() = runTest {
        viewModel.search(" d ")

        assertEquals(SearchUiState.Idle, viewModel.state.value)
        verify(repository, never()).searchOnline(any())
    }

    @Test
    fun foundBooks_areShown() = runTest {
        repository.stub { onBlocking { searchOnline("dune") } doReturn listOf(dune) }

        viewModel.search("  dune ")

        assertEquals(SearchUiState.Results(listOf(dune)), viewModel.state.value)
    }

    @Test
    fun nothingFound_showsNoResultsWithTheQuery() = runTest {
        repository.stub { onBlocking { searchOnline("qwxz") } doReturn emptyList() }

        viewModel.search("qwxz")

        assertEquals(SearchUiState.NoResults("qwxz"), viewModel.state.value)
    }

    @Test
    fun networkFailure_showsTheError() = runTest {
        repository.stub {
            onBlocking { searchOnline("dune") } doSuspendableAnswer { throw NetworkException(NetworkError.NO_INTERNET) }
        }

        viewModel.search("dune")

        assertEquals(SearchUiState.Error(NetworkError.NO_INTERNET), viewModel.state.value)
    }

    @Test
    fun whileSearching_stateIsLoading() = runTest {
        val answer = CompletableDeferred<List<OnlineBook>>()
        repository.stub { onBlocking { searchOnline("dune") } doSuspendableAnswer { answer.await() } }

        viewModel.search("dune")
        assertEquals(SearchUiState.Loading, viewModel.state.value)

        answer.complete(listOf(dune))
        assertEquals(SearchUiState.Results(listOf(dune)), viewModel.state.value)
    }

    @Test
    fun newSearch_cancelsTheRunningOne() = runTest {
        val slow = CompletableDeferred<List<OnlineBook>>()
        val emma = OnlineBook("/works/2", "Emma", "Jane Austen", null, 1815)
        repository.stub {
            onBlocking { searchOnline("dune") } doSuspendableAnswer { slow.await() }
            onBlocking { searchOnline("emma") } doReturn listOf(emma)
        }

        viewModel.search("dune")
        viewModel.search("emma")
        slow.complete(listOf(dune)) // the old answer arrives late and must be ignored

        assertEquals(SearchUiState.Results(listOf(emma)), viewModel.state.value)
    }

    @Test
    fun retry_repeatsTheLastSearch() = runTest {
        var online = false
        repository.stub {
            onBlocking { searchOnline("dune") } doSuspendableAnswer {
                if (online) listOf(dune) else throw NetworkException(NetworkError.TIMEOUT)
            }
        }
        viewModel.search("dune")
        assertEquals(SearchUiState.Error(NetworkError.TIMEOUT), viewModel.state.value)

        online = true // the connection is back
        viewModel.retry()

        assertEquals(SearchUiState.Results(listOf(dune)), viewModel.state.value)
        verify(repository, times(2)).searchOnline("dune")
    }

    @Test
    fun retry_withoutSearch_doesNothing() = runTest {
        viewModel.retry()

        assertEquals(SearchUiState.Idle, viewModel.state.value)
        verify(repository, never()).searchOnline(any())
    }
}
