package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.PhotoStorage
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the add / edit book screen.
 *
 * The navigation arguments arrive through [SavedStateHandle]:
 * bookId [NEW_BOOK_ID] means "add a new book", any other value is the book to edit;
 * title / author / coverUrl pre-fill a new book picked in the Open Library search.
 */
@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: BookRepository,
    private val photoStorage: PhotoStorage,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: Long = savedStateHandle[ARG_BOOK_ID] ?: NEW_BOOK_ID

    /** Values picked in the Open Library search; only used for a new book. */
    private val prefillTitle: String? = savedStateHandle[ARG_TITLE]
    private val prefillAuthor: String? = savedStateHandle[ARG_AUTHOR]
    private val prefillCoverUrl: String? = savedStateHandle[ARG_COVER_URL]

    val isNewBook: Boolean get() = bookId == NEW_BOOK_ID

    /** Photo of the book as stored in the database (null for a new book). */
    private var savedPhotoPath: String? = null

    /**
     * The book shown in the form: the edited book loaded from the database, or, for a
     * new book picked in the search, an unsaved book with the found values.
     * Emits nothing for an empty new book.
     */
    val book: LiveData<Book?> = liveData {
        when {
            !isNewBook -> {
                val loaded = repository.getBook(bookId)
                savedPhotoPath = loaded?.photoPath
                // Only on the first load; after process death the handle already has the user's choice.
                if (!savedStateHandle.contains(KEY_PHOTO_PATH)) {
                    savedStateHandle[KEY_PHOTO_PATH] = loaded?.photoPath
                }
                emit(loaded)
            }
            prefillTitle != null -> emit(
                Book(title = prefillTitle, author = prefillAuthor.orEmpty(), coverUrl = prefillCoverUrl)
            )
        }
    }

    /**
     * The photo shown in the form: the saved one, a newly taken one, or null.
     * Kept in [SavedStateHandle] so it survives rotation and the app being killed
     * while the camera is open.
     */
    val photoPath: LiveData<String?> = savedStateHandle.getLiveData(KEY_PHOTO_PATH)

    private val _errors = MutableLiveData(BookFormErrors())

    /** Validation errors of the last save attempt; kept across rotation. */
    val errors: LiveData<BookFormErrors> = _errors

    private val _saved = MutableLiveData(false)

    /** Becomes true once the book is stored, so the screen can close. */
    val saved: LiveData<Boolean> = _saved

    private val _deleted = MutableLiveData(false)

    /** Becomes true once the edited book is deleted, so the screen can close. */
    val deleted: LiveData<Boolean> = _deleted

    /** True while a save or delete is running; extra taps are ignored. */
    private var saving = false

    /** True once the photo choice is written to the database (or the book is deleted). */
    private var finished = false

    /**
     * Validates the form values and, if they are valid, adds a new book or
     * updates the edited one. Invalid values only update [errors].
     */
    fun save(title: String, author: String, notes: String, status: ReadingStatus) {
        if (saving) return // ignore double taps while the first save is running
        val cleanTitle = title.trim()
        val cleanAuthor = author.trim()
        val cleanNotes = notes.trim()

        val result = BookFormValidator.validate(cleanTitle, cleanAuthor, cleanNotes)
        _errors.value = result
        if (!result.isValid) return

        saving = true
        val photo = photoPath.value
        viewModelScope.launch {
            val existing = if (isNewBook) null else repository.getBook(bookId)
            if (existing == null) {
                repository.addBook(
                    Book(
                        title = cleanTitle,
                        author = cleanAuthor,
                        notes = cleanNotes,
                        status = status,
                        coverUrl = prefillCoverUrl,
                        photoPath = photo,
                    )
                )
            } else {
                repository.updateBook(
                    existing.copy(
                        title = cleanTitle,
                        author = cleanAuthor,
                        notes = cleanNotes,
                        status = status,
                        photoPath = photo,
                    )
                )
            }
            // The old photo was replaced or removed: its file is no longer needed.
            if (savedPhotoPath != photo) photoStorage.delete(savedPhotoPath)
            finished = true
            _saved.value = true
        }
    }

    /** Deletes the edited book and its photo. Does nothing in "new book" mode. */
    fun delete() {
        if (isNewBook || saving) return
        saving = true
        viewModelScope.launch {
            repository.getBook(bookId)?.let { repository.deleteBook(it) }
            photoStorage.delete(savedPhotoPath)
            if (photoPath.value != savedPhotoPath) photoStorage.delete(photoPath.value)
            finished = true
            _deleted.value = true
        }
    }

    /**
     * Creates an empty file for the camera and returns a content:// URI for it.
     * The path is remembered in [SavedStateHandle] until [onPhotoTaken] is called.
     */
    fun preparePhotoUri(): Uri {
        val file = photoStorage.newPhotoFile()
        savedStateHandle[KEY_PHOTO_IN_PROGRESS] = file.path
        return photoStorage.uriFor(file.path)
    }

    /** Result from the camera: keep the new photo, or throw the empty file away. */
    fun onPhotoTaken(success: Boolean) {
        val path = savedStateHandle.remove<String>(KEY_PHOTO_IN_PROGRESS) ?: return
        if (success) replacePhoto(path) else photoStorage.delete(path)
    }

    /** Removes the photo from the form; the book keeps it until the user saves. */
    fun removePhoto() = replacePhoto(null)

    private fun replacePhoto(newPath: String?) {
        val current = photoPath.value
        // A photo taken in this session and not saved yet can go right away.
        if (current != null && current != savedPhotoPath) photoStorage.delete(current)
        savedStateHandle[KEY_PHOTO_PATH] = newPath
    }

    /** Called while the user types, so a fixed field stops showing its error. */
    fun clearTitleError() = updateErrors { it.copy(title = null) }
    fun clearAuthorError() = updateErrors { it.copy(author = null) }
    fun clearNotesError() = updateErrors { it.copy(notes = null) }

    private fun updateErrors(change: (BookFormErrors) -> BookFormErrors) {
        val current = _errors.value ?: return
        val updated = change(current)
        if (updated != current) _errors.value = updated
    }

    /** The user left without saving: delete a photo taken here that nothing points to. */
    override fun onCleared() {
        if (!finished) {
            val unsaved = photoPath.value
            if (unsaved != savedPhotoPath) photoStorage.delete(unsaved)
        }
    }

    companion object {
        /** Names of the navigation arguments, as declared in nav_graph.xml. */
        const val ARG_BOOK_ID = "bookId"
        const val ARG_TITLE = "title"
        const val ARG_AUTHOR = "author"
        const val ARG_COVER_URL = "coverUrl"
        const val NEW_BOOK_ID = -1L

        private const val KEY_PHOTO_PATH = "photoPath"
        private const val KEY_PHOTO_IN_PROGRESS = "photoInProgress"
    }
}
