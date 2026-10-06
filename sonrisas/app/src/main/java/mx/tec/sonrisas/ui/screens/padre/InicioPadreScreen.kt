package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.ActividadUi
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TabsPadre
import mx.tec.sonrisas.ui.components.TarjetaActividad
import mx.tec.sonrisas.ui.components.TipoActividadUi
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun InicioPadreScreen(
    estado: UiState<List<ActividadUi>>,
    modifier: Modifier = Modifier,
    nombrePadre: String = "María",
    ninos: List<String> = listOf("Sofía", "Mateo"),
    ninoSeleccionado: Int = 0,
    onSeleccionarNino: (Int) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsPadre, seleccionado = 0, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Header con saludo y selector de niño
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Hola, $nombrePadre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Aquí está el resumen de hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
                SelectorNinoHoy(
                    ninos = ninos,
                    seleccionado = ninoSeleccionado,
                    onSeleccionar = onSeleccionarNino
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> Column(Modifier.padding(16.dp)) {
                        TituloResumen()
                        Spacer(Modifier.height(8.dp))
                        EstadoVacio(
                            titulo = "Sin actividades aún",
                            subtitulo = "La actividad de hoy aparecerá aquí"
                        )
                    }

                    is UiState.Error -> EstadoError(
                        titulo = "Algo salió mal",
                        mensaje = "No pudimos cargar la información",
                        onReintentar = onReintentar
                    )

                    is UiState.Exito -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { TituloResumen() }
                        items(estado.datos) { actividad ->
                            TarjetaActividad(actividad = actividad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloResumen() {
    Text(
        text = "Resumen del día",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = TextoOscuro
    )
}

// Selector "Hoy: [avatar] Sofía v" usado solo en esta pantalla
@Composable
private fun SelectorNinoHoy(
    ninos: List<String>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(50)

    Box {
        Row(
            modifier = Modifier
                .background(Color.White, forma)
                .border(1.dp, BordeCampo, forma)
                .clickable { abierto = true }
                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hoy:",
                style = MaterialTheme.typography.bodySmall,
                color = TextoGris
            )
            Spacer(Modifier.width(6.dp))
            AvatarCircular(tamano = 24.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = ninos.getOrElse(seleccionado) { "" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Cambiar niño",
                tint = TextoGris,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ninos.forEachIndexed { indice, nombre ->
                DropdownMenuItem(
                    text = { Text(nombre, style = MaterialTheme.typography.bodyMedium, color = TextoOscuro) },
                    leadingIcon = { AvatarCircular(tamano = 24.dp) },
                    onClick = {
                        onSeleccionar(indice)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioPadrePreview() {
    SonrisasTheme {
        InicioPadreScreen(
            estado = UiState.Exito(
                listOf(ActividadUi(TipoActividadUi.Comida, "Comió su comida", "Sopa de fideo y tacos de pollo", "1:00 PM"))
            )
        )
    }
}