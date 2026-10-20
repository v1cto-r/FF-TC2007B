package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity

@Dao
interface PickupAuthorizationDao {

    @Query("SELECT * FROM pickup_authorizations WHERE childId = :childId AND date = :date LIMIT 1")
    fun observeTodayByChild(childId: String, date: LocalDate): Flow<PickupAuthorizationEntity?>

    @Query("SELECT * FROM pickup_authorizations WHERE childId = :childId ORDER BY date DESC")
    fun observeAllByChild(childId: String): Flow<List<PickupAuthorizationEntity>>

    @Upsert
    suspend fun save(authorization: PickupAuthorizationEntity)

    @Delete
    suspend fun delete(authorization: PickupAuthorizationEntity)
}
