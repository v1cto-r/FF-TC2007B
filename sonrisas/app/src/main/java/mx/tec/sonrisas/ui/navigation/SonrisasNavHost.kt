package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion
import mx.tec.sonrisas.ui.screens.maestra.ChatMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.InicioMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.NinosScreen
import mx.tec.sonrisas.ui.screens.maestra.PerfilNinoScreen
import mx.tec.sonrisas.ui.screens.padre.ChatPadreScreen
import mx.tec.sonrisas.ui.screens.padre.InicioPadreScreen
import mx.tec.sonrisas.ui.state.AppViewModelProvider
import mx.tec.sonrisas.ui.state.MaestraViewModel
import mx.tec.sonrisas.ui.state.PadreViewModel

/**
 * NavHost que maneja la navegación por detrás de forma desacoplada de la carga de datos.
 * Las transiciones de pantalla ocurren de inmediato independientemente de la base de datos o API.
 */
@Composable
fun SonrisasNavHost(
    sesion: Sesion,
    onSalir: () -> Unit
) {
    val nav = rememberNavController()
    val destinoInicial = if (sesion.rol == Rol.MAESTRA) Route.INICIO_MAESTRA else Route.INICIO_PADRE

    NavHost(
        navController = nav,
        startDestination = destinoInicial
    ) {
        // --- MAESTRA ---
        composable(Route.INICIO_MAESTRA) {
            val viewModel: MaestraViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(Unit) { viewModel.cargarInicio() }

            InicioMaestraScreen(
                estado = viewModel.estadoEventos,
                presentes = viewModel.presentes,
                total = viewModel.total,
                onReintentar = { viewModel.cargarInicio() },
                onTab = { indice ->
                    when (indice) {
                        0 -> {}
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Route.NINOS) {
            val viewModel: MaestraViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(Unit) { viewModel.cargarNinos() }

            NinosScreen(
                estado = viewModel.estadoNinos,
                onNinoClick = { nino ->
                    nav.navigate(Route.perfilNino(nino.nombre))
                },
                onReintentar = { viewModel.cargarNinos() },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> {}
                        2 -> nav.navigate(Route.CHAT_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(
            route = Route.PERFIL_NINO,
            arguments = listOf(navArgument("nombreNino") { type = NavType.StringType })
        ) { backStackEntry ->
            val nombreNino = backStackEntry.arguments?.getString("nombreNino") ?: ""
            val viewModel: MaestraViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(nombreNino) { viewModel.cargarPerfilNino(nombreNino) }

            PerfilNinoScreen(
                estado = viewModel.estadoPerfilNino,
                onAtras = { nav.popBackStack() },
                onAbrirChat = { nav.navigate(Route.CHAT_MAESTRA) },
                onReintentar = { viewModel.cargarPerfilNino(nombreNino) },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Route.CHAT_MAESTRA) {
            val viewModel: MaestraViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(Unit) { viewModel.cargarMensajes() }

            ChatMaestraScreen(
                estado = viewModel.estadoMensajes,
                onEnviar = { texto -> viewModel.enviarMensaje(texto) },
                onReintentar = { viewModel.cargarMensajes() },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> {}
                    }
                }
            )
        }

        // --- PADRE ---
        composable(Route.INICIO_PADRE) {
            val viewModel: PadreViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(Unit) { viewModel.cargarInicio() }

            InicioPadreScreen(
                estado = viewModel.estadoActividades,
                onReintentar = { viewModel.cargarInicio() },
                onTab = { indice ->
                    when (indice) {
                        0 -> {}
                        1 -> nav.navigate(Route.CHAT_PADRE) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Route.CHAT_PADRE) {
            val viewModel: PadreViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LaunchedEffect(Unit) { viewModel.cargarMensajes() }

            ChatPadreScreen(
                estado = viewModel.estadoMensajes,
                onEnviar = { texto -> viewModel.enviarMensaje(texto) },
                onReintentar = { viewModel.cargarMensajes() },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_PADRE) { launchSingleTop = true }
                        1 -> {}
                    }
                }
            )
        }
    }
}
