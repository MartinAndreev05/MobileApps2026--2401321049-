package bg.uni_plovdiv.fmi.mobileapps.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Database operations for books.
 *
 * Writes are suspend functions, so Room runs them off the main thread.
 * [getAll] returns a Flow that emits a new list every time the table changes;
 * the ViewModels turn it into LiveData for the screens.
 */
@Dao
interface BookDao {

    /** Inserts a new book and returns its generated id. */
    @Insert
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    /** All books, most recently changed first. */
    @Query("SELECT * FROM books ORDER BY updated_at DESC")
    fun getAll(): Flow<List<Book>>

    /** The book with this id, or null if there is none. */
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?
}
