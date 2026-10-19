package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import mx.tec.sonrisas.data.remote.AdminChildDto
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.ChildInBody
import mx.tec.sonrisas.data.remote.MeDto
import mx.tec.sonrisas.data.remote.NoticeDto
import mx.tec.sonrisas.data.remote.NoticeInBody
import mx.tec.sonrisas.data.remote.SalonDto
import mx.tec.sonrisas.data.remote.SalonInBody
import mx.tec.sonrisas.data.remote.UserCreateBody
import mx.tec.sonrisas.data.remote.UserCreatedDto
import mx.tec.sonrisas.data.remote.UserDto

object RolApi {
    const val DOCENTE = "DOCENTE"
    const val PADRE = "PADRE"
}

// BACKEND: SalonIn.group_type es obligatorio en la API y la UI ya no pide tipo de grupo.
// Se manda "P" fijo. Pedir que sea opcional (o quitarlo) para borrar esta constante.
const val GROUP_TYPE_POR_DEFECTO = "P"

/** Todo lo que el admin ve de su estancia, tal cual lo devuelve la API. */
data class AdminSnapshot(
    val estanciaId: String,
    val salones: List<SalonDto>,
    val docentes: List<UserDto>,
    val padres: List<UserDto>,
    val ninos: List<AdminChildDto>,
    val avisos: List<NoticeDto>
)

@Singleton
class AdminRepository @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) {

    /** GET /auth/me, /salones, /users (DOCENTE y PADRE), /children y /notices de la estancia del admin. */
    suspend fun cargar(): AdminSnapshot = auth.authed { token ->
        val me = api.get<MeDto>("/auth/me", token)
        val estancia = me.estanciaId ?: throw ApiException(403, "Tu cuenta no tiene una estancia asignada")
        AdminSnapshot(
            estanciaId = estancia,
            salones = api.get<List<SalonDto>>("/salones?estancia_id=$estancia", token),
            docentes = api.get<List<UserDto>>("/users?role=${RolApi.DOCENTE}&estancia_id=$estancia", token),
            padres = api.get<List<UserDto>>("/users?role=${RolApi.PADRE}&estancia_id=$estancia", token),
            ninos = api.get<List<AdminChildDto>>("/children?estancia_id=$estancia", token),
            avisos = api.get<List<NoticeDto>>("/notices?estancia_id=$estancia", token)
        )
    }

    /** POST /users. Devuelve la contraseña temporal, que la API solo muestra en esta respuesta. */
    suspend fun crearUsuario(correo: String, nombreCompleto: String, rol: String): UserCreatedDto =
        auth.authed {
            api.post<UserCreateBody, UserCreatedDto>(
                "/users",
                UserCreateBody(correo.trim(), nombreCompleto.trim(), rol),
                it
            )
        }

    /** PUT /salones/{id}: crea el salón o lo reemplaza (nombre, tipo y maestra). */
    suspend fun guardarSalon(id: String, estanciaId: String, nombre: String, groupType: String, docenteId: String?) {
        auth.authed {
            api.put<SalonInBody, SalonDto>(
                "/salones/$id",
                SalonInBody(estanciaId, nombre.trim(), groupType, docenteId),
                it
            )
        }
    }

    /** PUT /children/{id}: crea al niño o lo reemplaza (nombre, salón y padre). */
    suspend fun guardarNino(id: String, estanciaId: String, nombre: String, salonId: String?, padreId: String?) {
        auth.authed {
            api.put<ChildInBody, AdminChildDto>(
                "/children/$id",
                ChildInBody(estanciaId, salonId, padreId, nombre.trim()),
                it
            )
        }
    }

    /** PUT /notices/{id}. salonId = null publica en toda la estancia. */
    suspend fun guardarAviso(id: String, estanciaId: String, salonId: String?, titulo: String, descripcion: String, fecha: String) {
        auth.authed {
            api.put<NoticeInBody, NoticeDto>(
                "/notices/$id",
                NoticeInBody(estanciaId, salonId, titulo.trim(), descripcion.trim(), fecha),
                it
            )
        }
    }
}