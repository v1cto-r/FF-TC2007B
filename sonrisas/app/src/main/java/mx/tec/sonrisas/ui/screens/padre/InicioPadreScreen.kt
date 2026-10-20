package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.PickupAuthorization
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
    autorizacionHoy: PickupAuthorization? = null,
    onSeleccionarNino: (Int) -> Unit = {},
    onAbrirPerfilNino: (Int) -> Unit = {},
    onAutorizarTercero: () -> Unit = {},
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
                    onSeleccionar = onSeleccionarNino,
                    onAbrirPerfil = { onAbrirPerfilNino(it) }
                )
            }

            // Tarjeta de Autorización a Tercero
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable(onClick = onAutorizarTercero),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (autorizacionHoy != null) CoralSuave else Color.White),
                border = if (autorizacionHoy != null) BorderStroke(1.dp, Coral) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HowToReg,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (autorizacionHoy != null) "Tercero Autorizado para Hoy" else "Autorizar entrega a tercero",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Text(
                            text = if (autorizacionHoy != null)
                                "${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship})"
                            else
                                "Registra quién pasará por tu hijo/a hoy",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

// Selector con comportamiento inteligente:
// 1 hijo -> Al hacer clic abre su perfil directamente.
// 2+ hijos -> Abre el menú desplegable. El ícono ℹ️ abre el perfil de ese hijo específicamente.
@Composable
private fun SelectorNinoHoy(
    ninos: List<String>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit,
    onAbrirPerfil: (Int) -> Unit = {}
) {
    var abierto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(50)
    val soloUnHijo = ninos.size <= 1

    Box {
        Row(
            modifier = Modifier
                .background(Color.White, forma)
                .border(1.dp, BordeCampo, forma)
                .clickable {
                    if (soloUnHijo) {
                        onAbrirPerfil(0)
                    } else {
                        abierto = true
                    }
                }
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
            if (!soloUnHijo) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Cambiar niño",
                    tint = TextoGris,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (!soloUnHijo) {
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                ninos.forEachIndexed { indice, nombre ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoOscuro,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        abierto = false
                                        onAbrirPerfil(indice)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Ver perfil",
                                        tint = Coral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
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
