package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.PhotoStorage
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import bg.uni_plovdiv.fmi.mobileapps.testutil.MainDispatcherRule
import bg.uni_plovdiv.fmi.mobileapps.testutil.observedValue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.io.File

class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val existing = Book(
        id = 7,
        title = "Dune",
        author = "Frank Herbert",
        status = ReadingStatus.READING,
        notes = "Spice",
        photoPath = OLD_PHOTO,
        createdAt = 100,
    )

    private val repository: BookRepository = mock {
        onBlocking { getBook(7) } doReturn existing
    }
    private val cameraUri: Uri = mock()
    private val photoStorage: PhotoStorage = mock {
        on { newPhotoFile() } doReturn File(NEW_PHOTO)
        on { uriFor(any()) } doReturn cameraUri
    }

    private fun newBookViewModel(vararg args: Pair<String, Any?>) =
        BookDetailViewModel(repository, photoStorage, SavedStateHandle(mapOf(*args)))

    /** Edit mode, with the book already loaded the way the screen does it. */
    private fun editViewModel(): BookDetailViewModel =
        newBookViewModel(BookDetailViewModel.ARG_BOOK_ID to 7L).also { it.book.observedValue() }

    // ---- Loading ----

    @Test
    fun newBook_hasNoBookToShow() {
        val viewModel = newBookViewModel()

        assertTrue(viewModel.isNewBook)
        assertNull(viewModel.book.observedValue())
        assertNull(viewModel.photoPath.value)
    }

    @Test
    fun bookFromSearch_isPrefilled() {
        val viewModel = newBookViewModel(
            BookDetailViewModel.ARG_TITLE to "Emma",
            BookDetailViewModel.ARG_AUTHOR to "Jane Austen",
            BookDetailViewModel.ARG_COVER_URL to COVER,
        )

        val book = viewModel.book.observedValue()!!

        assertTrue(viewModel.isNewBook)
        assertEquals("Emma", book.title)
        assertEquals("Jane Austen", book.author)
        assertEquals(COVER, book.coverUrl)
    }

    @Test
    fun editedBook_isLoadedWithItsPhoto() {
        val viewModel = editViewModel()

        assertFalse(viewModel.isNewBook)
        assertEquals(existing, viewModel.book.value)
        assertEquals(OLD_PHOTO, viewModel.photoPath.value)
    }

    // ---- Save ----

    @Test
    fun save_invalidForm_showsErrorsAndSavesNothing() = runTest {
        val viewModel = newBookViewModel()

        viewModel.save(title = " ", author = "", notes = "", status = ReadingStatus.READ)

        assertEquals(FieldError.REQUIRED, viewModel.errors.value?.title)
        assertEquals(FieldError.REQUIRED, viewModel.errors.value?.author)
        assertEquals(false, viewModel.saved.value)
        verify(repository, never()).addBook(any())
    }

    @Test
    fun save_newBook_addsTrimmedValuesWithCover() = runTest {
        val viewModel = newBookViewModel(
            BookDetailViewModel.ARG_TITLE to "Emma",
            BookDetailViewModel.ARG_COVER_URL to COVER,
        )

        viewModel.save(title = " Emma ", author = " Jane Austen ", notes = " Fun ", status = ReadingStatus.READ)

        val added = argumentCaptor<Book>()
        verify(repository).addBook(added.capture())
        with(added.firstValue) {
            assertEquals("Emma", title)
            assertEquals("Jane Austen", author)
            assertEquals("Fun", notes)
            assertEquals(ReadingStatus.READ, status)
            assertEquals(COVER, coverUrl)
            assertNull(photoPath)
        }
        assertEquals(true, viewModel.saved.value)
    }

    @Test
    fun save_editedBook_updatesItAndKeepsThePhoto() = runTest {
        val viewModel = editViewModel()

        viewModel.save(title = "Dune Messiah", author = "Frank Herbert", notes = "", status = ReadingStatus.READ)

        verify(repository).updateBook(
            existing.copy(title = "Dune Messiah", notes = "", status = ReadingStatus.READ)
        )
        verify(photoStorage, never()).delete(anyOrNull())
        assertEquals(true, viewModel.saved.value)
    }

    @Test
    fun save_afterRemovingPhoto_deletesTheOldFile() = runTest {
        val viewModel = editViewModel()

        viewModel.removePhoto()
        assertNull(viewModel.photoPath.value)
        verify(photoStorage, never()).delete(OLD_PHOTO) // not before the user saves

        viewModel.save(title = "Dune", author = "Frank Herbert", notes = "", status = ReadingStatus.READING)

        verify(repository).updateBook(existing.copy(notes = "", photoPath = null))
        verify(photoStorage).delete(OLD_PHOTO)
    }

    @Test
    fun typingClearsOnlyThatFieldsError() {
        val viewModel = newBookViewModel()
        viewModel.save(title = "", author = "", notes = "", status = ReadingStatus.READ)

        viewModel.clearTitleError()

        assertNull(viewModel.errors.value?.title)
        assertEquals(FieldError.REQUIRED, viewModel.errors.value?.author)

        viewModel.clearAuthorError()
        viewModel.clearNotesError()
        assertTrue(viewModel.errors.value!!.isValid)
    }

    // ---- Delete ----

    @Test
    fun delete_removesBookAndItsPhoto() = runTest {
        val viewModel = editViewModel()

        viewModel.delete()

        verify(repository).deleteBook(existing)
        verify(photoStorage).delete(OLD_PHOTO)
        assertEquals(true, viewModel.deleted.value)
    }

    @Test
    fun delete_forNewBook_doesNothing() = runTest {
        val viewModel = newBookViewModel()

        viewModel.delete()

        verify(repository, never()).deleteBook(any())
        assertEquals(false, viewModel.deleted.value)
    }

    // ---- Camera photo ----

    @Test
    fun takenPhoto_isShown() {
        val viewModel = newBookViewModel()

        assertSame(cameraUri, viewModel.preparePhotoUri())
        viewModel.onPhotoTaken(success = true)

        assertEquals(NEW_PHOTO, viewModel.photoPath.value)
    }

    @Test
    fun cancelledPhoto_deletesTheEmptyFileAndKeepsTheOldPhoto() {
        val viewModel = editViewModel()

        viewModel.preparePhotoUri()
        viewModel.onPhotoTaken(success = false)

        verify(photoStorage).delete(NEW_PHOTO)
        assertEquals(OLD_PHOTO, viewModel.photoPath.value)
    }

    @Test
    fun cameraResultWithoutRequest_isIgnored() {
        val viewModel = newBookViewModel()

        viewModel.onPhotoTaken(success = true)

        assertNull(viewModel.photoPath.value)
    }

    @Test
    fun secondUnsavedPhoto_replacesAndDeletesTheFirst() {
        val viewModel = newBookViewModel()
        viewModel.preparePhotoUri()
        viewModel.onPhotoTaken(success = true)

        photoStorage.stub { on { newPhotoFile() } doReturn File("second.jpg") }
        viewModel.preparePhotoUri()
        viewModel.onPhotoTaken(success = true)

        verify(photoStorage).delete(NEW_PHOTO)
        assertEquals("second.jpg", viewModel.photoPath.value)
    }

    @Test
    fun leavingWithoutSaving_deletesTheUnsavedPhoto() {
        val store = ViewModelStore()
        val viewModel = createInStore(store)
        viewModel.preparePhotoUri()
        viewModel.onPhotoTaken(success = true)

        store.clear() // the screen is closed for good: onCleared() runs

        verify(photoStorage).delete(NEW_PHOTO)
    }

    @Test
    fun leavingAfterSaving_keepsThePhoto() = runTest {
        val store = ViewModelStore()
        val viewModel = createInStore(store)
        viewModel.preparePhotoUri()
        viewModel.onPhotoTaken(success = true)
        viewModel.save(title = "Emma", author = "Jane Austen", notes = "", status = ReadingStatus.READ)

        store.clear()

        verify(photoStorage, never()).delete(NEW_PHOTO)
    }

    private fun createInStore(store: ViewModelStore): BookDetailViewModel {
        val factory = viewModelFactory {
            initializer { BookDetailViewModel(repository, photoStorage, SavedStateHandle()) }
        }
        return ViewModelProvider(store, factory)[BookDetailViewModel::class.java]
    }

    private companion object {
        const val OLD_PHOTO = "old.jpg"
        const val NEW_PHOTO = "new.jpg"
        const val COVER = "https://covers.openlibrary.org/b/id/1-M.jpg"
    }
}
