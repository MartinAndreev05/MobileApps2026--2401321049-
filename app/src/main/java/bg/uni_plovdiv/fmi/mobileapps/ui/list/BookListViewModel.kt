package bg.uni_plovdiv.fmi.mobileapps.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State for the book list screen. Survives screen rotation. */
@HiltViewModel
class BookListViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {

    /** All books; updates by itself whenever the database changes. */
    val books: LiveData<List<Book>> = repository.books.asLiveData()

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.deleteBook(book) }
    }

    /** Undo for [deleteBook]: puts the same book back. */
    fun restoreBook(book: Book) {
        viewModelScope.launch { repository.restoreBook(book) }
    }
}
