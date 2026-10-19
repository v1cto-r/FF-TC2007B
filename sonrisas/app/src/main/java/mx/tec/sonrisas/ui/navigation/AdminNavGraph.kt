package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.tec.sonrisas.data.RolApi
import mx.tec.sonrisas.ui.screens.admin.AdminAgregarAlumnosScreen
import mx.tec.sonrisas.ui.screens.admin.AdminAgregarNinoScreen
import mx.tec.sonrisas.ui.screens.admin.AdminAsignarMaestraScreen
import mx.tec.sonrisas.ui.screens.admin.AdminAvisosScreen
import mx.tec.sonrisas.ui.screens.admin.AdminCredencialDialog
import mx.tec.sonrisas.ui.screens.admin.AdminCrearGrupoScreen
import mx.tec.sonrisas.ui.screens.admin.AdminCrearUsuarioScreen
import mx.tec.sonrisas.ui.screens.admin.AdminDetallePadreScreen
import mx.tec.sonrisas.ui.screens.admin.AdminDetalleSalonScreen
import mx.tec.sonrisas.ui.screens.admin.AdminInicioScreen
import mx.tec.sonrisas.ui.screens.admin.AdminInscripcionScreen
import mx.tec.sonrisas.ui.screens.admin.AdminNuevoAvisoScreen
import mx.tec.sonrisas.ui.screens.admin.AdminPadresScreen
import mx.tec.sonrisas.ui.screens.admin.AdminSalonesScreen
import mx.tec.sonrisas.ui.screens.admin.ConEstado
import mx.tec.sonrisas.ui.state.AdminViewModel

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Salones, 2 = Padres, 3 = Avisos. */
private fun NavHostController.irATabAdmin(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ADMIN_INICIO
        1 -> Route.ADMIN_SALONES
        2 -> Route.ADMIN_PADRES
        else -> Route.ADMIN_AVISOS
    }
    navigate(destino) {
        popUpTo(Route.ADMIN_INICIO)
        launchSingleTop = true
    }
}

/** Un solo AdminViewModel para todo el flujo del admin, atado a la pantalla de Inicio. */
@Composable
private fun NavBackStackEntry.adminViewModel(nav: NavHostController): AdminViewModel {
    val raiz = remember(this) { nav.getBackStackEntry(Route.ADMIN_INICIO) }
    return hiltViewModel<AdminViewModel>(raiz)
}

fun NavGraphBuilder.adminGraph(
    nav: NavHostController,
    onSalir: () -> Unit
) {
    // --- Pestañas ---

    composable(Route.ADMIN_INICIO) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.cargar() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar, onSalir = onSalir) { datos ->
            AdminInicioScreen(
                salones = datos.salones,
                alumnosSinSalon = datos.alumnosSinSalon.size,
                onCrearGrupo = { nav.navigate(Route.ADMIN_CREAR_GRUPO) },
                onInscripcion = { nav.navigate(Route.ADMIN_INSCRIPCION) },
                onCrearMaestra = { nav.navigate(Route.ADMIN_CREAR_MAESTRA) },
                onCrearPadre = { nav.navigate(Route.ADMIN_CREAR_PADRE) },
                onSalonClick = { salon -> nav.navigate(Route.adminDetalleSalon(salon.id)) },
                onTab = { indice -> nav.irATabAdmin(indice) },
                onSalir = onSalir
            )
        }
    }

    composable(Route.ADMIN_SALONES) { entry ->
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminSalonesScreen(
                salones = datos.salones,
                onSalonClick = { salon -> nav.navigate(Route.adminDetalleSalon(salon.id)) },
                onCrearGrupo = { nav.navigate(Route.ADMIN_CREAR_GRUPO) },
                onTab = { indice -> nav.irATabAdmin(indice) }
            )
        }
    }

    composable(Route.ADMIN_PADRES) { entry ->
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminPadresScreen(
                padres = datos.padres,
                onPadreClick = { padre -> nav.navigate(Route.adminDetallePadre(padre.id)) },
                onNuevoPadre = { nav.navigate(Route.ADMIN_CREAR_PADRE) },
                onTab = { indice -> nav.irATabAdmin(indice) }
            )
        }
    }

    composable(Route.ADMIN_AVISOS) { entry ->
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminAvisosScreen(
                avisos = datos.avisos,
                onNuevoAviso = { nav.navigate(Route.ADMIN_NUEVO_AVISO) },
                onTab = { indice -> nav.irATabAdmin(indice) }
            )
        }
    }

    // --- Salones ---

    composable(
        route = Route.ADMIN_DETALLE_SALON,
        arguments = listOf(navArgument("salonId") { type = NavType.StringType })
    ) { entry ->
        val salonId = entry.arguments?.getString("salonId").orEmpty()
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            val salon = datos.salones.firstOrNull { it.id == salonId }
            if (salon != null) {
                AdminDetalleSalonScreen(
                    salon = salon,
                    onAtras = { nav.popBackStack() },
                    onAgregarAlumnos = { nav.navigate(Route.adminAgregarAlumnos(salonId)) },
                    onCambiarMaestra = { nav.navigate(Route.adminAsignarMaestra(salonId)) }
                )
            } else {
                LaunchedEffect(Unit) { nav.popBackStack() }
            }
        }
    }

    composable(Route.ADMIN_CREAR_GRUPO) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminCrearGrupoScreen(
                maestros = datos.maestros,
                alumnosDisponibles = datos.alumnosSinSalon,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onCrear = { nombre, maestroId, alumnoIds ->
                    vm.crearGrupo(nombre, maestroId, alumnoIds) { nav.popBackStack() }
                }
            )
        }
    }

    composable(
        route = Route.ADMIN_AGREGAR_ALUMNOS,
        arguments = listOf(navArgument("salonId") { type = NavType.StringType })
    ) { entry ->
        val salonId = entry.arguments?.getString("salonId").orEmpty()
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminAgregarAlumnosScreen(
                nombreSalon = datos.salones.firstOrNull { it.id == salonId }?.nombre.orEmpty(),
                alumnosDisponibles = datos.alumnosSinSalon,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onAgregar = { ids -> vm.agregarAlumnos(salonId, ids) { nav.popBackStack() } }
            )
        }
    }

    composable(
        route = Route.ADMIN_ASIGNAR_MAESTRA,
        arguments = listOf(navArgument("salonId") { type = NavType.StringType })
    ) { entry ->
        val salonId = entry.arguments?.getString("salonId").orEmpty()
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            val salon = datos.salones.firstOrNull { it.id == salonId }
            AdminAsignarMaestraScreen(
                nombreSalon = salon?.nombre.orEmpty(),
                maestros = datos.maestros,
                maestroActualId = salon?.maestro?.id,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onGuardar = { maestroId -> vm.asignarMaestra(salonId, maestroId) { nav.popBackStack() } }
            )
        }
    }

    // --- Cuentas ---

    composable(Route.ADMIN_CREAR_MAESTRA) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        AdminCrearUsuarioScreen(
            titulo = "Crear maestra",
            subtitulo = "Nueva cuenta de docente",
            guardando = vm.guardando,
            errorMensaje = vm.errorMensaje,
            onAtras = { nav.popBackStack() },
            onCrear = { nombre, apellido, correo -> vm.crearUsuario(nombre, apellido, correo, RolApi.DOCENTE) }
        )
        vm.credencial?.let { cuenta ->
            AdminCredencialDialog(credencial = cuenta, titulo = "Maestra creada") {
                vm.descartarCredencial()
                nav.popBackStack()
            }
        }
    }

    composable(Route.ADMIN_CREAR_PADRE) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        AdminCrearUsuarioScreen(
            titulo = "Crear padre",
            subtitulo = "Nueva cuenta de padre o tutor",
            guardando = vm.guardando,
            errorMensaje = vm.errorMensaje,
            onAtras = { nav.popBackStack() },
            onCrear = { nombre, apellido, correo -> vm.crearUsuario(nombre, apellido, correo, RolApi.PADRE) }
        )
        vm.credencial?.let { cuenta ->
            AdminCredencialDialog(credencial = cuenta, titulo = "Padre creado") {
                vm.descartarCredencial()
                nav.popBackStack()
            }
        }
    }

    composable(Route.ADMIN_INSCRIPCION) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        AdminInscripcionScreen(
            guardando = vm.guardando,
            errorMensaje = vm.errorMensaje,
            onAtras = { nav.popBackStack() },
            onIniciar = { nombre, apellido, correo, alumno -> vm.inscribir(nombre, apellido, correo, alumno) }
        )
        vm.credencial?.let { cuenta ->
            AdminCredencialDialog(credencial = cuenta, titulo = "Inscripción creada") {
                vm.descartarCredencial()
                nav.popBackStack()
            }
        }
    }

    // --- Padres ---

    composable(
        route = Route.ADMIN_DETALLE_PADRE,
        arguments = listOf(navArgument("padreId") { type = NavType.StringType })
    ) { entry ->
        val padreId = entry.arguments?.getString("padreId").orEmpty()
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            val padre = datos.padres.firstOrNull { it.id == padreId }
            if (padre != null) {
                AdminDetallePadreScreen(
                    padre = padre,
                    onAtras = { nav.popBackStack() },
                    onAgregarNino = { nav.navigate(Route.adminAgregarNino(padreId)) }
                )
            } else {
                LaunchedEffect(Unit) { nav.popBackStack() }
            }
        }
    }

    composable(
        route = Route.ADMIN_AGREGAR_NINO,
        arguments = listOf(navArgument("padreId") { type = NavType.StringType })
    ) { entry ->
        val padreId = entry.arguments?.getString("padreId").orEmpty()
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminAgregarNinoScreen(
                nombrePadre = datos.padres.firstOrNull { it.id == padreId }?.nombre.orEmpty(),
                salones = datos.salones,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onGuardar = { nombre, salonId -> vm.agregarNino(padreId, nombre, salonId) { nav.popBackStack() } }
            )
        }
    }

    // --- Avisos ---

    composable(Route.ADMIN_NUEVO_AVISO) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminNuevoAvisoScreen(
                salones = datos.salones,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onEnviar = { titulo, mensaje, salonId ->
                    vm.enviarAviso(titulo, mensaje, salonId) { nav.popBackStack() }
                }
            )
        }
    }
}