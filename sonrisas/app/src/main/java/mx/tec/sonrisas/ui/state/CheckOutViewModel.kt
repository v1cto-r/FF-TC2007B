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
import mx.tec.sonrisas.data.PendingPhotoStore
import mx.tec.sonrisas.data.PickupAuthorizationRepository
import mx.tec.sonrisas.data.StoredPhoto
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.PickupAuthorization

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val pickupAuthorizationRepository: PickupAuthorizationRepository,
    private val photoStore: PendingPhotoStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var estado by mutableStateOf<UiState<DailyLog?>>(UiState.Cargando)
        private set

    var autorizacionHoy by mutableStateOf<PickupAuthorization?>(null)
        private set

    /** The ID of whoever picks up, when it is someone the parent authorized. */
    var fotoIdentificacion by mutableStateOf<StoredPhoto?>(null)
        private set

    var leyendoFoto by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var guardado = false

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

    fun tomarFotoIdentificacion(uri: Uri) {
        viewModelScope.launch {
            leyendoFoto = true
            val foto = context.leerYBorrarFoto(uri)
            leyendoFoto = false
            if (foto == null) {
                error = "No se pudo leer la foto"
                return@launch
            }
            fotoIdentificacion?.let { photoStore.delete(it.path) }  // retaken: the old one goes
            fotoIdentificacion = photoStore.keep(foto.second)
            error = null
        }
    }

    fun descartarFotoIdentificacion() {
        fotoIdentificacion?.let { photoStore.delete(it.path) }
        fotoIdentificacion = null
    }

    /**
     * With [autorizacion], the child leaves with that authorized person and the ID photo is attached;
     * without it, it's a regular check-out (the parent or whoever the teacher writes down).
     */
    fun guardar(childId: String, checkOut: CheckOutRecord, autorizacion: PickupAuthorization?, onListo: () -> Unit) {
        val foto = fotoIdentificacion
        if (autorizacion != null && foto == null) {
            error = "Toma una foto de la identificación de quien recoge"
            return
        }
        error = null
        val registro = if (autorizacion != null && foto != null) {
            checkOut.copy(pickupAuthorizationId = autorizacion.id, idPhotoFileId = foto.fileId, idPhotoLocalPath = foto.path)
        } else {
            descartarFotoIdentificacion()
            checkOut
        }
        viewModelScope.launch {
            val actual = dailyLogRepository.observeToday(childId).first() ?: return@launch
            dailyLogRepository.save(actual.copy(checkOut = registro))
            guardado = true
            onListo()
        }
    }

    override fun onCleared() {
        if (!guardado) descartarFotoIdentificacion()  // left without saving: nothing will upload it
    }
}
