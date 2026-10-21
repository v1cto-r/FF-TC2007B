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
import mx.tec.sonrisas.ui.screens.admin.*
import mx.tec.sonrisas.ui.state.AdminViewModel
import mx.tec.sonrisas.ui.state.ChatViewModel
import mx.tec.sonrisas.ui.state.UiState

/** Pestañas de la barra inferior: 0 = Inicio, 1 = Salones, 2 = Padres, 3 = Chat. */
private fun NavHostController.irATabAdmin(indice: Int) {
    val destino = when (indice) {
        0 -> Route.ADMIN_INICIO
        1 -> Route.ADMIN_SALONES
        2 -> Route.ADMIN_PADRES
        else -> Route.ADMIN_CHAT_LIST
    }
    navigate(destino) {
        popUpTo(Route.ADMIN_INICIO)
        launchSingleTop = true
    }
}

@Composable
private fun NavBackStackEntry.adminViewModel(nav: NavHostController): AdminViewModel {
    val raiz = remember(this) { nav.getBackStackEntry(Route.ADMIN_INICIO) }
    return hiltViewModel<AdminViewModel>(raiz)
}

fun NavGraphBuilder.adminGraph(
    nav: NavHostController,
    miId: String,
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
                onMaestras = { nav.navigate(Route.ADMIN_MAESTRAS) },
                onCrearPadre = { nav.navigate(Route.ADMIN_CREAR_PADRE) },
                onMenuAlergias = { nav.navigate(Route.ADMIN_MENU_ALERGIAS) },
                onSalonClick = { salon -> nav.navigate(Route.adminDetalleSalon(salon.id)) },
                onCerrarSesion = onSalir,
                onTab = { indice -> nav.irATabAdmin(indice) }
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
                onCerrarSesion = onSalir,
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
                onCerrarSesion = onSalir,
                onTab = { indice -> nav.irATabAdmin(indice) }
            )
        }
    }

    // --- Chat Admin ---

    composable(Route.ADMIN_CHAT_LIST) { entry ->
        val vm = entry.adminViewModel(nav)
        val chat: ChatViewModel = hiltViewModel()
        LaunchedEffect(Unit) { if (vm.estado !is UiState.Exito) vm.cargar() }
        val estadoMaestros: UiState<List<MaestroUi>> = when (val e = vm.estado) {
            is UiState.Exito -> if (e.datos.maestros.isEmpty()) UiState.Vacio else UiState.Exito(e.datos.maestros)
            is UiState.Error -> e
            UiState.Vacio -> UiState.Vacio
            UiState.Cargando -> UiState.Cargando
        }
        // Chat ids: the admin's own id (org chat) and each teacher's id.
        val chatIds = listOf(miId) + ((vm.estado as? UiState.Exito)?.datos?.maestros?.map { it.id } ?: emptyList())
        LaunchedEffect(chatIds) { chat.observarUltimosMensajes(chatIds) }

        AdminChatListScreen(
            estado = estadoMaestros,
            miId = miId,
            ultimosMensajes = chat.ultimosMensajes,
            onChatOrg = { nav.navigate(Route.ADMIN_CHAT_ORG) },
            onMaestroClick = { maestro -> nav.navigate(Route.adminChatTeacher(maestro.id)) },
            onReintentar = vm::cargar,
            onCerrarSesion = onSalir,
            onTab = { indice -> nav.irATabAdmin(indice) }
        )
    }

    composable(Route.ADMIN_CHAT_ORG) {
        // Org↔admin chat: its id is the admin's own user id.
        val chat: ChatViewModel = hiltViewModel()
        LaunchedEffect(miId) { chat.cargarMensajes(miId) }
        AdminChatScreen(
            estado = chat.estadoMensajes,
            currentUserId = miId,
            titulo = "Organización",
            subtitulo = "Comunicación institucional",
            onAtras = { nav.popBackStack() },
            onEnviar = { texto, esAviso -> chat.enviarMensaje(miId, texto, esAviso) },
            onEnviarFoto = { uri -> chat.enviarFoto(miId, uri) },
            onReintentar = { chat.cargarMensajes(miId) },
            onTab = { indice -> nav.irATabAdmin(indice) }
        )
    }

    composable(
        route = Route.ADMIN_CHAT_TEACHER,
        arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
    ) { entry ->
        val teacherId = entry.arguments?.getString("teacherId").orEmpty()
        val vm = entry.adminViewModel(nav)
        val chat: ChatViewModel = hiltViewModel()
        // The chat id of a teacher↔admin chat is the teacher's user id.
        LaunchedEffect(teacherId) { chat.cargarMensajes(teacherId) }
        val maestroNombre = (vm.estado as? UiState.Exito)?.datos?.maestros?.firstOrNull { it.id == teacherId }?.nombre ?: "Profesor/a"

        AdminChatScreen(
            estado = chat.estadoMensajes,
            currentUserId = miId,
            titulo = maestroNombre,
            subtitulo = "Docente",
            onAtras = { nav.popBackStack() },
            onEnviar = { texto, esAviso -> chat.enviarMensaje(teacherId, texto, esAviso) },
            onEnviarFoto = { uri -> chat.enviarFoto(teacherId, uri) },
            onReintentar = { chat.cargarMensajes(teacherId) },
            onTab = { indice -> nav.irATabAdmin(indice) }
        )
    }

    composable(Route.ADMIN_MENU_ALERGIAS) { entry ->
        val vm = entry.adminViewModel(nav)
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { _ ->
            AdminMenuAlergiasScreen(
                platillos = vm.platillos,
                analisis = vm.analisisMenu,
                onAtras = { nav.popBackStack() },
                onAgregarPlatillo = vm::agregarPlatillo,
                onQuitarPlatillo = vm::quitarPlatillo,
                onNombrePlatillo = vm::cambiarNombrePlatillo,
                onAgregarIngrediente = vm::agregarIngrediente,
                onQuitarIngrediente = vm::quitarIngrediente,
                onAnalizar = vm::analizarMenu
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

    composable(Route.ADMIN_MAESTRAS) { entry ->
        val vm = entry.adminViewModel(nav)
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            AdminMaestrasScreen(
                maestros = datos.maestros,
                salones = datos.salones,
                guardando = vm.guardando,
                errorMensaje = vm.errorMensaje,
                onAtras = { nav.popBackStack() },
                onNuevaMaestra = { nav.navigate(Route.ADMIN_CREAR_MAESTRA) },
                onEliminar = { maestro -> vm.eliminarMaestra(maestro.id) }
            )
        }
    }

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
        LaunchedEffect(Unit) { vm.limpiarError() }
        ConEstado(estado = vm.estado, onReintentar = vm::cargar) { datos ->
            val padre = datos.padres.firstOrNull { it.id == padreId }
            if (padre != null) {
                AdminDetallePadreScreen(
                    padre = padre,
                    guardando = vm.guardando,
                    errorMensaje = vm.errorMensaje,
                    onAtras = { nav.popBackStack() },
                    onAgregarNino = { nav.navigate(Route.adminAgregarNino(padreId)) },
                    onEliminarNino = { hijo -> vm.eliminarNino(hijo.id) },
                    // Once deleted the padre leaves the list, and the branch below pops back
                    onEliminarPadre = { vm.eliminarPadre(padreId) }
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
}
