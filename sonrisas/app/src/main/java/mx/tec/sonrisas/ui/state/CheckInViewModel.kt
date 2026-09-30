package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.CheckInRecord
import mx.tec.sonrisas.domain.DailyLog

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    var estado by mutableStateOf<UiState<DailyLog?>>(UiState.Cargando)
        private set

    fun cargar(childId: String) {
        viewModelScope.launch {
            estado = UiState.Cargando
            try {
                dailyLogRepository.observeToday(childId).collect { log ->
                    estado = if (log == null) UiState.Vacio else UiState.Exito(log)
                }
            } catch (e: Exception) {
                estado = UiState.Error("Error al cargar")
            }
        }
    }

    fun guardar(childId: String, checkIn: CheckInRecord, onListo: () -> Unit) {
        viewModelScope.launch {
            val existente = dailyLogRepository.observeToday(childId).first()
            val cuidador = sesionRepository.sesion.first()?.nombre ?: "Maestra"
            val log = existente?.copy(checkIn = checkIn)
                ?: DailyLog(childId = childId, date = LocalDate.now(), caregiverName = cuidador, checkIn = checkIn)
            dailyLogRepository.save(log)
            onListo()
        }
    }
}
