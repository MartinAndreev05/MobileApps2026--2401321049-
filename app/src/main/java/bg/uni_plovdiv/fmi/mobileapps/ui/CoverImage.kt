package bg.uni_plovdiv.fmi.mobileapps.ui

import android.widget.ImageView
import bg.uni_plovdiv.fmi.mobileapps.R
import coil3.load
import coil3.request.error
import coil3.request.placeholder

/** Shows a book cover from [url], or the placeholder book while loading, on error or without a URL. */
fun ImageView.loadCover(url: String?) {
    if (url == null) {
        setImageResource(R.drawable.ic_cover_placeholder)
        return
    }
    load(url) {
        placeholder(R.drawable.ic_cover_placeholder)
        error(R.drawable.ic_cover_placeholder)
    }
}
