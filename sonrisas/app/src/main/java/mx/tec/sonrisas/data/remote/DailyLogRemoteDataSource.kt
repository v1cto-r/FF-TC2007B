package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog

interface DailyLogRemoteDataSource {
    suspend fun saveDailyLog(dailyLog: DailyLog)
    suspend fun deleteDailyLog(dailyLog: DailyLog)
    suspend fun saveActivity(activity: ActivityRecord)
    suspend fun deleteActivity(activity: ActivityRecord)
}

// TODO: implement once the API exists. Never called today because
// NetworkMonitor.isOnline() always returns false.
class DailyLogRemoteDataSourceImpl @Inject constructor() : DailyLogRemoteDataSource {
    override suspend fun saveDailyLog(dailyLog: DailyLog): Unit = TODO("Pending: API integration")
    override suspend fun deleteDailyLog(dailyLog: DailyLog): Unit = TODO("Pending: API integration")
    override suspend fun saveActivity(activity: ActivityRecord): Unit = TODO("Pending: API integration")
    override suspend fun deleteActivity(activity: ActivityRecord): Unit = TODO("Pending: API integration")
}
