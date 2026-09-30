package mx.tec.sonrisas.ui.state

import androidx.lifecycle.ViewModel
import mx.tec.sonrisas.data.SesionRepository

class LoginViewModel(private val repository: SesionRepository) : ViewModel() {

    fun iniciarSesion(correo: String, contrasena: String) {
        repository.entrar(correo, contrasena)
    }
}
