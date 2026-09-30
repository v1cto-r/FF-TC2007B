package mx.tec.sonrisas.ui.state

import mx.tec.sonrisas.domain.Sesion

sealed interface SesionEstado {
    data object Cargando : SesionEstado
    data object Anonimo : SesionEstado
    data class Activa(val sesion: Sesion) : SesionEstado
}
