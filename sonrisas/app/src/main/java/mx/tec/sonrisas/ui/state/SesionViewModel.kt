package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.BiometricStorageManager
import mx.tec.sonrisas.data.SesionRepository

@HiltViewModel
class SesionViewModel @Inject constructor(
    private val repository: SesionRepository,
    private val biometricStorageManager: BiometricStorageManager
) : ViewModel() {

    private val restaurando = MutableStateFlow(repository.haySesionGuardada && !biometricStorageManager.isBiometricRegistered())

    val estado: StateFlow<SesionEstado> = combine(repository.sesion, restaurando) { sesion, restaurando ->
        when {
            sesion != null -> SesionEstado.Activa(sesion)
            restaurando -> SesionEstado.Cargando
            else -> SesionEstado.Anonimo
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = if (restaurando.value) SesionEstado.Cargando else SesionEstado.Anonimo
    )

    init {
        if (restaurando.value) {
            viewModelScope.launch {
                repository.restaurar()
                restaurando.value = false
            }
        }
    }

    // Records not uploaded yet that logging out would throw away: waiting for the user to confirm
    var sinEnviarAlSalir by mutableStateOf<Int?>(null)
        private set

    fun salir() {
        viewModelScope.launch {
            val pendientes = repository.registrosSinEnviar()
            if (pendientes > 0) sinEnviarAlSalir = pendientes else salirAhora()
        }
    }

    fun confirmarSalida() {
        sinEnviarAlSalir = null
        viewModelScope.launch { salirAhora() }
    }

    fun cancelarSalida() {
        sinEnviarAlSalir = null
    }

    private suspend fun salirAhora() {
        biometricStorageManager.cancelarBiometrico()
        repository.salir()
    }
}
