package mx.tec.sonrisas.domain

data class Feeding(
    val breakfast: AmountEnum,
    val snack: AmountEnum,
    val lunch: AmountEnum,
    val afternoonSnack: AmountEnum
)
