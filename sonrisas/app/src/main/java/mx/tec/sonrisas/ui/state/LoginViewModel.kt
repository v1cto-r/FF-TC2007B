package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import mx.tec.sonrisas.data.SesionRepository

class LoginViewModel(private val repository: SesionRepository) : ViewModel() {

    var errorMensaje by mutableStateOf<String?>(null)
        private set

    fun iniciarSesion(correo: String, contrasena: String): Boolean {
        val exito = repository.entrar(correo, contrasena)
        if (!exito) {
            errorMensaje = "Credenciales incorrectas. Ingresa un usuario y contraseña válidos."
        } else {
            errorMensaje = null
        }
        return exito
    }

    fun limpiarError() {
        errorMensaje = null
    }
}
