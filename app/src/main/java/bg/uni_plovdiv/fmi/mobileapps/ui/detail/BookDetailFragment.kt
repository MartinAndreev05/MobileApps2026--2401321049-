package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookDetailBinding

/** Add / edit screen for a book. All state lives in [BookDetailViewModel]. */
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookDetailViewModel by viewModels { BookDetailViewModel.Factory }

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
        if (!viewModel.isNewBook) {
            (requireActivity() as AppCompatActivity).supportActionBar
                ?.setTitle(R.string.title_edit_book)
        }

        // viewLifecycleOwner (not the fragment itself) stops the observers when the view
        // is destroyed, so they never touch a dead view and the view is not leaked.
        viewModel.book.observe(viewLifecycleOwner) { book ->
            // After a rotation the fields restore what the user typed; don't overwrite it.
            if (book != null && savedInstanceState == null) {
                binding.titleInput.setText(book.title)
                binding.authorInput.setText(book.author)
                binding.notesInput.setText(book.notes)
            }
        }
        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            // Validation and the call to viewModel.save() are added with the create flow.
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
