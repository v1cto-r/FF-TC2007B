package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.ActivityRecordEntity

@Dao
interface ActivityRecordDao {

    @Query("SELECT * FROM activity_records WHERE dailyLogId = :dailyLogId AND deleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeByDailyLog(dailyLogId: String): Flow<List<ActivityRecordEntity>>

    // Activities of a deleted log are skipped: deleting the log on the server removes them too
    @Query(
        """SELECT a.* FROM activity_records a JOIN daily_logs d ON d.id = a.dailyLogId
           WHERE a.synced = 0 AND a.deleted = 0 AND d.deleted = 0"""
    )
    suspend fun getPendingUpserts(): List<ActivityRecordEntity>

    @Query("SELECT * FROM activity_records WHERE deleted = 1")
    suspend fun getPendingDeletes(): List<ActivityRecordEntity>

    @Query("SELECT id FROM activity_records WHERE synced = 0")
    suspend fun getUnsyncedIds(): List<String>

    @Upsert
    suspend fun save(activity: ActivityRecordEntity)

    @Upsert
    suspend fun saveAll(activities: List<ActivityRecordEntity>)

    @Query("DELETE FROM activity_records WHERE synced = 1 AND dailyLogId IN (:dailyLogIds) AND id NOT IN (:keepIds)")
    suspend fun deleteSyncedExcept(dailyLogIds: List<String>, keepIds: List<String>)

    @Query("UPDATE activity_records SET deleted = 1, synced = 0, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun markDeleted(id: String, modifiedAt: Long)

    @Query("UPDATE activity_records SET synced = 1 WHERE id = :id AND modifiedAt = :modifiedAt AND deleted = 0")
    suspend fun markSynced(id: String, modifiedAt: Long)

    @Query("DELETE FROM activity_records WHERE id = :id AND modifiedAt = :modifiedAt AND deleted = 1")
    suspend fun purge(id: String, modifiedAt: Long)
}
