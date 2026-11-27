package bg.uni_plovdiv.fmi.mobileapps.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's local SQLite database (file "reading_diary.db").
 *
 * Hilt keeps a single instance for the whole app (see di/DatabaseModule).
 * When the schema changes, raise [version] and add a Migration so existing
 * users keep their books. The schema of every version is exported to app/schemas.
 */
@Database(entities = [Book::class], version = 1, exportSchema = true)
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
