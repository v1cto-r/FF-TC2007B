package mx.tec.sonrisas.ui.navigation

import android.net.Uri

object Route {
    const val INICIO_MAESTRA = "inicio_maestra"
    const val NINOS = "ninos"
    const val PERFIL_NINO = "perfil_nino/{nombreNino}"
    fun perfilNino(nombre: String): String = "perfil_nino/${Uri.encode(nombre)}"
    const val CHAT_MAESTRA = "chat_maestra"

    const val INICIO_PADRE = "inicio_padre"
    const val CHAT_PADRE = "chat_padre"
}
