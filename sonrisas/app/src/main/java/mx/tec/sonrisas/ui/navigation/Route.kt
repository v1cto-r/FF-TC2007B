package mx.tec.sonrisas.ui.navigation

import android.net.Uri

object Route {
    const val INICIO_MAESTRA = "inicio_maestra"
    const val NINOS = "ninos"
    const val PERFIL_NINO = "perfil_nino/{ninoId}"
    fun perfilNino(ninoId: String): String = "perfil_nino/${Uri.encode(ninoId)}"

    const val CHAT_LISTA_MAESTRA = "chat_lista_maestra"

    // The teacher's chat with the estancia admin (its chat id is the teacher's own user id).
    const val CHAT_ADMIN_MAESTRA = "maestra_chat_admin"

    const val CHAT_MAESTRA = "chat_maestra/{ninoId}"
    fun chatMaestra(ninoId: String): String = "chat_maestra/${Uri.encode(ninoId)}"

    const val CHECK_IN = "check_in/{ninoId}"
    fun checkIn(ninoId: String): String = "check_in/${Uri.encode(ninoId)}"

    const val STAY = "stay/{ninoId}"
    fun stay(ninoId: String): String = "stay/${Uri.encode(ninoId)}"

    const val ACTIVITY = "activity/{ninoId}"
    fun activity(ninoId: String): String = "activity/${Uri.encode(ninoId)}"

    const val CHECK_OUT = "check_out/{ninoId}"
    fun checkOut(ninoId: String): String = "check_out/${Uri.encode(ninoId)}"

    const val ACTIVIDAD_MASIVA = "actividad_masiva"
    const val AVISO_MASIVO = "aviso_masivo"

    const val INICIO_PADRE = "inicio_padre"
    const val BITACORA_PADRE = "bitacora_padre"
    const val CAMBIAR_CONTRASENA = "cambiar_contrasena"
    const val ONBOARDING_HIJO = "onboarding_hijo/{ninoId}"
    fun onboardingHijo(ninoId: String): String = "onboarding_hijo/${Uri.encode(ninoId)}"

    const val PADRE_AUTORIZAR_TERCERO = "padre_autorizar_tercero/{ninoId}"
    fun padreAutorizarTercero(ninoId: String): String = "padre_autorizar_tercero/${Uri.encode(ninoId)}"

    const val PADRE_PERFIL_NINO = "padre_perfil_nino/{ninoId}"
    fun padrePerfilNino(ninoId: String): String = "padre_perfil_nino/${Uri.encode(ninoId)}"

    const val RESUMEN_SEMANAL_PADRE = "resumen_semanal_padre/{ninoId}"
    fun resumenSemanalPadre(ninoId: String): String = "resumen_semanal_padre/${Uri.encode(ninoId)}"

    const val CHAT_PADRE = "chat_padre/{ninoId}"
    fun chatPadre(ninoId: String): String = "chat_padre/${Uri.encode(ninoId)}"

    // --- ADMIN ---
    const val ADMIN_INICIO = "admin_inicio"
    const val ADMIN_SALONES = "admin_salones"
    const val ADMIN_INSCRIPCION = "admin_inscripcion"
    const val ADMIN_CREAR_GRUPO = "admin_crear_grupo"

    const val ADMIN_DETALLE_SALON = "admin_salon/{salonId}"
    fun adminDetalleSalon(salonId: String): String = "admin_salon/${Uri.encode(salonId)}"

    const val ADMIN_AGREGAR_ALUMNOS = "admin_agregar_alumnos/{salonId}"
    fun adminAgregarAlumnos(salonId: String): String = "admin_agregar_alumnos/${Uri.encode(salonId)}"

    const val ADMIN_ASIGNAR_MAESTRA = "admin_asignar_maestra/{salonId}"
    fun adminAsignarMaestra(salonId: String): String = "admin_asignar_maestra/${Uri.encode(salonId)}"

    const val ADMIN_PADRES = "admin_padres"
    const val ADMIN_AVISOS = "admin_avisos"
    const val ADMIN_CREAR_MAESTRA = "admin_crear_maestra"
    const val ADMIN_CREAR_PADRE = "admin_crear_padre"

    const val ADMIN_DETALLE_PADRE = "admin_padre/{padreId}"
    fun adminDetallePadre(padreId: String): String = "admin_padre/${Uri.encode(padreId)}"

    const val ADMIN_AGREGAR_NINO = "admin_agregar_nino/{padreId}"
    fun adminAgregarNino(padreId: String): String = "admin_agregar_nino/${Uri.encode(padreId)}"

    // --- ADMIN CHAT ---
    const val ADMIN_CHAT_LIST = "admin_chat_list"
    const val ADMIN_CHAT_ORG = "admin_chat_org"
    const val ADMIN_CHAT_TEACHERS = "admin_chat_teachers"
    const val ADMIN_CHAT_TEACHER = "admin_chat_teacher/{teacherId}"
    fun adminChatTeacher(teacherId: String): String = "admin_chat_teacher/${Uri.encode(teacherId)}"


    // --- ORGANIZACIÓN ---
    const val ORG_INICIO = "org_inicio"
    const val ORG_INSTANCIAS = "org_instancias"
    const val ORG_AVISOS = "org_avisos"
    const val ORG_NUEVA_INSTANCIA = "org_nueva_instancia"
    const val ORG_NUEVO_AVISO = "org_nuevo_aviso"

    const val ORG_DETALLE_INSTANCIA = "org_instancia/{instanciaId}"
    fun orgDetalleInstancia(instanciaId: String): String = "org_instancia/${Uri.encode(instanciaId)}"

    const val ORG_EDITAR_INSTANCIA = "org_editar_instancia/{instanciaId}"
    fun orgEditarInstancia(instanciaId: String): String = "org_editar_instancia/${Uri.encode(instanciaId)}"

    const val ORG_REASIGNAR_ADMIN = "org_reasignar_admin/{instanciaId}"
    fun orgReasignarAdmin(instanciaId: String): String = "org_reasignar_admin/${Uri.encode(instanciaId)}"

    const val ORG_CHAT_ADMIN = "org_chat_admin"
}
