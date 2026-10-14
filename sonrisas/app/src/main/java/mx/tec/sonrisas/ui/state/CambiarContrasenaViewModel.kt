package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SesionRepository

@HiltViewModel
class CambiarContrasenaViewModel @Inject constructor(
    private val repository: SesionRepository
) : ViewModel() {

    var error by mutableStateOf<String?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    fun guardar(actual: String, nueva: String) {
        viewModelScope.launch {
            guardando = true
            error = null
            try {
                repository.cambiarContrasena(actual, nueva)
            } catch (e: Exception) {
                error = mensajeDeError(e, ante401 = "La contraseña actual no es correcta.")
            } finally {
                guardando = false
            }
        }
    }
}
