package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.BiometricStorageManager
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Sesion

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: SesionRepository,
    private val biometricStorageManager: BiometricStorageManager
) : ViewModel() {

    var errorMensaje by mutableStateOf<String?>(null)
        private set

    var cargando by mutableStateOf(false)
        private set

    var mostrarDialogoRegistroBiometrico by mutableStateOf(false)
        private set

    var sesionPendienteParaBiometria by mutableStateOf<Sesion?>(null)
        private set

    val esBiometricoRegistrado: Boolean
        get() = biometricStorageManager.isBiometricRegistered()

    val usuarioGuardadoBiometrico: String?
        get() = biometricStorageManager.getUsuarioGuardado()

    fun iniciarSesion(correo: String, contrasena: String) {
        viewModelScope.launch {
            cargando = true
            errorMensaje = null
            try {
                repository.entrar(correo, contrasena)
                val sesion = repository.sesion.value
                if (sesion != null && !sesion.pendiente && !biometricStorageManager.isBiometricRegistered()) {
                    sesionPendienteParaBiometria = sesion
                    mostrarDialogoRegistroBiometrico = true
                }
            } catch (e: Exception) {
                errorMensaje = mensajeDeError(e)
            } finally {
                cargando = false
            }
        }
    }

    fun confirmarRegistroBiometrico() {
        val sesion = sesionPendienteParaBiometria
        if (sesion != null) {
            biometricStorageManager.registrarBiometrico(
                usuario = sesion.usuario,
                nombre = sesion.nombre,
                rol = sesion.rol
            )
        }
        mostrarDialogoRegistroBiometrico = false
        sesionPendienteParaBiometria = null
    }

    fun omitirRegistroBiometrico() {
        mostrarDialogoRegistroBiometrico = false
        sesionPendienteParaBiometria = null
    }

    fun iniciarSesionBiometricoExitoso() {
        viewModelScope.launch {
            cargando = true
            if (!repository.restaurar()) {
                errorMensaje = "Tu sesión venció. Inicia sesión con tu contraseña."
                biometricStorageManager.cancelarBiometrico()
            }
            cargando = false
        }
    }

    fun cancelarRegistroBiometricoExistente() {
        biometricStorageManager.cancelarBiometrico()
    }

    fun limpiarError() {
        errorMensaje = null
    }
}
