package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.ChildEntity

@Dao
interface ChildDao {

    @Query("SELECT * FROM children ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE id = :id")
    fun observeById(id: String): Flow<ChildEntity?>

    @Upsert
    suspend fun save(child: ChildEntity)

    @Delete
    suspend fun delete(child: ChildEntity)
}
