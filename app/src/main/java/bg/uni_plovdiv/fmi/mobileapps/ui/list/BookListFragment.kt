package bg.uni_plovdiv.fmi.mobileapps.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookListBinding

/** Start screen: the list of the user's books. All state lives in [BookListViewModel]. */
class BookListFragment : Fragment() {

    // The binding is only valid between onCreateView and onDestroyView;
    // clearing it afterwards prevents leaking the view hierarchy.
    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookListViewModel by viewModels { BookListViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // viewLifecycleOwner stops the observer when the view is destroyed (no leaks).
        viewModel.books.observe(viewLifecycleOwner) { books ->
            binding.emptyState.isVisible = books.isEmpty()
        }

        binding.addBookFab.setOnClickListener {
            // No bookId argument: the detail screen opens in "new book" mode.
            findNavController().navigate(R.id.action_bookList_to_bookDetail)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
