package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import mx.tec.sonrisas.data.BiometricStorageManager
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: SesionRepository,
    private val biometricStorageManager: BiometricStorageManager
) : ViewModel() {

    var errorMensaje by mutableStateOf<String?>(null)
        private set

    var mostrarDialogoRegistroBiometrico by mutableStateOf(false)
        private set

    var sesionPendienteParaBiometria by mutableStateOf<Sesion?>(null)
        private set

    val esBiometricoRegistrado: Boolean
        get() = biometricStorageManager.isBiometricRegistered()

    val usuarioGuardadoBiometrico: String?
        get() = biometricStorageManager.getUsuarioGuardado()

    fun iniciarSesion(correo: String, contrasena: String): Boolean {
        val exito = repository.entrar(correo, contrasena)
        if (!exito) {
            errorMensaje = "Credenciales incorrectas. Ingresa un usuario y contraseña válidos."
            return false
        }

        errorMensaje = null
        val sesionActual = (repository.sesion as? StateFlow<Sesion?>)?.value

        if (sesionActual != null) {
            if (!biometricStorageManager.isBiometricRegistered()) {
                sesionPendienteParaBiometria = sesionActual
                mostrarDialogoRegistroBiometrico = true
                return true
            }
        }
        return true
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
        val usuario = biometricStorageManager.getUsuarioGuardado() ?: "maestra@sonrisas.edu.mx"
        val nombre = biometricStorageManager.getNombreGuardado() ?: "Maestra Laura"
        val rol = biometricStorageManager.getRolGuardado() ?: Rol.MAESTRA

        repository.entrarConSesion(usuario = usuario, nombre = nombre, rol = rol)
    }

    fun cancelarRegistroBiometricoExistente() {
        biometricStorageManager.cancelarBiometrico()
    }

    fun limpiarError() {
        errorMensaje = null
    }
}
