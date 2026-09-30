package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.DailyLogEntity

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE id = :id")
    fun observeById(id: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE childId = :childId AND date = :date LIMIT 1")
    fun observeByChildAndDate(childId: String, date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE synced = 0")
    suspend fun getPendingSync(): List<DailyLogEntity>

    @Upsert
    suspend fun save(dailyLog: DailyLogEntity)

    @Delete
    suspend fun delete(dailyLog: DailyLogEntity)
}
