package bg.uni_plovdiv.fmi.mobileapps.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit description of the Open Library search API.
 * Docs: https://openlibrary.org/dev/docs/api/search
 */
interface OpenLibraryApi {

    /** Example: GET https://openlibrary.org/search.json?q=the+hobbit&fields=...&limit=20 */
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("limit") limit: Int = DEFAULT_LIMIT,
    ): SearchResponseDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"

        /** Only ask for the fields the app uses; keeps the response small. */
        const val SEARCH_FIELDS = "key,title,author_name,cover_i,first_publish_year"
        const val DEFAULT_LIMIT = 20
    }
}
