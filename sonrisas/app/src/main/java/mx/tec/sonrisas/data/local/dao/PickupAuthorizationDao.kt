package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity

@Dao
interface PickupAuthorizationDao {

    @Query("SELECT * FROM pickup_authorizations WHERE childId = :childId ORDER BY startDate DESC")
    fun observeAllByChild(childId: String): Flow<List<PickupAuthorizationEntity>>

    @Upsert
    suspend fun save(authorization: PickupAuthorizationEntity)

    @Query("DELETE FROM pickup_authorizations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pickup_authorizations")
    suspend fun clear()

    @Upsert
    suspend fun saveAll(authorizations: List<PickupAuthorizationEntity>)

    @Transaction
    suspend fun replaceAll(authorizations: List<PickupAuthorizationEntity>) {
        clear()
        saveAll(authorizations)
    }
}
