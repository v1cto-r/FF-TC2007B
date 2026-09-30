package mx.tec.sonrisas.domain

import java.time.LocalDate
import java.util.UUID

data class Notice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val date: LocalDate
)
