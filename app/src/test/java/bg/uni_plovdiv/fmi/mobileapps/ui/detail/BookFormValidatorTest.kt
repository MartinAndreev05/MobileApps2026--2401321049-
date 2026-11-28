package bg.uni_plovdiv.fmi.mobileapps.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFormValidatorTest {

    @Test
    fun validValues_haveNoErrors() {
        val errors = BookFormValidator.validate("The Hobbit", "J.R.R. Tolkien", "")

        assertTrue(errors.isValid)
        assertEquals(BookFormErrors(), errors)
    }

    @Test
    fun emptyTitleAndAuthor_areRequired() {
        val errors = BookFormValidator.validate("", "", "")

        assertFalse(errors.isValid)
        assertEquals(FieldError.REQUIRED, errors.title)
        assertEquals(FieldError.REQUIRED, errors.author)
        assertNull(errors.notes)
    }

    @Test
    fun blankTitle_isRequired() {
        val errors = BookFormValidator.validate("   ", "Author", "")

        assertEquals(FieldError.REQUIRED, errors.title)
        assertNull(errors.author)
    }

    @Test
    fun valuesAtMaxLength_areAccepted() {
        val errors = BookFormValidator.validate(
            title = "a".repeat(BookFormValidator.MAX_TITLE_LENGTH),
            author = "b".repeat(BookFormValidator.MAX_AUTHOR_LENGTH),
            notes = "c".repeat(BookFormValidator.MAX_NOTES_LENGTH),
        )

        assertTrue(errors.isValid)
    }

    @Test
    fun valuesOverMaxLength_areTooLong() {
        val errors = BookFormValidator.validate(
            title = "a".repeat(BookFormValidator.MAX_TITLE_LENGTH + 1),
            author = "b".repeat(BookFormValidator.MAX_AUTHOR_LENGTH + 1),
            notes = "c".repeat(BookFormValidator.MAX_NOTES_LENGTH + 1),
        )

        assertEquals(FieldError.TOO_LONG, errors.title)
        assertEquals(FieldError.TOO_LONG, errors.author)
        assertEquals(FieldError.TOO_LONG, errors.notes)
        assertFalse(errors.isValid)
    }

    @Test
    fun notes_areOptional() {
        val errors = BookFormValidator.validate("Title", "Author", "")

        assertNull(errors.notes)
    }
}
