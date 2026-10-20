package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.Stay

@HiltViewModel
class StayViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    var estado by mutableStateOf<UiState<DailyLog?>>(UiState.Cargando)
        private set

    val hayIngreso: Boolean
        get() = (estado as? UiState.Exito)?.datos != null

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

    fun guardar(childId: String, stay: Stay, onListo: () -> Unit) {
        viewModelScope.launch {
            val actual = dailyLogRepository.observeToday(childId).first() ?: return@launch
            dailyLogRepository.save(actual.copy(stay = stay))
            onListo()
        }
    }
}
