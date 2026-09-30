package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.screens.login.LoginScreen
import mx.tec.sonrisas.ui.state.AppViewModelProvider
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
    val sesionViewModel: SesionViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()

    when (val actual = estado) {
        SesionEstado.Cargando -> EstadoCargando()

        SesionEstado.Anonimo -> {
            val loginViewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LoginScreen(
                onIniciarSesion = { correo, contrasena ->
                    loginViewModel.iniciarSesion(correo, contrasena)
                }
            )
        }

        is SesionEstado.Activa -> SonrisasNavHost(
            sesion = actual.sesion,
            onSalir = sesionViewModel::salir
        )
    }
}
