package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import mx.tec.sonrisas.data.remote.AllergenDto
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.MealBody
import mx.tec.sonrisas.data.remote.MenuCheckBody
import mx.tec.sonrisas.data.remote.MenuCheckDto

/** El catálogo de alérgenos (lo usan padres y admin) y la revisión del menú semanal con Jev. */
@Singleton
class AlergenosRepository @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) {

    /** GET /allergens, ordenado por nombre. */
    suspend fun catalogo(): List<AllergenDto> = auth.authed { api.get<List<AllergenDto>>("/allergens", it) }

    /** POST /estancias/{id}/menu-check. Jev revisa platillo por platillo, así que puede tardar más que otras llamadas. */
    suspend fun revisarMenu(estanciaId: String, platillos: List<MealBody>): MenuCheckDto = auth.authed {
        api.post<MenuCheckBody, MenuCheckDto>(
            "/estancias/$estanciaId/menu-check",
            MenuCheckBody(platillos),
            it,
            timeoutSeconds = 90
        )
    }
}
