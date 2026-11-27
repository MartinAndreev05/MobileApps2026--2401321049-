package bg.uni_plovdiv.fmi.mobileapps.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bg.uni_plovdiv.fmi.mobileapps.ReadingDiaryApplication
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import kotlinx.coroutines.launch

/** State for the book list screen. Survives screen rotation. */
class BookListViewModel(private val repository: BookRepository) : ViewModel() {

    /** All books; updates by itself whenever the database changes. */
    val books: LiveData<List<Book>> = repository.books.asLiveData()

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.deleteBook(book) }
    }

    /** Undo for [deleteBook]: puts the same book back. */
    fun restoreBook(book: Book) {
        viewModelScope.launch { repository.restoreBook(book) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ReadingDiaryApplication
                BookListViewModel(app.bookRepository)
            }
        }
    }
}
