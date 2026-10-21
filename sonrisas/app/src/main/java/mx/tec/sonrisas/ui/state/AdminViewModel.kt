package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.AdminRepository
import mx.tec.sonrisas.data.AlergenosRepository
import mx.tec.sonrisas.data.AdminSnapshot
import mx.tec.sonrisas.data.GROUP_TYPE_POR_DEFECTO
import mx.tec.sonrisas.data.RolApi
import mx.tec.sonrisas.data.remote.AdminChildDto
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.MealBody
import mx.tec.sonrisas.data.remote.MenuCheckDto
import mx.tec.sonrisas.data.remote.UserDto
import mx.tec.sonrisas.ui.screens.admin.AdminDatos
import mx.tec.sonrisas.ui.screens.admin.AlergenoDetectadoUi
import mx.tec.sonrisas.ui.screens.admin.AlumnoUi
import mx.tec.sonrisas.ui.screens.admin.AnalisisMenuUi
import mx.tec.sonrisas.ui.screens.admin.ConflictoUi
import mx.tec.sonrisas.ui.screens.admin.CredencialUi
import mx.tec.sonrisas.ui.screens.admin.MaestroUi
import mx.tec.sonrisas.ui.screens.admin.NinoConflictoUi
import mx.tec.sonrisas.ui.screens.admin.PadreUi
import mx.tec.sonrisas.ui.screens.admin.PlatilloUi
import mx.tec.sonrisas.ui.screens.admin.SalonUi

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val alergenos: AlergenosRepository
) : ViewModel() {

    var estado by mutableStateOf<UiState<AdminDatos>>(UiState.Cargando)
        private set

    var guardando by mutableStateOf(false)
        private set

    var errorMensaje by mutableStateOf<String?>(null)
        private set

    var credencial by mutableStateOf<CredencialUi?>(null)
        private set

    private var snapshot: AdminSnapshot? = null

    // --- Menú semanal y alergias ---

    private var siguientePlatillo = 1L

    var platillos by mutableStateOf(listOf(PlatilloUi(id = 0)))
        private set

    /** null = todavía no se analiza este menú. */
    var analisisMenu by mutableStateOf<UiState<AnalisisMenuUi>?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            if (estado !is UiState.Exito) estado = UiState.Cargando
            try {
                refrescar()
            } catch (e: Exception) {
                if (estado !is UiState.Exito) estado = UiState.Error(mensajeAdmin(e))
            }
        }
    }

    fun limpiarError() {
        errorMensaje = null
    }

    fun descartarCredencial() {
        credencial = null
    }

    // --- Grupos ---

    fun crearGrupo(nombre: String, maestroId: String?, alumnoIds: Set<String>, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            val salonId = UUID.randomUUID().toString()
            repo.guardarSalon(salonId, snap.estanciaId, nombre, GROUP_TYPE_POR_DEFECTO, maestroId)
            moverNinos(snap, alumnoIds, salonId)
        }

    fun asignarMaestra(salonId: String, maestroId: String?, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            val salon = snap.salones.firstOrNull { it.id == salonId }
                ?: throw IllegalStateException("Salón no encontrado")
            repo.guardarSalon(salon.id, snap.estanciaId, salon.name, salon.groupType, maestroId)
        }

    fun agregarAlumnos(salonId: String, alumnoIds: Set<String>, onListo: () -> Unit) =
        ejecutar(onListo) {
            moverNinos(requerir(), alumnoIds, salonId)
        }

    // --- Cuentas y niños ---

    fun crearUsuario(nombre: String, apellido: String, correo: String, rol: String) = ejecutar {
        val completo = nombreCompleto(nombre, apellido)
        val creado = repo.crearUsuario(correo, completo, rol)
        credencial = CredencialUi(completo, creado.email, creado.temporaryPassword)
    }

    fun inscribir(nombreTutor: String, apellidoTutor: String, correo: String, nombreAlumno: String) = ejecutar {
        val snap = requerir()
        val completo = nombreCompleto(nombreTutor, apellidoTutor)
        val creado = repo.crearUsuario(correo, completo, RolApi.PADRE)
        credencial = CredencialUi(completo, creado.email, creado.temporaryPassword)
        repo.guardarNino(UUID.randomUUID().toString(), snap.estanciaId, nombreAlumno, null, creado.id)
    }

    fun agregarNino(padreId: String, nombreNino: String, salonId: String?, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            repo.guardarNino(UUID.randomUUID().toString(), snap.estanciaId, nombreNino, salonId, padreId)
        }

    // --- Bajas (soft delete) ---

    /** Takes her off her salones first: the API refuses to delete a docente who still teaches one. */
    fun eliminarMaestra(maestroId: String) = ejecutar {
        val snap = requerir()
        snap.salones.filter { it.docenteId == maestroId && it.status == null }.forEach { s ->
            repo.guardarSalon(s.id, snap.estanciaId, s.name, s.groupType, null)
        }
        repo.eliminarUsuario(maestroId)
    }

    fun eliminarNino(ninoId: String) = ejecutar {
        repo.eliminarNino(ninoId)
    }

    /** Removes their children first: the API refuses to delete a padre who still has any. */
    fun eliminarPadre(padreId: String) = ejecutar {
        requerir().ninos.filter { it.parentId == padreId && it.status == null }.forEach { repo.eliminarNino(it.id) }
        repo.eliminarUsuario(padreId)
    }

    // --- Menú semanal y alergias ---

    fun agregarPlatillo() {
        platillos = platillos + PlatilloUi(id = siguientePlatillo++)
        analisisMenu = null
    }

    fun quitarPlatillo(id: Long) {
        platillos = platillos.filterNot { it.id == id }.ifEmpty { listOf(PlatilloUi(id = siguientePlatillo++)) }
        analisisMenu = null
    }

    fun cambiarNombrePlatillo(id: Long, nombre: String) {
        platillos = platillos.map { if (it.id == id) it.copy(nombre = nombre) else it }
        analisisMenu = null
    }

    /** Acepta varios separados por coma ("leche, huevo"); ignora los repetidos. */
    fun agregarIngrediente(id: Long, texto: String) {
        val nuevos = texto.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (nuevos.isEmpty()) return
        platillos = platillos.map { p ->
            if (p.id == id) p.copy(ingredientes = (p.ingredientes + nuevos).distinctBy { it.lowercase() }) else p
        }
        analisisMenu = null
    }

    fun quitarIngrediente(id: Long, ingrediente: String) {
        platillos = platillos.map { if (it.id == id) it.copy(ingredientes = it.ingredientes - ingrediente) else it }
        analisisMenu = null
    }

    fun analizarMenu() {
        val enviar = platillos.filter { it.nombre.isNotBlank() }
            .map { MealBody(it.nombre.trim(), it.ingredientes) }
        if (enviar.isEmpty() || analisisMenu == UiState.Cargando) return
        viewModelScope.launch {
            analisisMenu = UiState.Cargando
            analisisMenu = try {
                UiState.Exito(alergenos.revisarMenu(requerir().estanciaId, enviar).toUi())
            } catch (e: Exception) {
                UiState.Error(mensajeAdmin(e))
            }
        }
    }

    // --- Internos ---

    private suspend fun moverNinos(snap: AdminSnapshot, ids: Set<String>, salonId: String) {
        snap.ninos.filter { it.id in ids }.forEach { nino ->
            repo.guardarNino(nino.id, snap.estanciaId, nino.fullName, salonId, nino.parentId)
        }
    }

    private suspend fun refrescar() {
        val nuevo = repo.cargar()
        snapshot = nuevo
        estado = UiState.Exito(nuevo.toDatos())
    }

    private fun requerir(): AdminSnapshot =
        snapshot ?: throw IllegalStateException("Los datos todavía no se cargan")

    private fun nombreCompleto(nombre: String, apellido: String) = "${nombre.trim()} ${apellido.trim()}".trim()

    private fun ejecutar(onListo: () -> Unit = {}, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            guardando = true
            errorMensaje = null
            var fallo: Exception? = null
            try {
                bloque()
            } catch (e: Exception) {
                fallo = e
            }
            runCatching { refrescar() }
            guardando = false
            if (fallo == null) onListo() else errorMensaje = mensajeAdmin(fallo)
        }
    }

    private fun mensajeAdmin(e: Exception): String = when {
        e is ApiException && e.code == 403 -> e.detail ?: "No tienes permiso para hacer esto."
        e is ApiException && e.code == 404 -> e.detail ?: "No encontramos lo que buscas."
        e is ApiException && e.code == 409 -> e.detail ?: "Ese registro ya existe o entra en conflicto con otro."
        e is ApiException && (e.code == 502 || e.code == 503) -> "El análisis no está disponible, intenta más tarde."
        else -> mensajeDeError(e, ante401 = "Tu sesión venció. Cierra sesión e inicia de nuevo.")
    }
}

private fun MenuCheckDto.toUi() = AnalisisMenuUi(
    detectados = detected.map { AlergenoDetectadoUi(it.name, (it.probability * 100).toInt(), it.meals) },
    ninos = conflicts.map { c ->
        NinoConflictoUi(c.childId, c.fullName, c.salonName, c.conflicts.map { ConflictoUi(it.allergen, it.meals) })
    }
)

private fun UserDto.toMaestro() = MaestroUi(id = id, nombre = fullName, correo = email)

private fun AdminSnapshot.toDatos(): AdminDatos {
    val maestrosApi = docentes.filter { it.status == null }
    val tutoresApi = padres.filter { it.status == null }
    val ninosActivos = ninos.filter { it.status == null }
    val salonesActivos = salones.filter { it.status == null }

    val maestroPorId = maestrosApi.associateBy { it.id }
    val tutorPorId = tutoresApi.associateBy { it.id }
    val salonPorId = salonesActivos.associateBy { it.id }

    fun alumno(n: AdminChildDto) = AlumnoUi(
        id = n.id,
        nombre = n.fullName,
        tutor = n.parentId?.let { tutorPorId[it] }?.fullName ?: "Sin tutor asignado",
        salon = n.salonId?.let { salonPorId[it] }?.name
    )

    val salonesUi = salonesActivos
        .map { s ->
            SalonUi(
                id = s.id,
                nombre = s.name,
                maestro = s.docenteId?.let { maestroPorId[it] }?.toMaestro(),
                alumnos = ninosActivos.filter { it.salonId == s.id }.map { alumno(it) }
            )
        }
        .sortedBy { it.nombre }

    val padresUi = tutoresApi
        .map { p ->
            PadreUi(
                id = p.id,
                nombre = p.fullName,
                correo = p.email,
                hijos = ninosActivos.filter { it.parentId == p.id }.map { alumno(it) }
            )
        }
        .sortedBy { it.nombre }

    return AdminDatos(
        salones = salonesUi,
        maestros = maestrosApi.map { it.toMaestro() }.sortedBy { it.nombre },
        padres = padresUi,
        alumnosSinSalon = ninosActivos.filter { it.salonId == null }.map { alumno(it) }
    )
}
