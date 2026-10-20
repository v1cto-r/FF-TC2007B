package mx.tec.sonrisas.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.sync.withLock
import mx.tec.sonrisas.data.PendingPhotoStore
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.local.entities.DailyLogEntity
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSource
import mx.tec.sonrisas.data.remote.FileRemoteDataSource

/**
 * Uploads the daily logs (check-in, stay, check-out) and activities the teacher saved in Room.
 *
 * Order matters: deletes go first because the server allows only one live log per child and day,
 * and logs go before their activities because an activity needs its log to exist on the server.
 */
@HiltWorker
class DailyLogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyLogDao: DailyLogDao,
    private val activityDao: ActivityRecordDao,
    private val remote: DailyLogRemoteDataSource,
    private val files: FileRemoteDataSource,
    private val lock: DailyLogSyncLock
) : CoroutineWorker(context, params) {

    private var retry = false

    override suspend fun doWork(): Result = lock.mutex.withLock {
        for (row in dailyLogDao.getPendingDeletes()) {
            attempt("delete log ${row.id}") {
                remote.deleteDailyLog(row.toDomain())
                dailyLogDao.purge(row.id, row.modifiedAt)
            }
        }
        for (row in dailyLogDao.getPendingUpserts()) {
            attempt("upload log ${row.id}") {
                uploadIdPhoto(row)
                remote.saveDailyLog(row.toDomain())
                dailyLogDao.markSynced(row.id, row.modifiedAt)
            }
        }
        for (row in activityDao.getPendingDeletes()) {
            attempt("delete activity ${row.id}") {
                remote.deleteActivity(row.toDomain())
                activityDao.purge(row.id, row.modifiedAt)
            }
        }
        for (row in activityDao.getPendingUpserts()) {
            attempt("upload activity ${row.id}") {
                uploadActivityPhotos(row.id, row.dailyLogId)
                remote.saveActivity(row.toDomain())
                activityDao.markSynced(row.id, row.modifiedAt)
            }
        }
        if (retry) Result.retry() else Result.success()
    }

    /** The ID photo of a check-out to an authorized person has to be on the server before the log refers to it. */
    private suspend fun uploadIdPhoto(row: DailyLogEntity) {
        val checkOut = row.checkOut ?: return
        val path = checkOut.idPhotoLocalPath ?: return
        val fileId = checkOut.idPhotoFileId ?: return
        val photo = File(path)
        if (photo.exists()) {
            files.uploadImage(fileId, PICKUP_ID_PHOTO, row.childId, "image/jpeg", photo.readBytes())
        }
        dailyLogDao.markIdPhotoUploaded(row.id, fileId)
        photo.delete()
    }

    /** Each photo is uploaded and taken off the list on its own, so a retry only sends what is missing. */
    private suspend fun uploadActivityPhotos(activityId: String, dailyLogId: String) {
        val pending = activityDao.getById(activityId)?.pendingImagePaths ?: return
        if (pending.isEmpty()) return
        val childId = activityDao.childIdOf(dailyLogId) ?: return
        var left = pending
        for (path in pending) {
            val photo = File(path)
            if (photo.exists()) {
                files.uploadImage(PendingPhotoStore.fileIdOf(path), ACTIVITY_IMAGE, childId, "image/jpeg", photo.readBytes())
            }
            left = left - path
            activityDao.setPendingImagePaths(activityId, left)
            photo.delete()
        }
    }

    /**
     * Network and server errors are retried with backoff. Any other 4xx means the server rejected
     * the row itself (invalid field, record archived...): retrying won't change that, so it is logged
     * and left pending for the next sync instead of holding back the rest.
     */
    private suspend fun attempt(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: IOException) {
            retry = true
        } catch (e: ApiException) {
            if (e.code in 400..499 && e.code !in RETRYABLE) {
                Log.w(TAG, "Server rejected $what: ${e.message}")
            } else {
                retry = true
            }
        }
    }

    private companion object {
        const val TAG = "DailyLogSync"
        const val PICKUP_ID_PHOTO = "PICKUP_ID_PHOTO"
        const val ACTIVITY_IMAGE = "ACTIVITY_IMAGE"
        // 401: session expired or logged out; 404: an activity whose log hasn't been uploaded yet
        val RETRYABLE = setOf(401, 404, 408, 429)
    }
}
