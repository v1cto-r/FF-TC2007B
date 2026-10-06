package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminDetalleSalonScreen(
    salon: SalonUi,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onAgregarAlumnos: () -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val n = salon.alumnos.size
            TopBarAtras(
                titulo = salon.nombre,
                subtitulo = if (n == 1) "1 alumno" else "$n alumnos",
                onAtras = onAtras
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TituloSeccion("Maestra asignada") }
                item {
                    if (salon.maestro != null) {
                        ItemPersona(titulo = salon.maestro.nombre, subtitulo = salon.maestro.correo)
                    } else {
                        Text(
                            text = "Este salón aún no tiene maestra asignada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Coral
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Alumnos ($n)") }

                if (salon.alumnos.isEmpty()) {
                    item {
                        Text(
                            text = "Aún no hay alumnos en este salón",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                } else {
                    items(salon.alumnos) { alumno ->
                        ItemPersona(titulo = alumno.nombre, subtitulo = "Tutor: ${alumno.tutor}")
                    }
                }

                item {
                    Button(
                        onClick = onAgregarAlumnos,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar alumnos")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminDetalleSalonPreview() {
    SonrisasTheme { AdminDetalleSalonScreen(salon = AdminMuestra.salones[0]) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminDetalleSalonSinMaestraPreview() {
    SonrisasTheme { AdminDetalleSalonScreen(salon = AdminMuestra.salones[2]) }
}