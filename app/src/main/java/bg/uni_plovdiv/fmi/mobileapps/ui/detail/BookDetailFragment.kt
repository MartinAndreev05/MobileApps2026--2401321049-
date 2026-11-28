package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.local.PhotoStorage
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookDetailBinding
import bg.uni_plovdiv.fmi.mobileapps.ui.labelRes
import bg.uni_plovdiv.fmi.mobileapps.ui.loadBookImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Add / edit screen for a book. All state lives in [BookDetailViewModel]. */
@AndroidEntryPoint
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookDetailViewModel by viewModels()

    @Inject
    lateinit var photoStorage: PhotoStorage

    /** Cover from Open Library; shown when the book has no photo of its own. */
    private var coverUrl: String? = null

    // Activity Result API: registered before the fragment is created, so the result
    // is delivered even if Android recreated the screen while the camera was open.
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            viewModel.onPhotoTaken(success)
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else onCameraPermissionDenied()
        }

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

        binding.searchOnlineButton.isVisible = viewModel.isNewBook
        binding.searchOnlineButton.setOnClickListener {
            findNavController().navigate(BookDetailFragmentDirections.actionBookDetailToBookSearch())
        }
        binding.takePhotoButton.setOnClickListener { onTakePhotoClicked() }
        binding.removePhotoButton.setOnClickListener { viewModel.removePhoto() }

        // viewLifecycleOwner (not the fragment itself) stops the observers when the view
        // is destroyed, so they never touch a dead view and the view is not leaked.
        viewModel.book.observe(viewLifecycleOwner) { book ->
            coverUrl = book?.coverUrl
            showImage()
            // A book from the search doesn't need the search button any more.
            if (book != null) binding.searchOnlineButton.isVisible = false

            // After a rotation the fields restore what the user typed; don't overwrite it.
            if (book != null && savedInstanceState == null) {
                binding.titleInput.setText(book.title)
                binding.authorInput.setText(book.author)
                binding.notesInput.setText(book.notes)
                binding.statusGroup.check(book.status.buttonId())
            }
        }
        viewModel.photoPath.observe(viewLifecycleOwner) { path ->
            binding.removePhotoButton.isVisible = path != null
            showImage()
        }
        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            binding.titleLayout.showError(errors.title)
            binding.authorLayout.showError(errors.author)
            binding.notesLayout.showError(errors.notes)
        }
        viewModel.saved.observe(viewLifecycleOwner) { saved -> if (saved) close() }
        viewModel.deleted.observe(viewLifecycleOwner) { deleted -> if (deleted) close() }

        if (!viewModel.isNewBook) {
            // Share and delete icons only while this screen is visible and editing a book.
            requireActivity().addMenuProvider(editMenu, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

    private fun showImage() {
        binding.coverImage.loadBookImage(viewModel.photoPath.value, coverUrl)
    }

    // ---- Camera and its runtime permission ----

    /**
     * CAMERA is a "dangerous" permission, so it is asked at runtime, at the moment it is
     * needed. If the user already said no once, Android lets us explain why first.
     */
    private fun onTakePhotoClicked() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED -> launchCamera()

            shouldShowRequestPermissionRationale(permission) ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.camera_rationale_title)
                    .setMessage(R.string.camera_rationale_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_continue) { _, _ ->
                        requestCameraPermission.launch(permission)
                    }
                    .show()

            else -> requestCameraPermission.launch(permission)
        }
    }

    private fun launchCamera() {
        val uri = viewModel.preparePhotoUri()
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.onPhotoTaken(success = false)
            Snackbar.make(binding.root, R.string.no_camera_app, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onCameraPermissionDenied() {
        // No rationale after a refusal means "don't ask again": only the settings can help now.
        val blocked = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        if (blocked) {
            Snackbar.make(binding.root, R.string.camera_permission_settings, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_settings) { openAppSettings() }
                .show()
        } else {
            Snackbar.make(binding.root, R.string.camera_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", requireContext().packageName, null))
        )
    }

    // ---- Share ----

    /** Opens the system share sheet with the book as text, plus the photo if there is one. */
    private fun shareBook() {
        val title = binding.titleInput.text?.toString().orEmpty().trim()
        val author = binding.authorInput.text?.toString().orEmpty().trim()
        val notes = binding.notesInput.text?.toString().orEmpty().trim()
        var text = getString(R.string.share_text, title, author, getString(selectedStatus().labelRes()))
        if (notes.isNotEmpty()) text += getString(R.string.share_text_notes, notes)

        val send = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, text)

        val photo = viewModel.photoPath.value
        if (photo != null) {
            // The receiving app gets read access to this one file only, through FileProvider.
            val uri = photoStorage.uriFor(photo)
            send.setType("image/jpeg")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = ClipData.newRawUri(null, uri)
        } else {
            send.setType("text/plain")
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    }

    // ---- Toolbar menu and delete ----

    private val editMenu = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_book_detail, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.action_share -> {
                shareBook()
                true
            }
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> false
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
