package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.model.Mocks
import mx.tec.sonrisas.model.Nino
import mx.tec.sonrisas.model.UiState
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.ChipAsistencia
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.ItemAccionRegistro
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun PerfilNinoScreen(
    estado: UiState<Nino>,
    modifier: Modifier = Modifier,
    nombreAdulto: String = "María López",
    onAtras: () -> Unit = {},
    onAbrirChat: () -> Unit = {},
    onRegistrarActividad: () -> Unit = {},
    onRegistrarComida: () -> Unit = {},
    onRegistrarAviso: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    val primerNombre = if (estado is UiState.Exito) estado.datos.nombre.substringBefore(" ") else null

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 1, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = if (primerNombre != null) "Perfil de $primerNombre" else "Perfil",
                onAtras = onAtras
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (estado) {
                    UiState.Cargando -> Box(Modifier.fillMaxWidth().height(200.dp)) { EstadoCargando() }

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin registros",
                        subtitulo = "Aún no hay información de este niño"
                    )

                    is UiState.Error -> Box(Modifier.fillMaxWidth().height(320.dp)) {
                        EstadoError(
                            titulo = "Error al cargar perfil",
                            mensaje = "No pudimos cargar la información",
                            onReintentar = onReintentar
                        )
                    }

                    is UiState.Exito -> {
                        val nino = estado.datos
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AvatarCircular(tamano = 72.dp, imagenRes = nino.fotoRes)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = nino.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChipAsistencia(estado = nino.asistencia)
                                    Text(
                                        text = nino.sala,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoGris
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(BordeCampo))
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "Adulto de ${nino.nombre.substringBefore(" ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextoGris
                                        )
                                        Text(
                                            text = nombreAdulto,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextoOscuro
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = "Llamar",
                                        tint = Coral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Acciones de Registro",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemAccionRegistro(
                        texto = "Abrir Chat con ${nombreAdulto.substringBefore(" ")}",
                        icono = Icons.Outlined.ChatBubbleOutline,
                        colorIcono = Coral,
                        colorFondo = CoralSuave,
                        iconoFinal = Icons.Outlined.ChevronRight,
                        onClick = onAbrirChat
                    )
                    ItemAccionRegistro(
                        texto = "Registrar Actividad",
                        icono = Icons.Outlined.Extension,
                        colorIcono = Amarillo,
                        colorFondo = AmarilloSuave,
                        onClick = onRegistrarActividad
                    )
                    ItemAccionRegistro(
                        texto = "Registrar Comida",
                        icono = Icons.Outlined.Restaurant,
                        colorIcono = Verde,
                        colorFondo = VerdeSuave,
                        onClick = onRegistrarComida
                    )
                    ItemAccionRegistro(
                        texto = "Registrar Aviso",
                        icono = Icons.Outlined.Campaign,
                        colorIcono = Coral,
                        colorFondo = CoralSuave,
                        onClick = onRegistrarAviso
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PerfilNinoPreview() {
    SonrisasTheme {
        PerfilNinoScreen(estado = UiState.Exito(Mocks.ninos.first()))
    }
}