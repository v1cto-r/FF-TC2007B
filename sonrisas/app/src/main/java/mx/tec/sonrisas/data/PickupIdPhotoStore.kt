package mx.tec.sonrisas.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredIdPhoto(val fileId: String, val path: String)

/**
 * The ID photo taken at check-out waits here until DailyLogSyncWorker uploads it. Not in the cache
 * directory: the system may empty that before the phone is back online.
 */
@Singleton
class PickupIdPhotoStore @Inject constructor(@ApplicationContext private val context: Context) {

    /** The file id is chosen now, so a retried upload keeps the same one. */
    suspend fun keep(jpeg: ByteArray): StoredIdPhoto = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val file = File(File(context.filesDir, DIR).apply { mkdirs() }, "$fileId.jpg")
        file.writeBytes(jpeg)
        StoredIdPhoto(fileId, file.absolutePath)
    }

    private companion object {
        const val DIR = "pickup_ids"
    }
}
