package mx.tec.sonrisas.ui.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.data.ChildRepository
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.ReportRemoteDataSource
import mx.tec.sonrisas.data.sync.SyncScheduler
import mx.tec.sonrisas.domain.SemanaReporte
import mx.tec.sonrisas.domain.semanasDisponibles

@HiltViewModel
class ReporteNinoViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val reportRemote: ReportRemoteDataSource,
    private val syncScheduler: SyncScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val semanas: List<SemanaReporte> = semanasDisponibles(LocalDate.now())

    var nombreNino by mutableStateOf<String?>(null)
        private set
    var seleccionada by mutableStateOf(semanas.first())
        private set
    var generando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // The report is built from what reached the server: records still on this phone are missing from it.
    var pendientes by mutableStateOf(0)
        private set

    fun cargar(ninoId: String) {
        viewModelScope.launch {
            nombreNino = childRepository.observeById(ninoId).first()?.fullName
            pendientes = dailyLogRepository.countPending()
            if (pendientes > 0) syncScheduler.requestSync()
        }
    }

    fun seleccionar(semana: SemanaReporte) {
        seleccionada = semana
        error = null
    }

    fun generar(ninoId: String, onListo: (File) -> Unit) {
        if (generando) return
        val semana = seleccionada
        viewModelScope.launch {
            generando = true
            error = null
            try {
                val pdf = reportRemote.weeklyReport(ninoId, semana.year, semana.week)
                val archivo = withContext(Dispatchers.IO) { guardar(ninoId, semana, pdf) }
                onListo(archivo)
            } catch (e: Exception) {
                error = if (e is ApiException && e.code == 404) "No encontramos a este niño en tu grupo."
                else mensajeDeError(e, ante401 = "Tu sesión expiró. Vuelve a iniciar sesión.")
            } finally {
                generando = false
                pendientes = dailyLogRepository.countPending()
            }
        }
    }

    // Only the latest report is kept: older ones are deleted so the cache doesn't grow.
    private fun guardar(ninoId: String, semana: SemanaReporte, pdf: ByteArray): File {
        val carpeta = File(context.cacheDir, "reportes")
        carpeta.deleteRecursively()
        carpeta.mkdirs()
        return File(carpeta, "reporte_${ninoId}_${semana.year}-W${semana.week}.pdf").apply { writeBytes(pdf) }
    }
}
