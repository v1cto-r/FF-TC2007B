package mx.tec.sonrisas.ui.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    sesionViewModel.sinEnviarAlSalir?.let { pendientes ->
        AlertDialog(
            onDismissRequest = sesionViewModel::cancelarSalida,
            title = { Text("Hay registros sin enviar") },
            text = {
                Text(
                    "$pendientes ${if (pendientes == 1) "registro todavía no llega" else "registros todavía no llegan"} " +
                        "a la escuela. Si cierras sesión ahora se perderán. Conéctate a internet y espera a que se envíen."
                )
            },
            confirmButton = { TextButton(onClick = sesionViewModel::confirmarSalida) { Text("Cerrar sesión de todos modos") } },
            dismissButton = { TextButton(onClick = sesionViewModel::cancelarSalida) { Text("Esperar") } }
        )
    }

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
