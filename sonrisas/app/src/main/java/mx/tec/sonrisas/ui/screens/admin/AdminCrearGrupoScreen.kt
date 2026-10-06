package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminCrearGrupoScreen(
    maestros: List<MaestroUi>,
    alumnosDisponibles: List<AlumnoUi>,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onCrear: () -> Unit = {}
) {
    FlagSecureEnable()
    var nombre by remember { mutableStateOf("") }
    var maestroSel by remember { mutableStateOf<String?>(null) }
    var alumnosSel by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = onCrear,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Crear grupo")
                }
            }
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
                item { TituloSeccion("Maestra del grupo") }
                items(maestros) { maestro ->
                    val elegido = maestro.id == maestroSel
                    ItemPersona(
                        titulo = maestro.nombre,
                        subtitulo = maestro.correo,
                        resaltado = elegido,
                        onClick = { maestroSel = maestro.id },
                        trailing = {
                            RadioButton(
                                selected = elegido,
                                onClick = { maestroSel = maestro.id },
                                colors = RadioButtonDefaults.colors(selectedColor = Coral)
                            )
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Alumnos (${alumnosSel.size} seleccionados)") }
                items(alumnosDisponibles) { alumno ->
                    val elegido = alumno.id in alumnosSel
                    val alternar = {
                        alumnosSel = if (elegido) alumnosSel - alumno.id else alumnosSel + alumno.id
                    }
                    ItemPersona(
                        titulo = alumno.nombre,
                        subtitulo = "Tutor: ${alumno.tutor}",
                        resaltado = elegido,
                        onClick = alternar,
                        trailing = {
                            Checkbox(
                                checked = elegido,
                                onCheckedChange = { alternar() },
                                colors = CheckboxDefaults.colors(checkedColor = Coral)
                            )
                        }
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