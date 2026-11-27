package bg.uni_plovdiv.fmi.mobileapps.data

import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.BookDao
import kotlinx.coroutines.flow.Flow

/**
 * The only place the rest of the app gets book data from.
 *
 * ViewModels never talk to Room directly, so the data source can change
 * (for example, Open Library is added later) without touching the screens.
 * All database work is done in suspend functions or a Flow, never on the UI thread.
 *
 * @param now returns the current time in milliseconds; replaceable in tests.
 */
class BookRepository(
    private val bookDao: BookDao,
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
}
