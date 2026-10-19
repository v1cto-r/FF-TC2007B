package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminAsignarMaestraScreen(
    nombreSalon: String,
    maestros: List<MaestroUi>,
    maestroActualId: String?,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onGuardar: (String?) -> Unit = {}
) {
    FlagSecureDisable()
    var seleccion by remember { mutableStateOf(maestroActualId) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Guardar",
                habilitado = seleccion != maestroActualId,
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onGuardar(seleccion) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Maestra del salón", subtitulo = nombreSalon, onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TituloSeccion("Elige una maestra") }
                item {
                    FilaRadio(
                        titulo = "Sin maestra",
                        subtitulo = "Dejar el salón sin asignar",
                        seleccionado = seleccion == null,
                        onClick = { seleccion = null }
                    )
                }
                items(maestros) { maestro ->
                    FilaRadio(
                        titulo = maestro.nombre,
                        subtitulo = maestro.correo,
                        seleccionado = seleccion == maestro.id,
                        onClick = { seleccion = maestro.id }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminAsignarMaestraPreview() {
    SonrisasTheme {
        AdminAsignarMaestraScreen(
            nombreSalon = "Arcoíris",
            maestros = AdminMuestra.maestros,
            maestroActualId = null
        )
    }
}