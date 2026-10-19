package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.tec.sonrisas.ui.screens.organizacion.*
import mx.tec.sonrisas.ui.state.AdminViewModel

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Instancias, 2 = Avisos, 3 = Chat. */
private fun NavHostController.irATabOrg(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ORG_INICIO
        1 -> Route.ORG_INSTANCIAS
        2 -> Route.ORG_AVISOS
        else -> Route.ORG_CHAT_ADMIN
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
        OrgInicioScreen(
            instancias = OrgMuestra.instancias,
            nombre = nombre,
            onEnviarAviso = { nav.navigate(Route.ORG_NUEVO_AVISO) },
            onNuevaInstancia = { nav.navigate(Route.ORG_NUEVA_INSTANCIA) },
            onTab = { indice -> nav.irATabOrg(indice) },
            onSalir = onSalir
        )
    }

    composable(Route.ORG_INSTANCIAS) {
        OrgInstanciasScreen(
            instancias = OrgMuestra.instancias,
            onInstanciaClick = { instancia -> nav.navigate(Route.orgDetalleInstancia(instancia.id)) },
            onNuevaInstancia = { nav.navigate(Route.ORG_NUEVA_INSTANCIA) },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(Route.ORG_AVISOS) {
        OrgAvisosScreen(
            avisos = OrgMuestra.avisos,
            onNuevoAviso = { nav.navigate(Route.ORG_NUEVO_AVISO) },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(Route.ORG_CHAT_ADMIN) {
        val vm: AdminViewModel = hiltViewModel()
        val context = LocalContext.current
        val chatId = "admin_org"
        LaunchedEffect(Unit) { vm.cargarMensajes(chatId) }
        OrgChatScreen(
            estado = vm.estadoMensajes,
            currentUserId = "org",
            onEnviar = { texto -> vm.enviarMensaje(chatId, texto) },
            onEnviarFoto = { uri -> vm.enviarFoto(chatId, uri, context) },
            onToggleImportante = { msg -> vm.toggleImportant(msg.id) },
            onEliminar = { msg -> vm.eliminarMensaje(msg) },
            onReintentar = { vm.cargarMensajes(chatId) },
            onTab = { indice -> nav.irATabOrg(indice) }
        )
    }

    composable(
        route = Route.ORG_DETALLE_INSTANCIA,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val instancia = OrgMuestra.instancias.firstOrNull { it.id == instanciaId }
        if (instancia != null) {
            OrgDetalleInstanciaScreen(
                instancia = instancia,
                onAtras = { nav.popBackStack() },
                onEditar = { nav.navigate(Route.orgEditarInstancia(instanciaId)) },
                onReasignarAdmin = { nav.navigate(Route.orgReasignarAdmin(instanciaId)) },
                onCambiarEstado = { }
            )
        } else {
            LaunchedEffect(Unit) { nav.popBackStack() }
        }
    }

    composable(Route.ORG_NUEVA_INSTANCIA) {
        OrgNuevaInstanciaScreen(
            onAtras = { nav.popBackStack() },
            onGuardar = { nav.popBackStack() }
        )
    }

    composable(
        route = Route.ORG_EDITAR_INSTANCIA,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val instancia = OrgMuestra.instancias.firstOrNull { it.id == instanciaId }
        if (instancia != null) {
            OrgNuevaInstanciaScreen(
                instanciaInicial = instancia,
                onAtras = { nav.popBackStack() },
                onGuardar = { nav.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) { nav.popBackStack() }
        }
    }

    composable(
        route = Route.ORG_REASIGNAR_ADMIN,
        arguments = listOf(navArgument("instanciaId") { type = NavType.StringType })
    ) { entry ->
        val instanciaId = entry.arguments?.getString("instanciaId").orEmpty()
        val instancia = OrgMuestra.instancias.firstOrNull { it.id == instanciaId }
        if (instancia != null) {
            OrgReasignarAdminScreen(
                instancia = instancia,
                onAtras = { nav.popBackStack() },
                onGuardar = { nav.popBackStack() }
            )
        } else {
            LaunchedEffect(Unit) { nav.popBackStack() }
        }
    }

    composable(Route.ORG_NUEVO_AVISO) {
        OrgNuevoAvisoScreen(
            instancias = OrgMuestra.instancias,
            onAtras = { nav.popBackStack() },
            onEnviar = { _, _, _, _ -> nav.popBackStack() }
        )
    }
}
