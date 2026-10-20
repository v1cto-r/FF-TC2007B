package mx.tec.sonrisas.data

import androidx.room.withTransaction
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import mx.tec.sonrisas.data.local.SonrisasDatabase
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSource
import mx.tec.sonrisas.data.remote.toDomain
import mx.tec.sonrisas.data.sync.DailyLogSyncLock
import mx.tec.sonrisas.data.sync.SyncScheduler
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog

/**
 * Room is the source of truth: every write lands there first and is marked pending, then
 * DailyLogSyncWorker uploads it as soon as there is a connection. refreshFromServer() brings
 * down what other devices saved (how a parent sees the teacher's records).
 */
interface DailyLogRepository {
    fun observeByChild(childId: String): Flow<List<DailyLog>>
    fun observeById(id: String): Flow<DailyLog?>
    fun observeToday(childId: String): Flow<DailyLog?>
    suspend fun save(dailyLog: DailyLog)

    /** Undoes today's check-out; the server's copy is removed on the next sync. */
    suspend fun cancelCheckOut(dailyLog: DailyLog)
    suspend fun delete(dailyLog: DailyLog)

    fun observeActivities(dailyLogId: String): Flow<List<ActivityRecord>>
    suspend fun saveActivity(activity: ActivityRecord)
    suspend fun deleteActivity(activity: ActivityRecord)

    /** Brings down the logs of the last [days] days (what other devices saved). */
    suspend fun refreshFromServer(days: Long = DEFAULT_REFRESH_DAYS)

    /** Records saved here that haven't reached the server yet. */
    suspend fun countPending(): Int

    /** On logout: stops the upload and forgets every log, activity and photo on this phone. */
    suspend fun clearLocal()

    companion object {
        const val DEFAULT_REFRESH_DAYS = 30L
    }
}

@Singleton
class DailyLogRepositoryImpl @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val activityDao: ActivityRecordDao,
    private val db: SonrisasDatabase,
    private val remote: DailyLogRemoteDataSource,
    private val syncScheduler: SyncScheduler,
    private val syncLock: DailyLogSyncLock,
    private val photoStore: PendingPhotoStore
) : DailyLogRepository {

    override fun observeByChild(childId: String): Flow<List<DailyLog>> =
        dailyLogDao.observeByChild(childId).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<DailyLog?> =
        dailyLogDao.observeById(id).map { row -> row?.toDomain() }

    override fun observeToday(childId: String): Flow<DailyLog?> =
        dailyLogDao.observeByChildAndDate(childId, LocalDate.now()).map { row -> row?.toDomain() }

    override suspend fun save(dailyLog: DailyLog) {
        val log = dailyLog.copy(checkOutCleared = dailyLog.checkOutCleared && dailyLog.checkOut == null)
        dailyLogDao.save(log.toEntity().copy(synced = false, modifiedAt = System.currentTimeMillis()))
        syncScheduler.requestSync()
    }

    override suspend fun cancelCheckOut(dailyLog: DailyLog) {
        dailyLog.checkOut?.idPhotoLocalPath?.let { File(it).delete() }  // never uploaded: no longer needed
        save(dailyLog.copy(checkOut = null, checkOutCleared = dailyLog.checkOut != null || dailyLog.checkOutCleared))
    }

    override suspend fun delete(dailyLog: DailyLog) {
        dailyLogDao.markDeleted(dailyLog.id, System.currentTimeMillis())
        syncScheduler.requestSync()
    }

    override fun observeActivities(dailyLogId: String): Flow<List<ActivityRecord>> =
        activityDao.observeByDailyLog(dailyLogId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveActivity(activity: ActivityRecord) {
        activityDao.save(activity.toEntity().copy(synced = false, modifiedAt = System.currentTimeMillis()))
        syncScheduler.requestSync()
    }

    override suspend fun deleteActivity(activity: ActivityRecord) {
        activityDao.markDeleted(activity.id, System.currentTimeMillis())
        syncScheduler.requestSync()
    }

    override suspend fun countPending(): Int = dailyLogDao.countUnsynced() + activityDao.countUnsynced()

    override suspend fun clearLocal() {
        syncScheduler.cancel()
        syncLock.mutex.withLock {
            db.withTransaction {
                activityDao.clear()
                dailyLogDao.clear()
            }
        }
        photoStore.deleteAll()
    }

    override suspend fun refreshFromServer(days: Long) = syncLock.mutex.withLock {
        val fromDate = LocalDate.now().minusDays(days)
        val serverLogs = remote.fetchDailyLogs(fromDate)
        db.withTransaction {
            // Local changes not uploaded yet win: overwriting them would lose the teacher's edits.
            // A server log for a child and day that already has a pending local log is skipped too,
            // so observeToday() never sees two logs for the same day.
            val pending = dailyLogDao.getUnsynced()
            val pendingIds = pending.map { it.id }.toSet()
            val pendingDays = pending.map { it.childId to it.date }.toSet()
            val pendingActivityIds = activityDao.getUnsyncedIds().toSet()

            val logs = serverLogs.mapNotNull { dto -> dto.toDomain()?.let { dto to it } }
                .filter { (_, log) -> log.id !in pendingIds && (log.childId to log.date) !in pendingDays }
            dailyLogDao.deleteSyncedExcept(fromDate, serverLogs.map { it.id })
            dailyLogDao.saveAll(logs.map { (_, log) -> log.toEntity() })

            val activities = logs.flatMap { (dto, _) -> dto.activities.map { it.toDomain() } }
            activityDao.deleteSyncedExcept(logs.map { (_, log) -> log.id }, activities.map { it.id })
            activityDao.saveAll(activities.filter { it.id !in pendingActivityIds }.map { it.toEntity() })
        }
    }
}
