package bg.uni_plovdiv.fmi.mobileapps.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.remote.NetworkError
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookSearchBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Searches Open Library. Picking a result opens the "new book" form
 * with the title, author and cover already filled in.
 */
@AndroidEntryPoint
class BookSearchFragment : Fragment() {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookSearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OnlineBookAdapter(onBookClick = ::useResult)
        binding.resultList.adapter = adapter

        // Search key on the on-screen keyboard, or Enter on a hardware keyboard.
        binding.queryInput.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enterPressed) {
                if (event == null || event.action == KeyEvent.ACTION_DOWN) startSearch()
                true
            } else {
                false
            }
        }
        binding.queryInput.doAfterTextChanged { binding.queryLayout.error = null }
        binding.retryButton.setOnClickListener { viewModel.retry() }

        // viewLifecycleOwner stops the observer when the view is destroyed (no leaks).
        viewModel.state.observe(viewLifecycleOwner) { state -> render(state, adapter) }

        if (savedInstanceState == null) {
            binding.queryInput.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, binding.queryInput)
                .show(WindowInsetsCompat.Type.ime())
        }
    }

    private fun startSearch() {
        val query = binding.queryInput.text?.toString().orEmpty()
        if (query.trim().length < BookSearchViewModel.MIN_QUERY_LENGTH) {
            binding.queryLayout.error = getString(R.string.error_query_too_short)
            return
        }
        binding.queryLayout.error = null
        hideKeyboard()
        viewModel.search(query)
    }

    private fun render(state: SearchUiState, adapter: OnlineBookAdapter) {
        binding.progress.isVisible = state is SearchUiState.Loading
        binding.retryButton.isVisible = state is SearchUiState.Error
        adapter.submitList((state as? SearchUiState.Results)?.books.orEmpty())
        binding.resultList.isVisible = state is SearchUiState.Results

        val message: String? = when (state) {
            SearchUiState.Idle -> getString(R.string.search_hint)
            SearchUiState.Loading, is SearchUiState.Results -> null
            is SearchUiState.NoResults -> getString(R.string.search_no_results, state.query)
            is SearchUiState.Error -> getString(state.error.messageRes())
        }
        binding.messageBox.isVisible = message != null
        binding.messageText.text = message
    }

    private fun useResult(book: OnlineBook) {
        hideKeyboard()
        findNavController().navigate(
            BookSearchFragmentDirections.actionBookSearchToBookDetail(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
            )
        )
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
    }

    @StringRes
    private fun NetworkError.messageRes(): Int = when (this) {
        NetworkError.NO_INTERNET -> R.string.error_no_internet
        NetworkError.TIMEOUT -> R.string.error_timeout
        NetworkError.SERVER -> R.string.error_server
        NetworkError.UNKNOWN -> R.string.error_unknown
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
