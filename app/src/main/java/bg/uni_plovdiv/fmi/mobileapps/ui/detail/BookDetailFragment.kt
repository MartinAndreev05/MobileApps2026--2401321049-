package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookDetailBinding

/**
 * Add / edit screen for a book. Opened with bookId = -1 for a new book,
 * or with the id of an existing book to edit it.
 */
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val bookId: Long
        get() = requireArguments().getLong(ARG_BOOK_ID, NEW_BOOK_ID)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (bookId != NEW_BOOK_ID) {
            (requireActivity() as AppCompatActivity).supportActionBar
                ?.setTitle(R.string.title_edit_book)
        }
        binding.saveButton.setOnClickListener {
            // Validation and saving to the database are added together with Room.
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        const val NEW_BOOK_ID = -1L
    }
}
