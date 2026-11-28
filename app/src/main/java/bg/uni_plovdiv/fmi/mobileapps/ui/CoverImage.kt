package bg.uni_plovdiv.fmi.mobileapps.ui

import android.widget.ImageView
import bg.uni_plovdiv.fmi.mobileapps.R
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import java.io.File

/** Shows a book cover from [url], or the placeholder book while loading, on error or without a URL. */
fun ImageView.loadCover(url: String?) = loadBookImage(photoPath = null, coverUrl = url)

/**
 * Shows the user's own photo if there is one, otherwise the Open Library cover,
 * otherwise the placeholder book.
 */
fun ImageView.loadBookImage(photoPath: String?, coverUrl: String?) {
    val source: Any? = photoPath?.let(::File) ?: coverUrl
    if (source == null) {
        setImageResource(R.drawable.ic_cover_placeholder)
        return
    }
    load(source) {
        placeholder(R.drawable.ic_cover_placeholder)
        error(R.drawable.ic_cover_placeholder)
    }
}
