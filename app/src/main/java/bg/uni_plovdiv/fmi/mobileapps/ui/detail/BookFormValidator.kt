package bg.uni_plovdiv.fmi.mobileapps.ui.detail

/** Why a form field was rejected. The screen turns it into a message. */
enum class FieldError {
    REQUIRED,
    TOO_LONG,
}

/** Validation result for the book form; null means the field is fine. */
data class BookFormErrors(
    val title: FieldError? = null,
    val author: FieldError? = null,
    val notes: FieldError? = null,
) {
    val isValid: Boolean get() = title == null && author == null && notes == null
}

/**
 * Checks the values typed in the book form. Plain Kotlin with no Android
 * dependencies, so it can be unit tested on the JVM. Expects trimmed values.
 */
object BookFormValidator {

    const val MAX_TITLE_LENGTH = 200
    const val MAX_AUTHOR_LENGTH = 100
    const val MAX_NOTES_LENGTH = 2000

    fun validate(title: String, author: String, notes: String): BookFormErrors =
        BookFormErrors(
            title = checkRequired(title, MAX_TITLE_LENGTH),
            author = checkRequired(author, MAX_AUTHOR_LENGTH),
            notes = if (notes.length > MAX_NOTES_LENGTH) FieldError.TOO_LONG else null,
        )

    private fun checkRequired(value: String, maxLength: Int): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        value.length > maxLength -> FieldError.TOO_LONG
        else -> null
    }
}
