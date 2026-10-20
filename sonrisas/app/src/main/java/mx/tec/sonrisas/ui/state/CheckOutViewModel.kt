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
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.PickupAuthorizationRepository
import mx.tec.sonrisas.data.PickupIdPhotoStore
import mx.tec.sonrisas.data.StoredIdPhoto
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.PickupAuthorization

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val pickupAuthorizationRepository: PickupAuthorizationRepository,
    private val idPhotoStore: PickupIdPhotoStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var estado by mutableStateOf<UiState<DailyLog?>>(UiState.Cargando)
        private set

    var autorizacionHoy by mutableStateOf<PickupAuthorization?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun cargar(childId: String) {
        viewModelScope.launch {
            try {
                pickupAuthorizationRepository.refreshFromServer()
            } catch (e: Exception) {
                e.printStackTrace()  // offline: the copy in Room is used
            }
        }
        viewModelScope.launch {
            estado = UiState.Cargando
            try {
                launch {
                    pickupAuthorizationRepository.observeForChildOn(childId).collect { auth ->
                        autorizacionHoy = auth
                    }
                }
                dailyLogRepository.observeToday(childId).collect { log ->
                    estado = if (log == null) UiState.Vacio else UiState.Exito(log)
                }
            } catch (e: Exception) {
                estado = UiState.Error("Error al cargar")
            }
        }
    }

    /** The camera shot of the ID of whoever picks the child up; its fields go into the CheckOutRecord. */
    fun guardarFotoIdentificacion(uri: Uri, onLista: (StoredIdPhoto) -> Unit) {
        viewModelScope.launch {
            val foto = context.leerFoto(uri)
            if (foto == null) {
                error = "No se pudo leer la foto"
                return@launch
            }
            onLista(idPhotoStore.keep(foto.second))
        }
    }

    fun guardar(childId: String, checkOut: CheckOutRecord, onListo: () -> Unit) {
        if (checkOut.pickupAuthorizationId != null && checkOut.idPhotoFileId == null) {
            error = "Toma una foto de la identificación de quien recoge"
            return
        }
        error = null
        viewModelScope.launch {
            val actual = dailyLogRepository.observeToday(childId).first() ?: return@launch
            dailyLogRepository.save(actual.copy(checkOut = checkOut))
            onListo()
        }
    }
}
