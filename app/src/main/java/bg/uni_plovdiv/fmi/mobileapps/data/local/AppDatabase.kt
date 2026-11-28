package bg.uni_plovdiv.fmi.mobileapps.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's local SQLite database (file "reading_diary.db").
 *
 * Hilt keeps a single instance for the whole app (see di/DatabaseModule).
 * The schema of every version is exported to app/schemas; Room compares
 * those files to generate the migrations, so existing users keep their books.
 *
 * Versions:
 * 1. books table
 * 2. books.photo_path column for the user's own photo
 */
@Database(
    entities = [Book::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        private const val DATABASE_NAME = "reading_diary.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
