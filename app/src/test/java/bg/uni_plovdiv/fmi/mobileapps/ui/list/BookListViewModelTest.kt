package bg.uni_plovdiv.fmi.mobileapps.ui.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.testutil.MainDispatcherRule
import bg.uni_plovdiv.fmi.mobileapps.testutil.observedValue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BookListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = listOf(
        Book(id = 1, title = "Dune", author = "Frank Herbert"),
        Book(id = 2, title = "Emma", author = "Jane Austen"),
    )

    private val repository: BookRepository = mock {
        on { books } doReturn flowOf(books)
    }

    @Test
    fun books_showWhatTheRepositoryHas() {
        val viewModel = BookListViewModel(repository)

        assertEquals(books, viewModel.books.observedValue())
    }

    @Test
    fun deleteBook_deletesThroughRepository() = runTest {
        val viewModel = BookListViewModel(repository)

        viewModel.deleteBook(books[0])

        verify(repository).deleteBook(books[0])
    }

    @Test
    fun restoreBook_putsTheBookBack() = runTest {
        val viewModel = BookListViewModel(repository)

        viewModel.restoreBook(books[1])

        verify(repository).restoreBook(books[1])
    }
}
