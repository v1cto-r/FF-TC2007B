package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val daycareCenter: String,
    val group: String,
    val parentName: String
)
