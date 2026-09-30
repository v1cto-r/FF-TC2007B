package mx.tec.sonrisas.domain

import java.util.UUID

data class Child(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val daycareCenter: String,
    val group: GroupEnum,
    val parentName: String
)
