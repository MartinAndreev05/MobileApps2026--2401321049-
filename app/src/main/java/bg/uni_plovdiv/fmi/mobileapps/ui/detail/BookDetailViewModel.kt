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

    private val _saved = MutableLiveData(false)

    /** Becomes true once the book is stored, so the screen can close. */
    val saved: LiveData<Boolean> = _saved

    /** Adds a new book or updates the edited one with the values from the form. */
    fun save(title: String, author: String, notes: String) {
        viewModelScope.launch {
            val existing = if (isNewBook) null else repository.getBook(bookId)
            if (existing == null) {
                repository.addBook(Book(title = title, author = author, notes = notes))
            } else {
                repository.updateBook(existing.copy(title = title, author = author, notes = notes))
            }
            _saved.value = true
        }
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
