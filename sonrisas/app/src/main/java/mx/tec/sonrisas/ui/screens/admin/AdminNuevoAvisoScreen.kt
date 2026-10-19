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
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminNuevoAvisoScreen(
    salones: List<SalonUi>,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onEnviar: (titulo: String, mensaje: String, salonId: String?) -> Unit = { _, _, _ -> }
) {
    FlagSecureDisable()
    var titulo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var salonSel by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Enviar aviso",
                habilitado = titulo.isNotBlank() && mensaje.isNotBlank(),
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onEnviar(titulo, mensaje, salonSel) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Nuevo aviso", subtitulo = "Comunicado para la estancia", onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CampoTexto("Título", titulo, { titulo = it }) }
                item { CampoTexto("Mensaje", mensaje, { mensaje = it }, singleLine = false) }

                item { Spacer(Modifier.height(8.dp)) }
                // BACKEND: no existe "solo maestros". El aviso lo ven también los padres de la estancia o del salón elegido.
                item { TituloSeccion("¿A quién se envía?") }
                item {
                    FilaRadio(
                        titulo = "Toda la estancia",
                        subtitulo = "Maestras y padres de todos los salones",
                        seleccionado = salonSel == null,
                        onClick = { salonSel = null }
                    )
                }
                items(salones) { salon ->
                    FilaRadio(
                        titulo = "Salón ${salon.nombre}",
                        subtitulo = salon.maestro?.let { "Maestra: ${it.nombre}" } ?: "Sin maestra asignada",
                        seleccionado = salonSel == salon.id,
                        onClick = { salonSel = salon.id }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminNuevoAvisoPreview() {
    SonrisasTheme { AdminNuevoAvisoScreen(salones = AdminMuestra.salones) }
}