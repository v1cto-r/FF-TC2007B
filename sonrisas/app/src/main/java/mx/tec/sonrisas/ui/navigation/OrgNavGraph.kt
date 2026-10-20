package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.tec.sonrisas.ui.screens.organizacion.OrgAvisosScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgDetalleInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInicioScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgInstanciasScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgMuestra
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevaInstanciaScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgNuevoAvisoScreen
import mx.tec.sonrisas.ui.screens.organizacion.OrgReasignarAdminScreen

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Instancias, 2 = Avisos. */
private fun NavHostController.irATabOrg(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ORG_INICIO
        1 -> Route.ORG_INSTANCIAS
        else -> Route.ORG_AVISOS
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