package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.NoticeEntity

@Dao
interface NoticeDao {

    @Query("SELECT * FROM notices ORDER BY date DESC")
    fun observeAll(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE id = :id")
    fun observeById(id: String): Flow<NoticeEntity?>

    @Upsert
    suspend fun save(notice: NoticeEntity)

    @Delete
    suspend fun delete(notice: NoticeEntity)

    @Query("DELETE FROM notices")
    suspend fun clear()

    @Upsert
    suspend fun saveAll(notices: List<NoticeEntity>)

    @Transaction
    suspend fun replaceAll(notices: List<NoticeEntity>) {
        clear()
        saveAll(notices)
    }
}
