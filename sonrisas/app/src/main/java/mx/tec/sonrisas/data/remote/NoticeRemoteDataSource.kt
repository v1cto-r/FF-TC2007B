package mx.tec.sonrisas.data.remote

import java.time.LocalDate
import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.Notice

interface NoticeRemoteDataSource {
    /** The estancia-wide notices plus those of the salones the user can see. */
    suspend fun fetchAll(): List<Notice>
}

class NoticeRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : NoticeRemoteDataSource {

    override suspend fun fetchAll(): List<Notice> =
        auth.authed { api.get<List<NoticeDto>>("/notices", it) }.map { dto ->
            Notice(id = dto.id, name = dto.name, description = dto.description, date = LocalDate.parse(dto.date))
        }
}
