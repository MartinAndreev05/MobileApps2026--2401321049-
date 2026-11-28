package bg.uni_plovdiv.fmi.mobileapps.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A book in the user's reading diary. One row of the "books" table. */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    /** 1 to 5 stars, or null while the book has no rating. */
    val rating: Int? = null,
    val notes: String = "",
    /** Cover image from Open Library, filled in when the book is found online. */
    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,
    /** Absolute path of the user's own photo (app-private storage), if any. Added in DB version 2. */
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    /** Creation and last-change times, in milliseconds since 1970 (UTC). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = createdAt,
)

/** Where the user is with a book. Room stores it as the enum name, e.g. "READING". */
enum class ReadingStatus {
    WANT_TO_READ,
    READING,
    READ,
}
