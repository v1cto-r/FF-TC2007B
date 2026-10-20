package mx.tec.sonrisas.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSource

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
    private val remote: DailyLogRemoteDataSource
) : CoroutineWorker(context, params) {

    private var retry = false

    override suspend fun doWork(): Result {
        for (row in dailyLogDao.getPendingDeletes()) {
            attempt("delete log ${row.id}") {
                remote.deleteDailyLog(row.toDomain())
                dailyLogDao.purge(row.id, row.modifiedAt)
            }
        }
        for (row in dailyLogDao.getPendingUpserts()) {
            attempt("upload log ${row.id}") {
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
                remote.saveActivity(row.toDomain())
                activityDao.markSynced(row.id, row.modifiedAt)
            }
        }
        return if (retry) Result.retry() else Result.success()
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
        // 401: session expired or logged out; 404: an activity whose log hasn't been uploaded yet
        val RETRYABLE = setOf(401, 404, 408, 429)
    }
}
