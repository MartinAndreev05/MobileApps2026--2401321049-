package bg.uni_plovdiv.fmi.mobileapps.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.remote.OnlineBook
import bg.uni_plovdiv.fmi.mobileapps.databinding.ItemOnlineBookBinding
import bg.uni_plovdiv.fmi.mobileapps.ui.loadCover

/** Open Library search results. Tapping a row calls [onBookClick]. */
class OnlineBookAdapter(
    private val onBookClick: (OnlineBook) -> Unit,
) : ListAdapter<OnlineBook, OnlineBookAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemOnlineBookBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onBookClick,
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemOnlineBookBinding,
        onBookClick: (OnlineBook) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var book: OnlineBook? = null

        init {
            binding.root.setOnClickListener { book?.let(onBookClick) }
        }

        fun bind(book: OnlineBook) {
            this.book = book
            binding.title.text = book.title
            binding.author.text = book.author.ifEmpty {
                binding.root.context.getString(R.string.unknown_author)
            }
            binding.year.isVisible = book.firstPublishYear != null
            binding.year.text = book.firstPublishYear?.toString()
            binding.cover.loadCover(book.coverUrl)
        }
    }

    private object Diff : DiffUtil.ItemCallback<OnlineBook>() {
        override fun areItemsTheSame(oldItem: OnlineBook, newItem: OnlineBook) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: OnlineBook, newItem: OnlineBook) = oldItem == newItem
    }
}
