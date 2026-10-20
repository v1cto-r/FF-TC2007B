package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog

interface DailyLogRemoteDataSource {
    suspend fun saveDailyLog(dailyLog: DailyLog)
    suspend fun deleteDailyLog(dailyLog: DailyLog)
    suspend fun saveActivity(activity: ActivityRecord)
    suspend fun deleteActivity(activity: ActivityRecord)
}

/**
 * PUT creates or replaces by the id generated on the phone, so sending the same row twice is
 * harmless: the sync worker can retry freely.
 */
class DailyLogRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : DailyLogRemoteDataSource {

    override suspend fun saveDailyLog(dailyLog: DailyLog) {
        auth.authed { api.send("PUT", "/daily-logs/${dailyLog.id}", it, api.jsonBody(dailyLog.toBody())) }
    }

    override suspend fun deleteDailyLog(dailyLog: DailyLog) = deleteIgnoringMissing("/daily-logs/${dailyLog.id}")

    override suspend fun saveActivity(activity: ActivityRecord) {
        auth.authed { api.send("PUT", "/activities/${activity.id}", it, api.jsonBody(activity.toBody())) }
    }

    override suspend fun deleteActivity(activity: ActivityRecord) = deleteIgnoringMissing("/activities/${activity.id}")

    // 404: it never reached the server or was already deleted; either way it's gone
    private suspend fun deleteIgnoringMissing(path: String) {
        try {
            auth.authed { api.delete(path, it) }
        } catch (e: ApiException) {
            if (e.code != 404) throw e
        }
    }
}
