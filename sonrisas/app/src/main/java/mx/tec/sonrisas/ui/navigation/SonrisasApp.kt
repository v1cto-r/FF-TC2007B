package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.screens.login.LoginScreen
import mx.tec.sonrisas.ui.state.LoginViewModel
import mx.tec.sonrisas.ui.state.SesionEstado
import mx.tec.sonrisas.ui.state.SesionViewModel

/**
 * La raíz de la app. Observa el estado de la sesión.
 * Sin sesión activa, muestra el Login.
 * Con sesión activa, levanta SonrisasNavHost con la navegación completa.
 */
@Composable
fun SonrisasApp() {
    val sesionViewModel: SesionViewModel = hiltViewModel()
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()

    when (val actual = estado) {
        SesionEstado.Cargando -> EstadoCargando()

        SesionEstado.Anonimo -> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                errorMensaje = loginViewModel.errorMensaje,
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

        is SesionEstado.Activa -> SonrisasNavHost(
            sesion = actual.sesion,
            onSalir = sesionViewModel::salir
        )
    }
}
