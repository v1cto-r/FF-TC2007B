package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_records",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dailyLogId")]
)
data class ActivityRecordEntity(
    @PrimaryKey val id: String,
    val dailyLogId: String,
    val name: String,
    val images: List<String>,
    val synced: Boolean
)
