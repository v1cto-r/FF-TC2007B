package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.screens.login.CambiarContrasenaScreen
import mx.tec.sonrisas.ui.screens.login.LoginScreen
import mx.tec.sonrisas.ui.state.CambiarContrasenaViewModel
import mx.tec.sonrisas.ui.state.LoginViewModel
import mx.tec.sonrisas.ui.state.SesionEstado
import mx.tec.sonrisas.ui.state.SesionViewModel

/**
 * La raíz de la app. Observa el estado de la sesión.
 * Sin sesión activa, muestra el Login.
 * Con sesión activa, levanta SonrisasNavHost con la navegación completa.
 */
@Composable
fun SonrisasApp(chatPendiente: String? = null, onChatAbierto: () -> Unit = {}) {
    val sesionViewModel: SesionViewModel = hiltViewModel()
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()

    when (val actual = estado) {
        SesionEstado.Cargando -> EstadoCargando()

        SesionEstado.Anonimo -> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                errorMensaje = loginViewModel.errorMensaje,
                cargando = loginViewModel.cargando,
                esBiometricoRegistrado = loginViewModel.esBiometricoRegistrado,
                mostrarDialogoRegistroBiometrico = loginViewModel.mostrarDialogoRegistroBiometrico,
                onIniciarSesion = { correo, contrasena ->
                    loginViewModel.iniciarSesion(correo, contrasena)
                },
                onIniciarBiometricoExitoso = {
                    loginViewModel.iniciarSesionBiometricoExitoso()
                },
                onConfirmarRegistroBiometrico = {
                    loginViewModel.confirmarRegistroBiometrico()
                },
                onOmitirRegistroBiometrico = {
                    loginViewModel.omitirRegistroBiometrico()
                }
            )
        }

        is SesionEstado.Activa -> if (actual.sesion.pendiente) {
            val cambioViewModel: CambiarContrasenaViewModel = hiltViewModel()
            CambiarContrasenaScreen(
                errorMensaje = cambioViewModel.error,
                guardando = cambioViewModel.guardando,
                onGuardar = cambioViewModel::guardar,
                onSalir = sesionViewModel::salir
            )
        } else {
            SonrisasNavHost(
                sesion = actual.sesion,
                chatPendiente = chatPendiente,
                onChatAbierto = onChatAbierto,
                onSalir = sesionViewModel::salir
            )
        }
    }
}
