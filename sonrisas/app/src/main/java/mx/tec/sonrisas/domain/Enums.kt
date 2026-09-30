package mx.tec.sonrisas.domain

enum class GroupEnum { L, M, P }

fun GroupEnum.etiqueta(): String = when (this) {
    GroupEnum.L -> "Lactantes"
    GroupEnum.M -> "Maternales"
    GroupEnum.P -> "Prescolares"
}

enum class AmountEnum { NONE, SOME, ALL, ASKED_FOR_MORE }

fun AmountEnum.etiqueta(): String = when (this) {
    AmountEnum.NONE -> "Nada"
    AmountEnum.SOME -> "Poco"
    AmountEnum.ALL -> "Todo"
    AmountEnum.ASKED_FOR_MORE -> "Pidió más"
}
