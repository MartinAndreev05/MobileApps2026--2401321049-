package bg.uni_plovdiv.fmi.mobileapps.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks every BookDao operation against a real Room database kept in memory. */
@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.bookDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val id = dao.insert(Book(title = "Под игото", author = "Иван Вазов"))

        val saved = dao.getById(id)

        assertEquals("Под игото", saved?.title)
        assertEquals(ReadingStatus.WANT_TO_READ, saved?.status)
    }

    @Test
    fun updateChangesTheRow() = runBlocking {
        val id = dao.insert(Book(title = "Тютюн", author = "Димитър Димов"))
        val book = dao.getById(id)!!

        dao.update(book.copy(status = ReadingStatus.READ, rating = 5))

        val updated = dao.getById(id)!!
        assertEquals(ReadingStatus.READ, updated.status)
        assertEquals(5, updated.rating)
    }

    @Test
    fun deleteRemovesTheRow() = runBlocking {
        val id = dao.insert(Book(title = "Бай Ганьо", author = "Алеко Константинов"))

        dao.delete(dao.getById(id)!!)

        assertNull(dao.getById(id))
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAllReturnsMostRecentlyChangedFirst() = runBlocking {
        dao.insert(Book(title = "Стара", author = "А", createdAt = 1_000))
        dao.insert(Book(title = "Нова", author = "Б", createdAt = 2_000))

        val titles = dao.getAll().first().map { it.title }

        assertEquals(listOf("Нова", "Стара"), titles)
    }
}
