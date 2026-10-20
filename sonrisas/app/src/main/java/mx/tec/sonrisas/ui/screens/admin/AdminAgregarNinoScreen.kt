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
fun AdminAgregarNinoScreen(
    nombrePadre: String,
    salones: List<SalonUi>,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onGuardar: (nombre: String, salonId: String?) -> Unit = { _, _ -> }
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }
    var salonSel by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Agregar niño",
                habilitado = nombre.isNotBlank(),
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onGuardar(nombre, salonSel) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Agregar niño", subtitulo = "Hijo(a) de $nombrePadre", onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CampoTexto("Nombre completo del niño", nombre, { nombre = it }) }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Salón (opcional)") }
                item {
                    FilaRadio(
                        titulo = "Sin salón por ahora",
                        subtitulo = "Se puede asignar después",
                        seleccionado = salonSel == null,
                        onClick = { salonSel = null }
                    )
                }
                items(salones) { salon ->
                    FilaRadio(
                        titulo = salon.nombre,
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
private fun AdminAgregarNinoPreview() {
    SonrisasTheme {
        AdminAgregarNinoScreen(nombrePadre = "María López", salones = AdminMuestra.salones)
    }
}