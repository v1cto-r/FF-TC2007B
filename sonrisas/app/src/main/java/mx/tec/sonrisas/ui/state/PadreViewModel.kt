package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SonrisasRepository
import mx.tec.sonrisas.model.Actividad
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.model.UiState

class PadreViewModel(private val repository: SonrisasRepository) : ViewModel() {

    var estadoActividades by mutableStateOf<UiState<List<Actividad>>>(UiState.Cargando)
        private set

    var estadoMensajes by mutableStateOf<UiState<List<Mensaje>>>(UiState.Cargando)
        private set

    fun cargarInicio() {
        viewModelScope.launch {
            estadoActividades = UiState.Cargando
            try {
                val actividades = repository.obtenerActividadesPadre()
                estadoActividades = if (actividades.isEmpty()) UiState.Vacio else UiState.Exito(actividades)
            } catch (e: Exception) {
                estadoActividades = UiState.Error("Error al cargar actividades")
            }
        }
    }

    fun cargarMensajes() {
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                val mensajes = repository.obtenerMensajesPadre()
                estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
            } catch (e: Exception) {
                estadoMensajes = UiState.Error("Error al cargar chat")
            }
        }
    }

    fun enviarMensaje(texto: String) {
        viewModelScope.launch {
            try {
                val actualizados = repository.enviarMensajePadre(texto)
                estadoMensajes = UiState.Exito(actualizados)
            } catch (e: Exception) {
                // error opcional
            }
        }
    }
}
