package bg.uni_plovdiv.fmi.mobileapps.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnlineBookMapperTest {

    @Test
    fun fullResult_isMapped() {
        val dto = BookDocDto(
            key = "/works/OL27482W",
            title = "The Hobbit",
            authorNames = listOf("J.R.R. Tolkien"),
            coverId = 14627509,
            firstPublishYear = 1937,
        )

        val book = dto.toOnlineBook()

        assertEquals(
            OnlineBook(
                key = "/works/OL27482W",
                title = "The Hobbit",
                author = "J.R.R. Tolkien",
                coverUrl = "https://covers.openlibrary.org/b/id/14627509-M.jpg",
                firstPublishYear = 1937,
            ),
            book,
        )
    }

    @Test
    fun severalAuthors_areJoinedAndBlankOnesSkipped() {
        val dto = BookDocDto(
            key = "/works/1",
            title = "Good Omens",
            authorNames = listOf(" Terry Pratchett ", "", "Neil Gaiman"),
            coverId = null,
            firstPublishYear = null,
        )

        assertEquals("Terry Pratchett, Neil Gaiman", dto.toOnlineBook()?.author)
    }

    @Test
    fun missingOptionalFields_giveEmptyAuthorAndNoCover() {
        val dto = BookDocDto(key = "/works/1", title = "  Title  ", authorNames = null, coverId = null, firstPublishYear = null)

        val book = dto.toOnlineBook()!!

        assertEquals("Title", book.title)
        assertEquals("", book.author)
        assertNull(book.coverUrl)
        assertNull(book.firstPublishYear)
    }

    @Test
    fun invalidCoverId_givesNoCover() {
        val dto = BookDocDto(key = "/works/1", title = "Title", authorNames = null, coverId = -1, firstPublishYear = null)

        assertNull(dto.toOnlineBook()?.coverUrl)
    }

    @Test
    fun resultWithoutKeyOrTitle_isSkipped() {
        assertNull(BookDocDto(key = null, title = "Title", authorNames = null, coverId = null, firstPublishYear = null).toOnlineBook())
        assertNull(BookDocDto(key = " ", title = "Title", authorNames = null, coverId = null, firstPublishYear = null).toOnlineBook())
        assertNull(BookDocDto(key = "/works/1", title = null, authorNames = null, coverId = null, firstPublishYear = null).toOnlineBook())
        assertNull(BookDocDto(key = "/works/1", title = "  ", authorNames = null, coverId = null, firstPublishYear = null).toOnlineBook())
    }
}
