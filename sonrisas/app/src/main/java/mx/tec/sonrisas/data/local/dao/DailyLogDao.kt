package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.DailyLogEntity

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE childId = :childId AND deleted = 0 ORDER BY date DESC")
    fun observeByChild(childId: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE childId = :childId AND date = :date AND deleted = 0 LIMIT 1")
    fun observeByChildAndDate(childId: String, date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE synced = 0 AND deleted = 0")
    suspend fun getPendingUpserts(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE deleted = 1")
    suspend fun getPendingDeletes(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<DailyLogEntity>

    @Upsert
    suspend fun save(dailyLog: DailyLogEntity)

    @Upsert
    suspend fun saveAll(dailyLogs: List<DailyLogEntity>)

    // Logs the server no longer has (deleted there); pending local changes are never touched
    @Query("DELETE FROM daily_logs WHERE synced = 1 AND date >= :fromDate AND id NOT IN (:keepIds)")
    suspend fun deleteSyncedExcept(fromDate: LocalDate, keepIds: List<String>)

    @Query("UPDATE daily_logs SET deleted = 1, synced = 0, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun markDeleted(id: String, modifiedAt: Long)

    // Only if nothing changed since the upload started; otherwise the newer version stays pending
    @Query("UPDATE daily_logs SET synced = 1 WHERE id = :id AND modifiedAt = :modifiedAt AND deleted = 0")
    suspend fun markSynced(id: String, modifiedAt: Long)

    @Query("DELETE FROM daily_logs WHERE id = :id AND modifiedAt = :modifiedAt AND deleted = 1")
    suspend fun purge(id: String, modifiedAt: Long)
}
