package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.Message

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val chatRepository: ChatRepository,
    private val sesionRepository: SesionRepository
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

    fun guardar(childId: String, nombre: String, onListo: () -> Unit) {
        viewModelScope.launch {
            val log = dailyLogRepository.observeToday(childId).first() ?: return@launch
            dailyLogRepository.saveActivity(ActivityRecord(dailyLogId = log.id, name = nombre))
            val autor = sesionRepository.sesion.first()?.usuario ?: "maestra"
            chatRepository.send(
                Message(
                    chatId = childId,
                    authorId = autor,
                    content = "Nueva actividad registrada: $nombre",
                    sentAt = LocalDateTime.now()
                )
            )
            onListo()
        }
    }
}
