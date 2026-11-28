package bg.uni_plovdiv.fmi.mobileapps.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the users' book photos in app-private storage (files/photos).
 * Other apps can't read this folder; the camera app and share targets get
 * temporary access to a single file through a content:// URI from [FileProvider].
 */
@Singleton
class PhotoStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val photoDir: File
        get() = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }

    /** A new, empty file for the camera to write a photo into. */
    fun newPhotoFile(): File = File(photoDir, "book_${UUID.randomUUID()}.jpg")

    /** content:// URI that can be handed to another app (camera, share targets). */
    fun uriFor(path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", File(path))

    /** Deletes a photo file; does nothing for null or a missing file. */
    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    companion object {
        /** Must match res/xml/file_paths.xml. */
        private const val PHOTO_DIR = "photos"

        /** Must match the provider authority in AndroidManifest.xml. */
        private const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
