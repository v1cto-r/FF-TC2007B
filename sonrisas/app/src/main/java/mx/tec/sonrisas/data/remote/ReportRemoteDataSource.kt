package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository

interface ReportRemoteDataSource {
    /** The PDF with one child's logs for Monday to Friday of an ISO week. */
    suspend fun weeklyReport(childId: String, year: Int, week: Int): ByteArray
}

class ReportRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : ReportRemoteDataSource {

    override suspend fun weeklyReport(childId: String, year: Int, week: Int): ByteArray =
        auth.authed {
            api.download("/daily-logs/report?child_id=$childId&year=$year&week=$week", it, timeoutSeconds = 60)
        }
}
