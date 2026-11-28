package bg.uni_plovdiv.fmi.mobileapps.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import bg.uni_plovdiv.fmi.mobileapps.data.local.Book
import bg.uni_plovdiv.fmi.mobileapps.databinding.ItemBookBinding
import bg.uni_plovdiv.fmi.mobileapps.ui.labelRes
import bg.uni_plovdiv.fmi.mobileapps.ui.loadBookImage

/**
 * Shows books as cards. ListAdapter compares the old and new list with [BookDiff]
 * on a background thread and only redraws the cards that actually changed.
 *
 * @param onBookClick called with the tapped book.
 */
class BookAdapter(
    private val onBookClick: (Book) -> Unit,
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding, onBookClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** The book shown at [position]; used by swipe-to-delete. */
    fun bookAt(position: Int): Book = getItem(position)

    class BookViewHolder(
        private val binding: ItemBookBinding,
        onBookClick: (Book) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var book: Book? = null

        init {
            binding.root.setOnClickListener { book?.let(onBookClick) }
        }

        fun bind(book: Book) {
            this.book = book
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.bookStatus.setText(book.status.labelRes())
            binding.bookCover.loadBookImage(book.photoPath, book.coverUrl)
        }
    }

    private object BookDiff : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
    }
}
