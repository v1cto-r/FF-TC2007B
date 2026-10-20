package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminAgregarAlumnosScreen(
    nombreSalon: String,
    alumnosDisponibles: List<AlumnoUi>,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onAgregar: (Set<String>) -> Unit = {}
) {
    FlagSecureDisable()
    var busqueda by remember { mutableStateOf("") }
    var seleccionados by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = if (seleccionados.isEmpty()) "Agregar alumnos" else "Agregar (${seleccionados.size})",
                habilitado = seleccionados.isNotEmpty(),
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onAgregar(seleccionados) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Agregar alumnos",
                subtitulo = "Salón $nombreSalon",
                onAtras = onAtras
            )

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Buscar alumno...", color = TextoGris) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextoGris) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = BordeCampo,
                    cursorColor = Coral
                )
            )
            Spacer(Modifier.height(8.dp))

            val filtrados = alumnosDisponibles.filter { it.nombre.contains(busqueda, ignoreCase = true) }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    alumnosDisponibles.isEmpty() -> EstadoVacio(
                        titulo = "Sin alumnos disponibles",
                        subtitulo = "Todos los alumnos ya tienen salón asignado",
                        modifier = Modifier.padding(16.dp)
                    )

                    filtrados.isEmpty() -> Text(
                        text = "Sin resultados para \"$busqueda\"",
                        modifier = Modifier.align(Alignment.TopCenter).padding(24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados) { alumno ->
                            val marcado = alumno.id in seleccionados
                            FilaCheck(
                                titulo = alumno.nombre,
                                subtitulo = "Tutor: ${alumno.tutor}",
                                marcado = marcado,
                                onClick = {
                                    seleccionados = if (marcado) seleccionados - alumno.id else seleccionados + alumno.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminAgregarAlumnosPreview() {
    SonrisasTheme {
        AdminAgregarAlumnosScreen(
            nombreSalon = "Mariposas",
            alumnosDisponibles = AdminMuestra.alumnosSinSalon
        )
    }
}