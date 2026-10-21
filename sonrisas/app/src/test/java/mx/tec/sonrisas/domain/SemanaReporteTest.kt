package mx.tec.sonrisas.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SemanaReporteTest {

    @Test
    fun `a midweek day belongs to its iso week, monday to friday`() {
        val semana = SemanaReporte.de(LocalDate.of(2026, 10, 21))
        assertEquals(2026, semana.year)
        assertEquals(43, semana.week)
        assertEquals(LocalDate.of(2026, 10, 19), semana.lunes)
        assertEquals(LocalDate.of(2026, 10, 23), semana.viernes)
        assertEquals("Semana 43 · 19–23 oct 2026", semana.etiqueta)
        assertEquals("lunes 19 oct – viernes 23 oct 2026", semana.cobertura)
    }

    @Test
    fun `the first days of january can belong to the last week of the previous year`() {
        val semana = SemanaReporte.de(LocalDate.of(2027, 1, 2))
        assertEquals(2026, semana.year)
        assertEquals(53, semana.week)
        assertEquals(LocalDate.of(2026, 12, 28), semana.lunes)
    }

    @Test
    fun `a week across months shows both months`() {
        val semana = SemanaReporte.de(LocalDate.of(2025, 12, 31))
        assertEquals(2026, semana.year)
        assertEquals(1, semana.week)
        assertEquals("Semana 1 · 29 dic – 2 ene 2026", semana.etiqueta)
    }

    @Test
    fun `available weeks start with the current one and go back`() {
        val semanas = semanasDisponibles(LocalDate.of(2026, 1, 7), cuantas = 3)
        assertEquals(listOf(2026 to 2, 2026 to 1, 2025 to 52), semanas.map { it.year to it.week })
    }
}
