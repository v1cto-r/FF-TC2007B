package mx.tec.sonrisas.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** An ISO week (Monday to Friday) the weekly report can cover. */
data class SemanaReporte(val year: Int, val week: Int, val lunes: LocalDate) {
    val viernes: LocalDate get() = lunes.plusDays(4)

    /** "Semana 42 · 12–16 oct 2026", or "Semana 1 · 29 dic – 2 ene 2026" across months. */
    val etiqueta: String
        get() = "Semana $week · ${rango()}"

    /** "lunes 12 oct – viernes 16 oct 2026" */
    val cobertura: String
        get() = "lunes ${dia(lunes)} – viernes ${dia(viernes)} ${viernes.year}"

    private fun rango(): String = if (lunes.month == viernes.month) {
        "${lunes.dayOfMonth}–${dia(viernes)} ${viernes.year}"
    } else {
        "${dia(lunes)} – ${dia(viernes)} ${viernes.year}"
    }

    private fun dia(fecha: LocalDate) = "${fecha.dayOfMonth} ${MESES[fecha.monthValue - 1]}"

    companion object {
        fun de(fecha: LocalDate): SemanaReporte = SemanaReporte(
            year = fecha.get(IsoFields.WEEK_BASED_YEAR),
            week = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
            lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        )
    }
}

/** The week of [hoy] first, then the [cuantas] - 1 weeks before it. */
fun semanasDisponibles(hoy: LocalDate, cuantas: Int = 52): List<SemanaReporte> =
    (0 until cuantas).map { SemanaReporte.de(hoy.minusWeeks(it.toLong())) }
