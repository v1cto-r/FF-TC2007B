package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val date: LocalDate
)
