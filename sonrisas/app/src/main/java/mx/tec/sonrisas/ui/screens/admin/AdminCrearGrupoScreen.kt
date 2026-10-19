package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminCrearGrupoScreen(
    maestros: List<MaestroUi>,
    alumnosDisponibles: List<AlumnoUi>,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onCrear: (nombre: String, maestroId: String?, alumnoIds: Set<String>) -> Unit = { _, _, _ -> }
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }
    var maestroSel by remember { mutableStateOf<String?>(null) }
    var alumnosSel by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Crear grupo",
                habilitado = nombre.isNotBlank(),
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onCrear(nombre, maestroSel, alumnosSel) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Crear grupo", subtitulo = "Nuevo salón", onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CampoTexto("Nombre del grupo", nombre, { nombre = it }) }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Maestra del grupo (opcional)") }
                if (maestros.isEmpty()) {
                    item {
                        Text(
                            text = "Aún no hay maestras. Crea una desde el Inicio.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                }
                items(maestros) { maestro ->
                    val elegido = maestro.id == maestroSel
                    FilaRadio(
                        titulo = maestro.nombre,
                        subtitulo = maestro.correo,
                        seleccionado = elegido,
                        // tocar la maestra elegida la quita
                        onClick = { maestroSel = if (elegido) null else maestro.id }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Alumnos sin salón (${alumnosSel.size} seleccionados)") }
                if (alumnosDisponibles.isEmpty()) {
                    item {
                        Text(
                            text = "No hay alumnos sin salón",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                }
                items(alumnosDisponibles) { alumno ->
                    val marcado = alumno.id in alumnosSel
                    FilaCheck(
                        titulo = alumno.nombre,
                        subtitulo = "Tutor: ${alumno.tutor}",
                        marcado = marcado,
                        onClick = { alumnosSel = if (marcado) alumnosSel - alumno.id else alumnosSel + alumno.id }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminCrearGrupoPreview() {
    SonrisasTheme {
        AdminCrearGrupoScreen(
            maestros = AdminMuestra.maestros,
            alumnosDisponibles = AdminMuestra.alumnosSinSalon
        )
    }
}