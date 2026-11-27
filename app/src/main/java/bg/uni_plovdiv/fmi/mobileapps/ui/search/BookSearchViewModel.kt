package bg.uni_plovdiv.fmi.mobileapps.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkError
import bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkException
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the search screen can show. */
sealed interface SearchUiState {
    /** Nothing searched yet. */
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val books: List<OnlineBook>) : SearchUiState
    data class NoResults(val query: String) : SearchUiState
    data class Error(val error: NetworkError) : SearchUiState
}

/** State for the Open Library search screen. Survives rotation, so results aren't fetched twice. */
@HiltViewModel
class BookSearchViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {

    private val _state = MutableLiveData<SearchUiState>(SearchUiState.Idle)
    val state: LiveData<SearchUiState> = _state

    private var lastQuery: String? = null
    private var searchJob: Job? = null

    /** Starts a search; a search that is still running is cancelled first. */
    fun search(query: String) {
        val clean = query.trim()
        if (clean.length < MIN_QUERY_LENGTH) return
        lastQuery = clean
        searchJob?.cancel()
        _state.value = SearchUiState.Loading
        searchJob = viewModelScope.launch {
            _state.value = try {
                val books = repository.searchOnline(clean)
                if (books.isEmpty()) SearchUiState.NoResults(clean) else SearchUiState.Results(books)
            } catch (e: NetworkException) {
                SearchUiState.Error(e.error)
            }
        }
    }

    /** Repeats the last search, e.g. after the connection is back. */
    fun retry() {
        lastQuery?.let(::search)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
