package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import mx.tec.sonrisas.ui.components.CampoOpciones
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

/**
 * Crear aviso para los administradores de las instancias.
 * idsInstancias = null en onEnviar significa "todas las instancias".
 */
@Composable
fun OrgNuevoAvisoScreen(
    instancias: List<InstanciaUi>,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onEnviar: (titulo: String, mensaje: String, urgente: Boolean, idsInstancias: List<String>?) -> Unit =
        { _, _, _, _ -> }
) {
    FlagSecureDisable()
    var titulo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var paraTodas by remember { mutableStateOf(true) }
    var urgente by remember { mutableStateOf(false) }
    var seleccionadas by remember { mutableStateOf(setOf<String>()) }

    // Con "Elegir instancias" hay que marcar al menos una
    val puedeEnviar = titulo.isNotBlank() && mensaje.isNotBlank() && (paraTodas || seleccionadas.isNotEmpty())

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = {
                        onEnviar(titulo, mensaje, urgente, if (paraTodas) null else seleccionadas.toList())
                    },
                    enabled = puedeEnviar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral,
                        contentColor = Color.White,
                        disabledContainerColor = Coral.copy(alpha = 0.4f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text("Enviar aviso")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Nuevo aviso",
                subtitulo = "Para administradores de instancias",
                onAtras = onAtras
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CampoTexto("Título", titulo, { titulo = it }) }
                item { CampoTexto("Mensaje", mensaje, { mensaje = it }, singleLine = false) }

                item {
                    CampoOpciones(
                        etiqueta = "Enviar a",
                        valor = paraTodas,
                        opciones = listOf(true, false),
                        onValorChange = { paraTodas = it },
                        etiquetaOpcion = { if (it) "Todas las instancias" else "Elegir instancias" }
                    )
                }

                // Lista con casillas solo si eligió instancias específicas
                if (!paraTodas) {
                    item { TituloSeccion("Instancias (${seleccionadas.size} seleccionadas)") }
                    items(instancias, key = { it.id }) { instancia ->
                        val elegida = instancia.id in seleccionadas
                        val alternar = {
                            seleccionadas = if (elegida) seleccionadas - instancia.id else seleccionadas + instancia.id
                        }
                        TarjetaInstancia(
                            instancia = instancia,
                            onClick = alternar,
                            resaltado = elegida,
                            trailing = {
                                Checkbox(
                                    checked = elegida,
                                    onCheckedChange = { alternar() },
                                    colors = CheckboxDefaults.colors(checkedColor = Coral)
                                )
                            }
                        )
                    }
                }

                item {
                    CampoOpciones(
                        etiqueta = "Tipo",
                        valor = urgente,
                        opciones = listOf(false, true),
                        onValorChange = { urgente = it },
                        etiquetaOpcion = { if (it) "Urgente" else "Informativo" }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgNuevoAvisoPreview() {
    SonrisasTheme { OrgNuevoAvisoScreen(instancias = OrgMuestra.instancias) }
}