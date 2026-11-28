package bg.uni_plovdiv.fmi.mobileapps.data

import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.BookDao
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import bg.uni_plovdiv.fmi.mobileapps.data.remote.BookRemoteDataSource
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class BookRepositoryTest {

    private val book = Book(
        id = 7,
        title = "Dune",
        author = "Frank Herbert",
        status = ReadingStatus.READING,
        createdAt = 100,
        updatedAt = 200,
    )

    private val dao: BookDao = mock {
        on { getAll() } doReturn flowOf(listOf(book))
    }
    private val remote: BookRemoteDataSource = mock()
    private val repository = BookRepository(dao, remote, now = { NOW })

    @Test
    fun books_comeFromTheDao() = runTest {
        assertEquals(listOf(book), repository.books.first())
    }

    @Test
    fun getBook_returnsTheBookOrNull() = runTest {
        dao.stub {
            onBlocking { getById(7) } doReturn book
            onBlocking { getById(8) } doReturn null
        }

        assertEquals(book, repository.getBook(7))
        assertNull(repository.getBook(8))
    }

    @Test
    fun addBook_setsBothDatesToNowAndReturnsNewId() = runTest {
        dao.stub { onBlocking { insert(any()) } doReturn 42L }

        val id = repository.addBook(book.copy(id = 0))

        assertEquals(42L, id)
        val saved = argumentCaptor<Book>()
        verify(dao).insert(saved.capture())
        assertEquals(NOW, saved.firstValue.createdAt)
        assertEquals(NOW, saved.firstValue.updatedAt)
        assertEquals("Dune", saved.firstValue.title)
    }

    @Test
    fun updateBook_changesOnlyUpdatedAt() = runTest {
        repository.updateBook(book.copy(notes = "Great"))

        verify(dao).update(book.copy(notes = "Great", updatedAt = NOW))
    }

    @Test
    fun deleteBook_deletesFromDao() = runTest {
        repository.deleteBook(book)

        verify(dao).delete(book)
    }

    @Test
    fun restoreBook_insertsTheSameBookUnchanged() = runTest {
        repository.restoreBook(book)

        verify(dao).insert(book)
    }

    @Test
    fun searchOnline_trimsTheQuery() = runTest {
        val found = listOf(OnlineBook("/works/1", "Dune", "Frank Herbert", null, 1965))
        remote.stub { onBlocking { search("dune") } doReturn found }

        assertEquals(found, repository.searchOnline("  dune  "))
    }

    private companion object {
        const val NOW = 5_000L
    }
}
