package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.screens.organizacion.ChatOrgScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgDetalleInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInicioScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInstanciasScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgMuestra
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevaInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevoAvisoScreen
import mx.tec.sonrisas.ui.state.OrgViewModel
import mx.tec.sonrisas.ui.state.UiState

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Instancias. */
private fun NavHostController.irATabOrg(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ORG_INICIO
        else -> Route.ORG_INSTANCIAS
    }
    navigate(destino) {
        popUpTo(Route.ORG_INICIO)
        launchSingleTop = true
    }
}

fun NavGraphBuilder.orgGraph(
    nav: NavHostController,
    nombre: String,
    onSalir: () -> Unit
) {
    composable(Route.ORG_INICIO) {
        val viewModel: OrgViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.cargarInstancias() }

        OrgInicioScreen(
            estado = viewModel.estadoInstancias,
            onEnviarAviso = { nav.navigate(Route.ORG_NUEVO_AVISO) },
            onNuevaInstancia = { nav.navigate(Route.ORG_NUEVA_INSTANCIA) },
            onReintentar = { viewModel.cargarInstancias() },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(Route.ORG_INSTANCIAS) {
        val viewModel: OrgViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.cargarInstancias() }

        OrgInstanciasScreen(
            estado = viewModel.estadoInstancias,
            onInstanciaClick = { instancia -> nav.navigate(Route.orgDetalleInstancia(instancia.id)) },
            onNuevaInstancia = { nav.navigate(Route.ORG_NUEVA_INSTANCIA) },
            onReintentar = { viewModel.cargarInstancias() },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(
        route = Route.ORG_DETALLE_INSTANCIA,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val viewModel: OrgViewModel = hiltViewModel()
        LaunchedEffect(instanciaId) { viewModel.cargarInstanciaDetalle(instanciaId) }

        when (val estado = viewModel.estadoDetalleInstancia) {
            UiState.Cargando -> EstadoCargando()
            is UiState.Error -> EstadoError(
                titulo = "Error",
                mensaje = estado.mensaje,
                onReintentar = { viewModel.cargarInstanciaDetalle(instanciaId) }
            )
            UiState.Vacio -> EstadoVacio(
                titulo = "No encontrada",
                subtitulo = "La instancia no existe"
            )
            is UiState.Exito -> {
                val instancia = estado.datos
                OrgDetalleInstanciaScreen(
                    instancia = instancia,
                    onAtras = { nav.popBackStack() },
                    onEditar = { nav.navigate(Route.orgEditarInstancia(instanciaId)) },
                    onChatAdmin = { nav.navigate(Route.orgChat(instanciaId)) },
                    onCambiarEstado = {
                        viewModel.cambiarEstadoInstancia(instanciaId, instancia.activa) {
                            viewModel.cargarInstanciaDetalle(instanciaId)
                        }
                    }
                )
            }
        }
    }

    composable(
        route = Route.ORG_CHAT,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val viewModel: OrgViewModel = hiltViewModel()
        val estado = viewModel.estadoDetalleInstancia
        val instancia = (estado as? UiState.Exito)?.datos
        val adminNombre = instancia?.admin?.nombre ?: "Administrador"

        val mensajesEjemplo = listOf(
            Message(
                chatId = "admin_$instanciaId",
                authorId = "admin",
                content = "Hola, soy $adminNombre. ¿En qué podemos ayudarte desde la estancia ${instancia?.nombre ?: ""}",
                sentAt = LocalDateTime.now().minusMinutes(10)
            )
        )

        ChatOrgScreen(
            estado = UiState.Exito(mensajesEjemplo),
            currentUserId = "org",
            titulo = adminNombre,
            subtitulo = instancia?.nombre ?: "Instancia",
            onAtras = { nav.popBackStack() },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(Route.ORG_NUEVA_INSTANCIA) {
        val viewModel: OrgViewModel = hiltViewModel()
        OrgNuevaInstanciaScreen(
            contrasenaGenerada = viewModel.contrasenaTemporalGenerada,
            onAtras = { nav.popBackStack() },
            onGuardar = { nombre, direccion, telefono, capacidad, adminNombre, adminCorreo ->
                viewModel.guardarInstancia(null, nombre, direccion, telefono, capacidad, adminNombre, adminCorreo) {
                    nav.popBackStack()
                }
            },
            onCerrarDialogoContrasena = {
                viewModel.limpiarContrasenaTemporal()
                nav.popBackStack()
            }
        )
    }

    composable(
        route = Route.ORG_EDITAR_INSTANCIA,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val viewModel: OrgViewModel = hiltViewModel()
        LaunchedEffect(instanciaId) { viewModel.cargarInstanciaDetalle(instanciaId) }

        when (val estado = viewModel.estadoDetalleInstancia) {
            UiState.Cargando -> EstadoCargando()
            is UiState.Exito -> {
                val instancia = estado.datos
                OrgNuevaInstanciaScreen(
                    instanciaInicial = instancia,
                    contrasenaGenerada = viewModel.contrasenaTemporalGenerada,
                    onAtras = { nav.popBackStack() },
                    onGuardar = { nombre, direccion, telefono, capacidad, adminNombre, adminCorreo ->
                        viewModel.guardarInstancia(instanciaId, nombre, direccion, telefono, capacidad, adminNombre, adminCorreo) {
                            nav.popBackStack()
                        }
                    },
                    onCerrarDialogoContrasena = {
                        viewModel.limpiarContrasenaTemporal()
                    },
                    onResetPassword = { userId ->
                        viewModel.resetarContrasenaAdmin(userId)
                    }
                )
            }
            else -> EstadoCargando()
        }
    }

    composable(Route.ORG_NUEVO_AVISO) {
        OrgNuevoAvisoScreen(
            instancias = OrgMuestra.instancias,
            onAtras = { nav.popBackStack() },
            onEnviar = { _, _, _, idsInstancias ->
                val targetInstanciaId = idsInstancias?.firstOrNull() ?: OrgMuestra.instancias.firstOrNull()?.id ?: "i1"
                nav.popBackStack()
                nav.navigate(Route.orgChat(targetInstanciaId))
            }
        )
    }
}
