package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SonrisasRepository
import mx.tec.sonrisas.model.Evento
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.model.Nino
import mx.tec.sonrisas.model.UiState

class MaestraViewModel(private val repository: SonrisasRepository) : ViewModel() {

    var estadoEventos by mutableStateOf<UiState<List<Evento>>>(UiState.Cargando)
        private set

    var estadoNinos by mutableStateOf<UiState<List<Nino>>>(UiState.Cargando)
        private set

    var estadoPerfilNino by mutableStateOf<UiState<Nino>>(UiState.Cargando)
        private set

    var estadoMensajes by mutableStateOf<UiState<List<Mensaje>>>(UiState.Cargando)
        private set

    val presentes: Int = 18
    val total: Int = 20

    fun cargarInicio() {
        viewModelScope.launch {
            estadoEventos = UiState.Cargando
            try {
                val eventos = repository.obtenerEventos()
                estadoEventos = if (eventos.isEmpty()) UiState.Vacio else UiState.Exito(eventos)
            } catch (e: Exception) {
                estadoEventos = UiState.Error("Error al cargar eventos")
            }
        }
    }

    fun cargarNinos() {
        viewModelScope.launch {
            estadoNinos = UiState.Cargando
            try {
                val ninos = repository.obtenerNinos()
                estadoNinos = if (ninos.isEmpty()) UiState.Vacio else UiState.Exito(ninos)
            } catch (e: Exception) {
                estadoNinos = UiState.Error("Error al cargar niños")
            }
        }
    }

    fun cargarPerfilNino(nombre: String) {
        viewModelScope.launch {
            estadoPerfilNino = UiState.Cargando
            try {
                val nino = repository.obtenerNino(nombre)
                estadoPerfilNino = if (nino != null) UiState.Exito(nino) else UiState.Error("Niño no encontrado")
            } catch (e: Exception) {
                estadoPerfilNino = UiState.Error("Error al cargar perfil")
            }
        }
    }

    fun cargarMensajes() {
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                val mensajes = repository.obtenerMensajesMaestra()
                estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
            } catch (e: Exception) {
                estadoMensajes = UiState.Error("Error al cargar chat")
            }
        }
    }

    fun enviarMensaje(texto: String) {
        viewModelScope.launch {
            try {
                val actualizados = repository.enviarMensajeMaestra(texto)
                estadoMensajes = UiState.Exito(actualizados)
            } catch (e: Exception) {
                // error opcional
            }
        }
    }
}
