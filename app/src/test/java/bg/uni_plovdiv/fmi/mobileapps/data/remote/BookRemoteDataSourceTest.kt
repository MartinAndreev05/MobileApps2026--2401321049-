package bg.uni_plovdiv.fmi.mobileapps.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BookRemoteDataSourceTest {

    private val api: OpenLibraryApi = mock()
    private val dataSource = BookRemoteDataSource(api)

    @Test
    fun search_mapsResultsAndSkipsUnusableOnes() = runTest {
        api.stub {
            onBlocking { search(any(), any(), any()) } doReturn SearchResponseDto(
                numFound = 2,
                docs = listOf(
                    BookDocDto(key = "/works/1", title = "Dune", authorNames = listOf("Frank Herbert")),
                    BookDocDto(key = "/works/2", title = null),
                ),
            )
        }

        val books = dataSource.search("dune")

        assertEquals(1, books.size)
        assertEquals("Dune", books[0].title)
        assertEquals("Frank Herbert", books[0].author)
    }

    @Test
    fun search_withoutDocs_returnsEmptyList() = runTest {
        api.stub { onBlocking { search(any(), any(), any()) } doReturn SearchResponseDto() }

        assertTrue(dataSource.search("zzzz").isEmpty())
    }

    @Test
    fun search_failure_throwsNetworkExceptionWithReason() = runTest {
        assertSearchFails(UnknownHostException(), NetworkError.NO_INTERNET)
        assertSearchFails(SocketTimeoutException(), NetworkError.TIMEOUT)
        assertSearchFails(httpError(503), NetworkError.SERVER)
        assertSearchFails(IOException("connection reset"), NetworkError.NO_INTERNET)
        assertSearchFails(IllegalStateException("bad JSON"), NetworkError.UNKNOWN)
    }

    @Test
    fun search_cancelled_isNotTurnedIntoAnError() = runTest {
        val cancelled = CancellationException("newer search started")
        api.stub { onBlocking { search(any(), any(), any()) } doSuspendableAnswer { throw cancelled } }

        try {
            dataSource.search("dune")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertSame(cancelled, e)
        }
    }

    @Test
    fun toNetworkError_mapsEveryKindOfFailure() {
        assertEquals(NetworkError.TIMEOUT, SocketTimeoutException().toNetworkError())
        assertEquals(NetworkError.NO_INTERNET, UnknownHostException().toNetworkError())
        assertEquals(NetworkError.SERVER, httpError(404).toNetworkError())
        assertEquals(NetworkError.NO_INTERNET, IOException().toNetworkError())
        assertEquals(NetworkError.UNKNOWN, RuntimeException().toNetworkError())
    }

    private suspend fun assertSearchFails(failure: Exception, expected: NetworkError) {
        api.stub { onBlocking { search(any(), any(), any()) } doSuspendableAnswer { throw failure } }
        try {
            dataSource.search("dune")
            fail("Expected NetworkException for $failure")
        } catch (e: NetworkException) {
            assertEquals(expected, e.error)
            assertSame(failure, e.cause)
        }
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}
