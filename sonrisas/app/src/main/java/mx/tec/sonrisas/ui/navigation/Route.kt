package mx.tec.sonrisas.ui.navigation

import android.net.Uri

object Route {
    const val INICIO_MAESTRA = "inicio_maestra"
    const val NINOS = "ninos"
    const val PERFIL_NINO = "perfil_nino/{ninoId}"
    fun perfilNino(ninoId: String): String = "perfil_nino/${Uri.encode(ninoId)}"

    const val CHAT_LISTA_MAESTRA = "chat_lista_maestra"

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

    const val PADRE_AUTORIZAR_TERCERO = "padre_autorizar_tercero/{ninoId}"
    fun padreAutorizarTercero(ninoId: String): String = "padre_autorizar_tercero/${Uri.encode(ninoId)}"

    const val RESUMEN_SEMANAL_PADRE = "resumen_semanal_padre/{ninoId}"
    fun resumenSemanalPadre(ninoId: String): String = "resumen_semanal_padre/${Uri.encode(ninoId)}"

    const val CHAT_PADRE = "chat_padre/{ninoId}"
    fun chatPadre(ninoId: String): String = "chat_padre/${Uri.encode(ninoId)}"
}
