package bg.uni_plovdiv.fmi.mobileapps.data.remote

import com.google.gson.annotations.SerializedName

/*
 * Data transfer objects: the exact shape of Open Library's JSON.
 * Every field is nullable because the API leaves out what it doesn't know
 * (for example books without a cover); the mapper decides what to keep.
 */

data class SearchResponseDto(
    @SerializedName("numFound") val numFound: Int? = null,
    @SerializedName("docs") val docs: List<BookDocDto>? = null,
)

data class BookDocDto(
    /** Work id, e.g. "/works/OL27482W". */
    @SerializedName("key") val key: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("author_name") val authorNames: List<String>? = null,
    @SerializedName("cover_i") val coverId: Long? = null,
    @SerializedName("first_publish_year") val firstPublishYear: Int? = null,
)
