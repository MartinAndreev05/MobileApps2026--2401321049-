package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bg.uni_plovdiv.fmi.mobileapps.ReadingDiaryApplication
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import kotlinx.coroutines.launch

/**
 * State for the add / edit book screen.
 *
 * The bookId navigation argument arrives through [SavedStateHandle]:
 * [NEW_BOOK_ID] means "add a new book", any other value is the book to edit.
 */
class BookDetailViewModel(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bookId: Long = savedStateHandle[ARG_BOOK_ID] ?: NEW_BOOK_ID

    val isNewBook: Boolean get() = bookId == NEW_BOOK_ID

    /** The book being edited, loaded once from the database. Emits nothing for a new book. */
    val book: LiveData<Book?> = liveData {
        if (!isNewBook) emit(repository.getBook(bookId))
    }

    private val _errors = MutableLiveData(BookFormErrors())

    /** Validation errors of the last save attempt; kept across rotation. */
    val errors: LiveData<BookFormErrors> = _errors

    private val _saved = MutableLiveData(false)

    /** Becomes true once the book is stored, so the screen can close. */
    val saved: LiveData<Boolean> = _saved

    /** True while a save or delete is running; extra taps are ignored. */
    private var saving = false

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
        viewModelScope.launch {
            val existing = if (isNewBook) null else repository.getBook(bookId)
            if (existing == null) {
                repository.addBook(
                    Book(title = cleanTitle, author = cleanAuthor, notes = cleanNotes, status = status)
                )
            } else {
                repository.updateBook(
                    existing.copy(
                        title = cleanTitle,
                        author = cleanAuthor,
                        notes = cleanNotes,
                        status = status,
                    )
                )
            }
            _saved.value = true
        }
    }

    private val _deleted = MutableLiveData(false)

    /** Becomes true once the edited book is deleted, so the screen can close. */
    val deleted: LiveData<Boolean> = _deleted

    /** Deletes the edited book. Does nothing in "new book" mode. */
    fun delete() {
        if (isNewBook || saving) return
        saving = true
        viewModelScope.launch {
            repository.getBook(bookId)?.let { repository.deleteBook(it) }
            _deleted.value = true
        }
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

    companion object {
        /** Name of the navigation argument, as declared in nav_graph.xml. */
        const val ARG_BOOK_ID = "bookId"
        const val NEW_BOOK_ID = -1L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ReadingDiaryApplication
                BookDetailViewModel(app.bookRepository, createSavedStateHandle())
            }
        }
    }
}
