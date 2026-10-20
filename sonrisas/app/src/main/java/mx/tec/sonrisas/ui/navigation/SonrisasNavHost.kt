package mx.tec.sonrisas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion
import mx.tec.sonrisas.domain.WeeklySummary
import mx.tec.sonrisas.ui.screens.maestra.ActivityFormScreen
import mx.tec.sonrisas.ui.screens.maestra.BroadcastFormScreen
import mx.tec.sonrisas.ui.screens.maestra.ChatListScreen
import mx.tec.sonrisas.ui.screens.maestra.ChatMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.CheckInFormScreen
import mx.tec.sonrisas.ui.screens.maestra.CheckOutFormScreen
import mx.tec.sonrisas.ui.screens.maestra.InicioMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.NinosScreen
import mx.tec.sonrisas.ui.screens.maestra.PerfilNinoScreen
import mx.tec.sonrisas.ui.screens.maestra.StayFormScreen
import mx.tec.sonrisas.ui.screens.padre.CambiarContrasenaScreen
import mx.tec.sonrisas.ui.screens.padre.ChatPadreScreen
import mx.tec.sonrisas.ui.screens.padre.InicioPadreScreen
import mx.tec.sonrisas.ui.screens.padre.OnboardingHijoScreen
import mx.tec.sonrisas.ui.screens.padre.PadreAutorizarTerceroScreen
import mx.tec.sonrisas.ui.screens.padre.PadreBitacoraScreen
import mx.tec.sonrisas.ui.screens.padre.PadrePerfilNinoScreen
import mx.tec.sonrisas.ui.screens.padre.PadreResumenSemanalScreen
import mx.tec.sonrisas.ui.state.ActivityViewModel
import mx.tec.sonrisas.ui.state.CheckInViewModel
import mx.tec.sonrisas.ui.state.CheckOutViewModel
import mx.tec.sonrisas.ui.state.ChatViewModel
import mx.tec.sonrisas.ui.state.MaestraViewModel
import mx.tec.sonrisas.ui.state.PadreViewModel
import mx.tec.sonrisas.ui.state.StayViewModel
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun SonrisasNavHost(
    sesion: Sesion,
    chatPendiente: String?,
    onChatAbierto: () -> Unit,
    onSalir: () -> Unit
) {
    val nav = rememberNavController()
    val destinoInicial = when (sesion.rol) {
        Rol.MAESTRA -> Route.INICIO_MAESTRA
        Rol.PADRE -> Route.INICIO_PADRE
        Rol.ADMIN -> Route.ADMIN_INICIO
        Rol.ORG -> Route.ORG_INICIO
    }

    // A tapped push notification carries a chat id. Your own id is your chat with the level above
    // (teacher → admin, admin → org); a teacher's id is an admin↔teacher chat; ORG only receives admin ids;
    // anything else is a child chat.
    LaunchedEffect(chatPendiente) {
        chatPendiente?.let {
            nav.navigate(
                when (sesion.rol) {
                    Rol.MAESTRA -> if (it == sesion.id) Route.CHAT_ADMIN_MAESTRA else Route.chatMaestra(it)
                    Rol.ADMIN -> if (it == sesion.id) Route.ADMIN_CHAT_ORG else Route.adminChatTeacher(it)
                    Rol.ORG -> Route.orgChatAdmin(it)
                    else -> Route.chatPadre(it)
                }
            )
            onChatAbierto()
        }
    }

    NavHost(
        navController = nav,
        startDestination = destinoInicial
    ) {
        adminGraph(nav = nav, miId = sesion.id, onSalir = onSalir)
        orgGraph(nav = nav, nombre = sesion.nombre, miId = sesion.id, onSalir = onSalir)

        // --- MAESTRA ---
        composable(Route.INICIO_MAESTRA) {
            val viewModel: MaestraViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarInicio() }

            InicioMaestraScreen(
                estado = viewModel.estadoEventos,
                presentes = viewModel.presentes,
                total = viewModel.total,
                grupo = viewModel.grupo,
                onActividad = { nav.navigate(Route.ACTIVIDAD_MASIVA) },
                onAviso = { nav.navigate(Route.AVISO_MASIVO) },
                onReintentar = { viewModel.cargarInicio() },
                onTab = { indice ->
                    when (indice) {
                        0 -> {}
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_LISTA_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Route.NINOS) {
            val viewModel: MaestraViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarNinos() }

            NinosScreen(
                estado = viewModel.estadoNinos,
                onNinoClick = { nino ->
                    nav.navigate(Route.perfilNino(nino.id))
                },
                onReintentar = { viewModel.cargarNinos() },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> {}
                        2 -> nav.navigate(Route.CHAT_LISTA_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(
            route = Route.PERFIL_NINO,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: MaestraViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargarPerfilNino(ninoId) }

            PerfilNinoScreen(
                estado = viewModel.estadoPerfilNino,
                onAtras = { nav.popBackStack() },
                onAbrirChat = { nav.navigate(Route.chatMaestra(ninoId)) },
                onCheckIn = { nav.navigate(Route.checkIn(ninoId)) },
                onStay = { nav.navigate(Route.stay(ninoId)) },
                onActividad = { nav.navigate(Route.activity(ninoId)) },
                onCheckOut = { nav.navigate(Route.checkOut(ninoId)) },
                onCancelarSalida = { viewModel.cancelarSalida(ninoId) { viewModel.cargarPerfilNino(ninoId) } },
                onReintentar = { viewModel.cargarPerfilNino(ninoId) },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_LISTA_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Route.CHAT_LISTA_MAESTRA) {
            val viewModel: MaestraViewModel = hiltViewModel()
            val chat: ChatViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarConversaciones() }
            // The teacher↔admin chat id is the teacher's own user id.
            LaunchedEffect(sesion.id) { chat.observarUltimosMensajes(listOf(sesion.id)) }

            ChatListScreen(
                estado = viewModel.estadoConversaciones,
                ultimoMensajeDireccion = chat.ultimosMensajes[sesion.id],
                onChatDireccion = { nav.navigate(Route.CHAT_ADMIN_MAESTRA) },
                onConversacionClick = { child -> nav.navigate(Route.chatMaestra(child.id)) },
                onReintentar = { viewModel.cargarConversaciones() },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> {}
                    }
                }
            )
        }

        composable(Route.CHAT_ADMIN_MAESTRA) {
            // Teacher↔admin chat: its id is the teacher's own user id. Nothing can be deleted.
            val chat: ChatViewModel = hiltViewModel()
            val miId = sesion.id
            LaunchedEffect(miId) { chat.cargarMensajes(miId) }

            ChatMaestraScreen(
                estado = chat.estadoMensajes,
                currentUserId = miId,
                titulo = "Dirección",
                subtitulo = "Administración de la estancia",
                onAtras = { nav.popBackStack() },
                onEnviar = { texto, esAviso -> chat.enviarMensaje(miId, texto, esAviso) },
                onEnviarFoto = { uri -> chat.enviarFoto(miId, uri) },
                onEliminar = null,
                onReintentar = { chat.cargarMensajes(miId) },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_LISTA_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(
            route = Route.CHAT_MAESTRA,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: MaestraViewModel = hiltViewModel()
            LaunchedEffect(ninoId) {
                viewModel.cargarPerfilNino(ninoId)
                viewModel.cargarMensajes(ninoId)
            }
            val nino = (viewModel.estadoPerfilNino as? UiState.Exito)?.datos?.child

            ChatMaestraScreen(
                estado = viewModel.estadoMensajes,
                currentUserId = sesion.id,
                titulo = if (nino != null) "${nino.parentName} - ${nino.fullName.substringBefore(" ")}" else "Chat",
                subtitulo = nino?.fullName,
                onAtras = { nav.popBackStack() },
                onEnviar = { texto, esAviso -> viewModel.enviarMensaje(ninoId, texto, esAviso) },
                onEnviarFoto = { uri -> viewModel.enviarFoto(ninoId, uri) },
                onReintentar = { viewModel.cargarMensajes(ninoId) },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_MAESTRA) { launchSingleTop = true }
                        1 -> nav.navigate(Route.NINOS) { launchSingleTop = true }
                        2 -> nav.navigate(Route.CHAT_LISTA_MAESTRA) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(
            route = Route.CHECK_IN,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: CheckInViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargar(ninoId) }

            CheckInFormScreen(
                nombreMaestra = sesion.nombre,
                checkInExistente = (viewModel.estado as? UiState.Exito)?.datos?.checkIn,
                onAtras = { nav.popBackStack() },
                onGuardar = { checkIn -> viewModel.guardar(ninoId, checkIn) { nav.popBackStack() } }
            )
        }

        composable(
            route = Route.STAY,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: StayViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargar(ninoId) }

            StayFormScreen(
                stayExistente = (viewModel.estado as? UiState.Exito)?.datos?.stay,
                hayIngreso = viewModel.hayIngreso,
                onAtras = { nav.popBackStack() },
                onGuardar = { stay -> viewModel.guardar(ninoId, stay) { nav.popBackStack() } },
                onIrACheckIn = { nav.navigate(Route.checkIn(ninoId)) { popUpTo(Route.STAY) { inclusive = true } } }
            )
        }

        composable(
            route = Route.ACTIVITY,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: ActivityViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargar(ninoId) }

            ActivityFormScreen(
                hayIngreso = viewModel.hayIngreso,
                onAtras = { nav.popBackStack() },
                onGuardar = { nombre -> viewModel.guardar(ninoId, nombre) { nav.popBackStack() } },
                onIrACheckIn = { nav.navigate(Route.checkIn(ninoId)) { popUpTo(Route.ACTIVITY) { inclusive = true } } }
            )
        }

        composable(
            route = Route.CHECK_OUT,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: CheckOutViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargar(ninoId) }

            CheckOutFormScreen(
                nombreMaestra = sesion.nombre,
                autorizacionHoy = viewModel.autorizacionHoy,
                onAtras = { nav.popBackStack() },
                onGuardar = { checkOut -> viewModel.guardar(ninoId, checkOut) { nav.popBackStack() } }
            )
        }

        composable(Route.ACTIVIDAD_MASIVA) {
            val viewModel: MaestraViewModel = hiltViewModel()
            BroadcastFormScreen(
                titulo = "Actividad para todos",
                descripcionAyuda = "Se enviará como mensaje de chat a los padres de todos los niños presentes hoy.",
                textoBoton = "Enviar a presentes",
                onAtras = { nav.popBackStack() },
                onEnviar = { titulo, mensaje -> viewModel.enviarActividadATodos(titulo, mensaje) { nav.popBackStack() } }
            )
        }

        composable(Route.AVISO_MASIVO) {
            val viewModel: MaestraViewModel = hiltViewModel()
            BroadcastFormScreen(
                titulo = "Aviso General",
                descripcionAyuda = "Se enviará como aviso a los padres de TODOS los niños, presentes o no.",
                textoBoton = "Enviar a todos",
                onAtras = { nav.popBackStack() },
                onEnviar = { titulo, mensaje -> viewModel.enviarAvisoATodos(titulo, mensaje) { nav.popBackStack() } }
            )
        }

        // --- PADRE ONBOARDING & RUTAS ---
        composable(Route.CAMBIAR_CONTRASENA) {
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarInicio() }

            CambiarContrasenaScreen(
                onGuardarContrasena = { _ ->
                    val primerHijoId = viewModel.ninos.firstOrNull()?.id ?: ""
                    if (primerHijoId.isNotBlank()) {
                        nav.navigate(Route.onboardingHijo(primerHijoId)) {
                            popUpTo(Route.CAMBIAR_CONTRASENA) { inclusive = true }
                        }
                    } else {
                        nav.navigate(Route.INICIO_PADRE) {
                            popUpTo(Route.CAMBIAR_CONTRASENA) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Route.ONBOARDING_HIJO,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarInicio() }
            val nino = viewModel.ninos.firstOrNull { it.id == ninoId } ?: Child(id = ninoId, fullName = "Alumno", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = sesion.nombre)

            OnboardingHijoScreen(
                child = nino,
                onGuardarInformacion = { birthdate, allergies, actaUri ->
                    viewModel.actualizarDatosHijo(ninoId, birthdate, allergies, actaUri) {
                        nav.navigate(Route.INICIO_PADRE) {
                            popUpTo(Route.ONBOARDING_HIJO) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.INICIO_PADRE) {
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarInicio() }

            val hijoIncompleto = viewModel.ninos.firstOrNull { !it.profileCompleted }
            LaunchedEffect(hijoIncompleto) {
                if (hijoIncompleto != null) {
                    nav.navigate(Route.onboardingHijo(hijoIncompleto.id)) {
                        popUpTo(Route.INICIO_PADRE) { inclusive = true }
                    }
                }
            }

            InicioPadreScreen(
                estado = viewModel.estadoActividades,
                nombrePadre = sesion.nombre,
                ninos = viewModel.ninos.map { it.fullName.substringBefore(" ") },
                ninoSeleccionado = viewModel.ninoSeleccionadoIndex,
                autorizacionHoy = viewModel.autorizacionHoy,
                onSeleccionarNino = { viewModel.seleccionarNino(it) },
                onAbrirPerfilNino = { indice ->
                    viewModel.ninos.getOrNull(indice)?.id?.let { id ->
                        nav.navigate(Route.padrePerfilNino(id))
                    }
                },
                onAutorizarTercero = {
                    viewModel.ninoActualId?.let { id ->
                        nav.navigate(Route.padreAutorizarTercero(id))
                    }
                },
                onReintentar = { viewModel.cargarInicio() },
                onTab = { indice ->
                    when (indice) {
                        0 -> {}
                        1 -> nav.navigate(Route.BITACORA_PADRE) { launchSingleTop = true }
                        2 -> viewModel.ninoActualId?.let { nav.navigate(Route.chatPadre(it)) { launchSingleTop = true } }
                    }
                }
            )
        }

        composable(Route.BITACORA_PADRE) {
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                viewModel.cargarInicio()
            }
            LaunchedEffect(viewModel.ninoActualId) {
                viewModel.ninoActualId?.let { viewModel.cargarResumenSemanal(it) }
            }

            val ninoId = viewModel.ninoActualId ?: ""
            val ninoNombre = viewModel.ninos.getOrNull(viewModel.ninoSeleccionadoIndex)?.fullName ?: "Alumno/a"
            val resumen = viewModel.resumenSemanal ?: WeeklySummary(childId = ninoId, childName = ninoNombre)

            PadreBitacoraScreen(
                resumen = resumen,
                ninos = viewModel.ninos.map { it.fullName.substringBefore(" ") },
                ninoSeleccionado = viewModel.ninoSeleccionadoIndex,
                onSeleccionarNino = {
                    viewModel.seleccionarNino(it)
                    viewModel.ninos.getOrNull(it)?.id?.let { id -> viewModel.cargarResumenSemanal(id) }
                },
                onAbrirPerfilNino = { indice ->
                    viewModel.ninos.getOrNull(indice)?.id?.let { id ->
                        nav.navigate(Route.padrePerfilNino(id))
                    }
                },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_PADRE) { launchSingleTop = true }
                        1 -> {}
                        2 -> {
                            val targetId = viewModel.ninoActualId ?: viewModel.ninos.firstOrNull()?.id ?: ""
                            if (targetId.isNotBlank()) {
                                nav.navigate(Route.chatPadre(targetId)) { launchSingleTop = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            route = Route.PADRE_AUTORIZAR_TERCERO,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargarAutorizacionHoy(ninoId) }
            val nino = viewModel.ninos.firstOrNull { it.id == ninoId }

            PadreAutorizarTerceroScreen(
                childId = ninoId,
                childName = nino?.fullName ?: "Alumno/a",
                parentName = sesion.nombre,
                autorizacionExistente = viewModel.autorizacionHoy,
                onAtras = { nav.popBackStack() },
                onGuardar = { autorizacion ->
                    viewModel.guardarAutorizacionTercero(autorizacion) {
                        nav.popBackStack()
                    }
                }
            )
        }

        composable(
            route = Route.PADRE_PERFIL_NINO,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.cargarInicio() }
            val nino = viewModel.ninos.firstOrNull { it.id == ninoId } ?: Child(id = ninoId, fullName = "Alumno", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = sesion.nombre)

            PadrePerfilNinoScreen(
                child = nino,
                onAtras = { nav.popBackStack() },
                onSolicitarCambio = { mensaje ->
                    viewModel.enviarMensaje(mensaje)
                }
            )
        }

        composable(
            route = Route.RESUMEN_SEMANAL_PADRE,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargarResumenSemanal(ninoId) }

            val resumen = viewModel.resumenSemanal ?: WeeklySummary(childId = ninoId, childName = "Alumno/a")

            PadreResumenSemanalScreen(
                resumen = resumen,
                onCerrar = { nav.popBackStack() }
            )
        }

        composable(
            route = Route.CHAT_PADRE,
            arguments = listOf(navArgument("ninoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ninoId = backStackEntry.arguments?.getString("ninoId") ?: ""
            val viewModel: PadreViewModel = hiltViewModel()
            LaunchedEffect(ninoId) { viewModel.cargarMensajes(ninoId) }
            val nino = viewModel.ninos.firstOrNull { it.id == ninoId }

            ChatPadreScreen(
                estado = viewModel.estadoMensajes,
                currentUserId = sesion.id,
                subtitulo = nino?.let { "Sobre ${it.fullName.substringBefore(" ")}" },
                onAtras = { nav.popBackStack() },
                onEnviar = { texto, esAviso -> viewModel.enviarMensaje(texto, esAviso) },
                onEnviarFoto = { uri -> viewModel.enviarFoto(uri) },
                // Only the parent (or the estancia admin, in child chats) can delete; teachers can't.
                onEliminar = if (sesion.rol == Rol.PADRE || sesion.rol == Rol.ADMIN) {
                    { mensaje -> viewModel.deleteMessage(mensaje.id) }
                } else null,
                onReintentar = { viewModel.cargarMensajes(ninoId) },
                onTab = { indice ->
                    when (indice) {
                        0 -> nav.navigate(Route.INICIO_PADRE) { launchSingleTop = true }
                        1 -> nav.navigate(Route.BITACORA_PADRE) { launchSingleTop = true }
                        2 -> {}
                    }
                }
            )
        }
    }
}
