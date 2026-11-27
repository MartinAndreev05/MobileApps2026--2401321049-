package bg.uni_plovdiv.fmi.mobileapps.data

import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.BookDao
import bg.uni_plovdiv.fmi.mobileapps.data.remote.BookRemoteDataSource
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import kotlinx.coroutines.flow.Flow

/**
 * The only place the rest of the app gets book data from.
 *
 * Two sources sit behind it: the local Room database (the user's own books,
 * always the source of truth) and Open Library (search only; a result is
 * copied into the database when the user saves it). ViewModels never talk to
 * Room or Retrofit directly. All work is done in suspend functions or a Flow,
 * never on the UI thread.
 *
 * @param now returns the current time in milliseconds; replaceable in tests.
 */
class BookRepository(
    private val bookDao: BookDao,
    private val remote: BookRemoteDataSource,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** All books, most recently changed first. Emits again after every change. */
    val books: Flow<List<Book>> = bookDao.getAll()

    suspend fun getBook(id: Long): Book? = bookDao.getById(id)

    /** Saves a new book and returns its id. */
    suspend fun addBook(book: Book): Long {
        val time = now()
        return bookDao.insert(book.copy(createdAt = time, updatedAt = time))
    }

    /** Saves changes to an existing book and marks it as just changed. */
    suspend fun updateBook(book: Book) {
        bookDao.update(book.copy(updatedAt = now()))
    }

    suspend fun deleteBook(book: Book) {
        bookDao.delete(book)
    }

    /** Puts a just-deleted book back unchanged (same id and dates), for "Undo". */
    suspend fun restoreBook(book: Book) {
        bookDao.insert(book)
    }

    /**
     * Searches Open Library.
     * @throws bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkException if the request fails.
     */
    suspend fun searchOnline(query: String): List<OnlineBook> = remote.search(query.trim())
}
