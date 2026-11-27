package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

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
                binding.statusGroup.check(book.status.buttonId())
            }
        }
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            binding.titleLayout.showError(errors.title)
            binding.authorLayout.showError(errors.author)
            binding.notesLayout.showError(errors.notes)
        }
        viewModel.saved.observe(viewLifecycleOwner) { saved -> if (saved) close() }
        viewModel.deleted.observe(viewLifecycleOwner) { deleted -> if (deleted) close() }

        if (!viewModel.isNewBook) {
            // The delete icon only exists while this screen is visible and editing a book.
            requireActivity().addMenuProvider(deleteMenu, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        binding.saveButton.setOnClickListener {
            viewModel.save(
                title = binding.titleInput.text?.toString().orEmpty(),
                author = binding.authorInput.text?.toString().orEmpty(),
                notes = binding.notesInput.text?.toString().orEmpty(),
                status = selectedStatus(),
            )
        }
    }

    private val deleteMenu = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_book_detail, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId != R.id.action_delete) return false
            confirmDelete()
            return true
        }
    }

    private fun confirmDelete() {
        val title = viewModel.book.value?.title ?: binding.titleInput.text.toString()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, title))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    /** Hides the keyboard and goes back to the list. */
    private fun close() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
        findNavController().navigateUp()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Added only after Android has put back the typed text (e.g. after a rotation);
        // otherwise restoring the text would count as typing and clear the errors.
        binding.titleInput.doAfterTextChanged { viewModel.clearTitleError() }
        binding.authorInput.doAfterTextChanged { viewModel.clearAuthorError() }
        binding.notesInput.doAfterTextChanged { viewModel.clearNotesError() }
    }

    private fun selectedStatus(): ReadingStatus = when (binding.statusGroup.checkedButtonId) {
        R.id.statusReading -> ReadingStatus.READING
        R.id.statusRead -> ReadingStatus.READ
        else -> ReadingStatus.WANT_TO_READ
    }

    private fun ReadingStatus.buttonId(): Int = when (this) {
        ReadingStatus.WANT_TO_READ -> R.id.statusWantToRead
        ReadingStatus.READING -> R.id.statusReading
        ReadingStatus.READ -> R.id.statusRead
    }

    private fun TextInputLayout.showError(error: FieldError?) {
        this.error = when (error) {
            FieldError.REQUIRED -> getString(R.string.error_required)
            FieldError.TOO_LONG -> getString(R.string.error_too_long)
            null -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
