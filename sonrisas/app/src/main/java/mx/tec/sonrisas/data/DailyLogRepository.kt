package mx.tec.sonrisas.data

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.sync.SyncScheduler
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog

/**
 * Room is the source of truth: every write lands there first and is marked pending, then
 * DailyLogSyncWorker uploads it as soon as there is a connection.
 */
interface DailyLogRepository {
    fun observeByChild(childId: String): Flow<List<DailyLog>>
    fun observeById(id: String): Flow<DailyLog?>
    fun observeToday(childId: String): Flow<DailyLog?>
    suspend fun save(dailyLog: DailyLog)
    suspend fun delete(dailyLog: DailyLog)

    fun observeActivities(dailyLogId: String): Flow<List<ActivityRecord>>
    suspend fun saveActivity(activity: ActivityRecord)
    suspend fun deleteActivity(activity: ActivityRecord)
}

@Singleton
class DailyLogRepositoryImpl @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val activityDao: ActivityRecordDao,
    private val syncScheduler: SyncScheduler
) : DailyLogRepository {

    override fun observeByChild(childId: String): Flow<List<DailyLog>> =
        dailyLogDao.observeByChild(childId).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<DailyLog?> =
        dailyLogDao.observeById(id).map { row -> row?.toDomain() }

    override fun observeToday(childId: String): Flow<DailyLog?> =
        dailyLogDao.observeByChildAndDate(childId, LocalDate.now()).map { row -> row?.toDomain() }

    override suspend fun save(dailyLog: DailyLog) {
        dailyLogDao.save(dailyLog.toEntity().copy(synced = false, modifiedAt = System.currentTimeMillis()))
        syncScheduler.requestSync()
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
}
