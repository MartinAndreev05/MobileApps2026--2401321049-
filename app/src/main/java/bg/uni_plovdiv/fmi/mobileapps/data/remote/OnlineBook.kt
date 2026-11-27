package bg.uni_plovdiv.fmi.mobileapps.data.remote

/** A book found in Open Library, in the form the app uses. */
data class OnlineBook(
    val key: String,
    val title: String,
    /** Authors joined with ", ", or an empty string if Open Library has none. */
    val author: String,
    val coverUrl: String?,
    val firstPublishYear: Int?,
)

private const val COVER_URL = "https://covers.openlibrary.org/b/id/%d-M.jpg"

/** Medium-size cover image URL for an Open Library cover id. */
fun coverUrlFor(coverId: Long): String = COVER_URL.format(coverId)

/**
 * Converts one search result into an [OnlineBook].
 * Returns null for results the app can't use (no id or no title).
 */
fun BookDocDto.toOnlineBook(): OnlineBook? {
    val cleanTitle = title?.trim()
    if (key.isNullOrBlank() || cleanTitle.isNullOrEmpty()) return null
    return OnlineBook(
        key = key,
        title = cleanTitle,
        author = authorNames.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", "),
        coverUrl = coverId?.takeIf { it > 0 }?.let(::coverUrlFor),
        firstPublishYear = firstPublishYear,
    )
}
