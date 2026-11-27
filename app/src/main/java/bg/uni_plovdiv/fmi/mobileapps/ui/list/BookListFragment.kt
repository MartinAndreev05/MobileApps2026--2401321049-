package bg.uni_plovdiv.fmi.mobileapps.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.databinding.FragmentBookListBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/** Start screen: the list of the user's books. All state lives in [BookListViewModel]. */
@AndroidEntryPoint
class BookListFragment : Fragment() {

    // The binding is only valid between onCreateView and onDestroyView;
    // clearing it afterwards prevents leaking the view hierarchy.
    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookListViewModel by viewModels()

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

        val adapter = BookAdapter(onBookClick = ::openBook)
        binding.bookList.adapter = adapter
        swipeToDelete(adapter).attachToRecyclerView(binding.bookList)

        // viewLifecycleOwner stops the observer when the view is destroyed (no leaks).
        viewModel.books.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
            binding.emptyState.isVisible = books.isEmpty()
            binding.bookList.isVisible = books.isNotEmpty()
        }

        // Search icon in the toolbar, only while this screen is visible.
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_book_list, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId != R.id.action_search_online) return false
                    findNavController().navigate(BookListFragmentDirections.actionBookListToBookSearch())
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        binding.addBookFab.setOnClickListener {
            // Default bookId (-1): the detail screen opens in "new book" mode.
            findNavController().navigate(BookListFragmentDirections.actionBookListToBookDetail())
        }
    }

    private fun openBook(book: Book) {
        // Safe Args: the bookId argument is type-checked at compile time.
        findNavController().navigate(
            BookListFragmentDirections.actionBookListToBookDetail(bookId = book.id)
        )
    }

    /** Swiping a card left or right deletes the book; the Snackbar offers Undo. */
    private fun swipeToDelete(adapter: BookAdapter) = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val book = adapter.bookAt(position)
                viewModel.deleteBook(book)
                Snackbar.make(binding.root, getString(R.string.book_deleted, book.title), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.addBookFab)
                    .setAction(R.string.action_undo) { viewModel.restoreBook(book) }
                    .show()
            }
        }
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
