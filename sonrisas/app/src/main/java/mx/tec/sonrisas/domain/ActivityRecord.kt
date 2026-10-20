package mx.tec.sonrisas.domain

import java.util.UUID

data class ActivityRecord(
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val name: String,
    // Server file ids of the photos, in order
    val images: List<String> = emptyList(),
    // Photos not uploaded yet, still on this phone
    val pendingImagePaths: List<String> = emptyList(),
    val synced: Boolean = false
)
