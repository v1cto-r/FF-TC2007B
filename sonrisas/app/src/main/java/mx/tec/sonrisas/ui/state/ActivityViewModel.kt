package mx.tec.sonrisas.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.PendingPhotoStore
import mx.tec.sonrisas.data.StoredPhoto
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.Message

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val chatRepository: ChatRepository,
    private val sesionRepository: SesionRepository,
    private val photoStore: PendingPhotoStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Photos taken for the activity being written; uploaded by the sync worker once it is saved. */
    var fotos by mutableStateOf<List<StoredPhoto>>(emptyList())
        private set

    var leyendoFoto by mutableStateOf(false)
        private set

    private var guardado = false

    fun agregarFoto(uri: Uri) {
        viewModelScope.launch {
            leyendoFoto = true
            context.leerYBorrarFoto(uri)?.let { fotos = fotos + photoStore.keep(it.second) }
            leyendoFoto = false
        }
    }

    fun quitarFoto(foto: StoredPhoto) {
        photoStore.delete(foto.path)
        fotos = fotos - foto
    }

    override fun onCleared() {
        if (!guardado) fotos.forEach { photoStore.delete(it.path) }  // left without saving
    }

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
            dailyLogRepository.saveActivity(
                ActivityRecord(
                    dailyLogId = log.id,
                    name = nombre,
                    images = fotos.map { it.fileId },
                    pendingImagePaths = fotos.map { it.path }
                )
            )
            guardado = true
            val autor = sesionRepository.sesion.first()?.id ?: ""
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
