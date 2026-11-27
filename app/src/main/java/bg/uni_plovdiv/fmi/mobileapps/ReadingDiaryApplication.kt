package bg.uni_plovdiv.fmi.mobileapps

import android.app.Application
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.AppDatabase

/**
 * Application class. Creates the app-wide objects once and hands them to the ViewModels.
 * (Manual dependency injection; Hilt takes over this job later.)
 */
class ReadingDiaryApplication : Application() {

    val bookRepository: BookRepository by lazy {
        BookRepository(AppDatabase.getInstance(this).bookDao())
    }
}
