package bg.uni_plovdiv.fmi.mobileapps.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Why an online search failed. The screen turns it into a message. */
enum class NetworkError {
    /** No connection, or the server could not be reached. */
    NO_INTERNET,

    /** The server took too long to answer. */
    TIMEOUT,

    /** The server answered with an error (HTTP 4xx / 5xx). */
    SERVER,

    /** Anything else, e.g. a response that is not valid JSON. */
    UNKNOWN,
}

/** Thrown by [BookRemoteDataSource] with the reason the request failed. */
class NetworkException(val error: NetworkError, cause: Throwable? = null) : Exception(cause)

/**
 * Talks to Open Library. All network code lives here, so the repository and
 * the screens only see [OnlineBook]s or a [NetworkException].
 */
class BookRemoteDataSource @Inject constructor(
    private val api: OpenLibraryApi,
) {

    suspend fun search(query: String): List<OnlineBook> {
        val response = try {
            api.search(query)
        } catch (e: CancellationException) {
            throw e // the search was cancelled (e.g. a newer one started); not an error
        } catch (e: Exception) {
            throw NetworkException(e.toNetworkError(), e)
        }
        return response.docs.orEmpty().mapNotNull { it.toOnlineBook() }
    }
}

/** Maps a failure from Retrofit/OkHttp to the reason shown to the user. */
fun Throwable.toNetworkError(): NetworkError = when (this) {
    is SocketTimeoutException -> NetworkError.TIMEOUT
    is UnknownHostException -> NetworkError.NO_INTERNET
    is HttpException -> NetworkError.SERVER
    is IOException -> NetworkError.NO_INTERNET
    else -> NetworkError.UNKNOWN
}
