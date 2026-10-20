package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminMaestrasScreen(
    maestros: List<MaestroUi>,
    salones: List<SalonUi>,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onNuevaMaestra: () -> Unit = {},
    onEliminar: (MaestroUi) -> Unit = {}
) {
    FlagSecureDisable()
    var porEliminar by remember { mutableStateOf<MaestroUi?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaMaestra,
                containerColor = Coral,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("Nueva maestra", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Maestras",
                subtitulo = if (maestros.size == 1) "1 maestra" else "${maestros.size} maestras",
                onAtras = onAtras
            )

            errorMensaje?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Coral
                )
            }

            if (maestros.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin maestras registradas",
                    subtitulo = "Crea la primera con el botón de abajo",
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(maestros) { maestro ->
                    val suyos = salones.filter { it.maestro?.id == maestro.id }.map { it.nombre }
                    ItemPersona(
                        titulo = maestro.nombre,
                        subtitulo = if (suyos.isEmpty()) "${maestro.correo} · Sin salón"
                        else "${maestro.correo} · ${suyos.joinToString()}",
                        trailing = {
                            IconButton(onClick = { porEliminar = maestro }, enabled = !guardando) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar", tint = Coral)
                            }
                        }
                    )
                }
            }
        }
    }

    porEliminar?.let { maestro ->
        val suyos = salones.filter { it.maestro?.id == maestro.id }.map { it.nombre }
        AdminConfirmarBajaDialog(
            titulo = "Eliminar a ${maestro.nombre}",
            mensaje = buildString {
                if (suyos.isNotEmpty()) append("Se quitará de ${suyos.joinToString()}, que quedarán sin maestra. ")
                append("Su sesión se cerrará de inmediato y ya no podrá entrar.")
            },
            onConfirmar = {
                porEliminar = null
                onEliminar(maestro)
            },
            onCancelar = { porEliminar = null }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminMaestrasPreview() {
    SonrisasTheme { AdminMaestrasScreen(maestros = AdminMuestra.maestros, salones = AdminMuestra.salones) }
}
