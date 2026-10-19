package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.tec.sonrisas.ui.screens.admin.AdminAgregarAlumnosScreen
import mx.tec.sonrisas.ui.screens.admin.AdminCrearGrupoScreen
import mx.tec.sonrisas.ui.screens.admin.AdminDetalleSalonScreen
import mx.tec.sonrisas.ui.screens.admin.AdminInicioScreen
import mx.tec.sonrisas.ui.screens.admin.AdminInscripcionScreen
import mx.tec.sonrisas.ui.screens.admin.AdminMuestra
import mx.tec.sonrisas.ui.screens.admin.AdminSalonesScreen

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Salones, 2 = Inscripción. */
private fun NavHostController.irATabAdmin(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ADMIN_INICIO
        1 -> Route.ADMIN_SALONES
        else -> Route.ADMIN_INSCRIPCION
    }
    navigate(destino) {
        popUpTo(Route.ADMIN_INICIO)
        launchSingleTop = true
    }
}

fun NavGraphBuilder.adminGraph(
    nav: NavHostController,
    onSalir: () -> Unit
) {
    composable(Route.ADMIN_INICIO) {
        AdminInicioScreen(
            salones = AdminMuestra.salones,
            alumnosSinSalon = AdminMuestra.alumnosSinSalon.size,
            onCrearGrupo = { nav.navigate(Route.ADMIN_CREAR_GRUPO) },
            onInscripcion = { nav.irATabAdmin(2) },
            onSalonClick = { salon -> nav.navigate(Route.adminDetalleSalon(salon.id)) },
            onTab = { indice -> nav.irATabAdmin(indice) },
            onSalir = onSalir
        )
    }

    composable(Route.ADMIN_SALONES) {
        AdminSalonesScreen(
            salones = AdminMuestra.salones,
            onSalonClick = { salon -> nav.navigate(Route.adminDetalleSalon(salon.id)) },
            onCrearGrupo = { nav.navigate(Route.ADMIN_CREAR_GRUPO) },
            onTab = { indice -> nav.irATabAdmin(indice) }
        )
    }

    composable(Route.ADMIN_INSCRIPCION) {
        AdminInscripcionScreen(
            onIniciar = { nav.irATabAdmin(0) },
            onTab = { indice -> nav.irATabAdmin(indice) }
        )
    }

    composable(
        route = Route.ADMIN_DETALLE_SALON,
        arguments = listOf(navArgument("salonId") { type = NavType.StringType })
    ) { entry ->
        val salonId = entry.arguments?.getString("salonId").orEmpty()
        val salon = AdminMuestra.salones.firstOrNull { it.id == salonId }
        if (salon != null) {
            AdminDetalleSalonScreen(
                salon = salon,
                onAtras = { nav.popBackStack() },
                onAgregarAlumnos = { nav.navigate(Route.adminAgregarAlumnos(salonId)) }
            )
        } else {
            LaunchedEffect(Unit) { nav.popBackStack() }
        }
    }

    composable(Route.ADMIN_CREAR_GRUPO) {
        AdminCrearGrupoScreen(
            maestros = AdminMuestra.maestros,
            alumnosDisponibles = AdminMuestra.alumnosSinSalon,
            onAtras = { nav.popBackStack() },
            onCrear = { nav.popBackStack() }
        )
    }

    composable(
        route = Route.ADMIN_AGREGAR_ALUMNOS,
        arguments = listOf(navArgument("salonId") { type = NavType.StringType })
    ) { entry ->
        val salonId = entry.arguments?.getString("salonId").orEmpty()
        val salon = AdminMuestra.salones.firstOrNull { it.id == salonId }
        AdminAgregarAlumnosScreen(
            nombreSalon = salon?.nombre.orEmpty(),
            alumnosDisponibles = AdminMuestra.alumnosSinSalon,
            onAtras = { nav.popBackStack() },
            onAgregar = { nav.popBackStack() }
        )
    }
}