package mx.tec.sonrisas.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A photo on this phone waiting for DailyLogSyncWorker; the file is named after its server file id. */
data class StoredPhoto(val fileId: String, val path: String)

/**
 * Photos taken offline (the ID of whoever picks a child up, activity photos) wait here until they are
 * uploaded. Not in the cache directory: the system may empty that before the phone is back online.
 */
@Singleton
class PendingPhotoStore @Inject constructor(@ApplicationContext private val context: Context) {

    /** The file id is chosen now, so a retried upload keeps the same one. */
    suspend fun keep(jpeg: ByteArray): StoredPhoto = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val file = File(dir(), "$fileId.jpg")
        file.writeBytes(jpeg)
        StoredPhoto(fileId, file.absolutePath)
    }

    fun delete(path: String) {
        File(path).delete()
    }

    /** On logout: whatever wasn't uploaded is discarded with the records that pointed at it. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir().deleteRecursively()
    }

    private fun dir() = File(context.filesDir, DIR).apply { mkdirs() }

    companion object {
        private const val DIR = "pending_photos"

        fun fileIdOf(path: String): String = File(path).nameWithoutExtension
    }
}
