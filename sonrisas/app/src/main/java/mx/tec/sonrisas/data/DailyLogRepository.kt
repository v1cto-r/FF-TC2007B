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
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSource
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog

/**
 * Same as ChildRepository: decides Room vs. API
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
    private val remote: DailyLogRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) : DailyLogRepository {

    override fun observeByChild(childId: String): Flow<List<DailyLog>> =
        dailyLogDao.observeByChild(childId).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<DailyLog?> =
        dailyLogDao.observeById(id).map { row -> row?.toDomain() }

    override fun observeToday(childId: String): Flow<DailyLog?> =
        dailyLogDao.observeByChildAndDate(childId, LocalDate.now()).map { row -> row?.toDomain() }

    override suspend fun save(dailyLog: DailyLog) {
        if (networkMonitor.isOnline()) {
            remote.saveDailyLog(dailyLog)
        } else {
            dailyLogDao.save(dailyLog.toEntity())
        }
    }

    override suspend fun delete(dailyLog: DailyLog) {
        if (networkMonitor.isOnline()) {
            remote.deleteDailyLog(dailyLog)
        } else {
            dailyLogDao.delete(dailyLog.toEntity())
        }
    }

    override fun observeActivities(dailyLogId: String): Flow<List<ActivityRecord>> =
        activityDao.observeByDailyLog(dailyLogId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveActivity(activity: ActivityRecord) {
        if (networkMonitor.isOnline()) {
            remote.saveActivity(activity)
        } else {
            activityDao.save(activity.toEntity())
        }
    }

    override suspend fun deleteActivity(activity: ActivityRecord) {
        if (networkMonitor.isOnline()) {
            remote.deleteActivity(activity)
        } else {
            activityDao.delete(activity.toEntity())
        }
    }
}
