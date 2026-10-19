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
fun AdminDetallePadreScreen(
    padre: PadreUi,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onAgregarNino: () -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = padre.nombre, subtitulo = padre.correo, onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TituloSeccion("Niños (${padre.hijos.size})") }

                if (padre.hijos.isEmpty()) {
                    item {
                        Text(
                            text = "Este padre aún no tiene niños registrados",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                } else {
                    items(padre.hijos) { hijo ->
                        ItemPersona(
                            titulo = hijo.nombre,
                            subtitulo = hijo.salon?.let { "Salón $it" } ?: "Sin salón asignado"
                        )
                    }
                }

                item {
                    Button(
                        onClick = onAgregarNino,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar niño")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminDetallePadrePreview() {
    SonrisasTheme { AdminDetallePadreScreen(padre = AdminMuestra.padres[0]) }
}