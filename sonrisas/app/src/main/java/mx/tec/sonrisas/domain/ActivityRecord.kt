package mx.tec.sonrisas.domain

import java.util.UUID

data class ActivityRecord(
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val name: String,
    val images: List<String> = emptyList(),
    val synced: Boolean = false
)
