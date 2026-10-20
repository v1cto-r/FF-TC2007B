package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.screens.organizacion.ChatOrgScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgChatListScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgDetalleInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInicioScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInstanciasScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevaInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevoAvisoScreen
import mx.tec.sonrisas.ui.state.ChatViewModel
import mx.tec.sonrisas.ui.state.OrgViewModel
import mx.tec.sonrisas.ui.state.UiState

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Estancias, 2 = Chat. */
private fun NavHostController.irATabOrg(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ORG_INICIO
        1 -> Route.ORG_INSTANCIAS
        else -> Route.ORG_CHAT_LIST
    }
    navigate(destino) {
        popUpTo(Route.ORG_INICIO)
        launchSingleTop = true
    }
}

fun NavGraphBuilder.orgGraph(
    nav: NavHostController,
    nombre: String,
    miId: String,
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
            onCerrarSesion = onSalir,
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
            onCerrarSesion = onSalir,
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
                    onChatAdmin = { instancia.admin?.let { nav.navigate(Route.orgChatAdmin(it.id)) } },
                    onCambiarEstado = {
                        viewModel.cambiarEstadoInstancia(instanciaId, instancia.activa) {
                            viewModel.cargarInstanciaDetalle(instanciaId)
                        }
                    }
                )
            }
        }
    }

    composable(Route.ORG_CHAT_LIST) {
        val viewModel: OrgViewModel = hiltViewModel()
        val chat: ChatViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.cargarInstancias() }
        // Chat ids: each estancia's admin user id.
        val adminIds = (viewModel.estadoInstancias as? UiState.Exito)?.datos?.mapNotNull { it.admin?.id } ?: emptyList()
        LaunchedEffect(adminIds) { chat.observarUltimosMensajes(adminIds) }

        OrgChatListScreen(
            estado = viewModel.estadoInstancias,
            ultimosMensajes = chat.ultimosMensajes,
            onInstanciaClick = { adminId -> nav.navigate(Route.orgChatAdmin(adminId)) },
            onReintentar = { viewModel.cargarInstancias() },
            onCerrarSesion = onSalir,
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(
        route = Route.ORG_CHAT_ADMIN,
        arguments = listOf(navArgument("adminId") { type = NavType.StringType })
    ) { entry ->
        val adminId = entry.arguments?.getString("adminId").orEmpty()
        val viewModel: OrgViewModel = hiltViewModel()
        val chat: ChatViewModel = hiltViewModel()
        LaunchedEffect(adminId) {
            viewModel.cargarInstancias()
            chat.cargarMensajes(adminId)
        }
        val instancia = (viewModel.estadoInstancias as? UiState.Exito)?.datos?.firstOrNull { it.admin?.id == adminId }

        ChatOrgScreen(
            estado = chat.estadoMensajes,
            currentUserId = miId,
            titulo = instancia?.nombre ?: "Instancia",
            subtitulo = instancia?.admin?.nombre ?: "Administrador",
            onAtras = { nav.popBackStack() },
            onEnviar = { texto, esAviso -> chat.enviarMensaje(adminId, texto, esAviso) },
            onEnviarFoto = { uri -> chat.enviarFoto(adminId, uri) },
            onReintentar = { chat.cargarMensajes(adminId) },
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
        val viewModel: OrgViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.cargarInstancias() }
        val instancias = (viewModel.estadoInstancias as? UiState.Exito)?.datos ?: emptyList()

        OrgNuevoAvisoScreen(
            instancias = instancias,
            onAtras = { nav.popBackStack() },
            onEnviar = { titulo, mensaje, idsInstancias ->
                viewModel.guardarAviso(titulo, mensaje, idsInstancias) {
                    nav.popBackStack()
                }
            }
        )
    }
}
