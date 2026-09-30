package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.ActivityRecordEntity

@Dao
interface ActivityRecordDao {

    @Query("SELECT * FROM activity_records WHERE dailyLogId = :dailyLogId ORDER BY name COLLATE NOCASE ASC")
    fun observeByDailyLog(dailyLogId: String): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records WHERE synced = 0")
    suspend fun getPendingSync(): List<ActivityRecordEntity>

    @Upsert
    suspend fun save(activity: ActivityRecordEntity)

    @Delete
    suspend fun delete(activity: ActivityRecordEntity)
}
