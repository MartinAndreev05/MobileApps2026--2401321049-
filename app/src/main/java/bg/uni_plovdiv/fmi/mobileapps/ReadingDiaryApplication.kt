package bg.uni_plovdiv.fmi.mobileapps

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class. @HiltAndroidApp starts Hilt, which then creates the
 * database and repository (see di/DatabaseModule) and injects them where needed.
 */
@HiltAndroidApp
class ReadingDiaryApplication : Application()
