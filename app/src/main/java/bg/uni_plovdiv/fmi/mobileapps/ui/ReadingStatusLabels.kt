package bg.uni_plovdiv.fmi.mobileapps.ui

import androidx.annotation.StringRes
import bg.uni_plovdiv.fmi.mobileapps.R
import bg.uni_plovdiv.fmi.mobileapps.data.local.ReadingStatus

/** The text shown to the user for each reading status. */
@StringRes
fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.WANT_TO_READ -> R.string.status_want_to_read
    ReadingStatus.READING -> R.string.status_reading
    ReadingStatus.READ -> R.string.status_read
}
