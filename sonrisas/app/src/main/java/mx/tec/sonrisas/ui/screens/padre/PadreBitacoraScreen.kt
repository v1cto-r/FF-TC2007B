package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import mx.tec.sonrisas.domain.WeeklySummary
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TabsPadre
import mx.tec.sonrisas.ui.theme.*

@Composable
fun PadreBitacoraScreen(
    resumen: WeeklySummary,
    ninos: List<String>,
    ninoSeleccionado: Int,
    onSeleccionarNino: (Int) -> Unit,
    onAbrirPerfilNino: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    onCerrarSesion: (() -> Unit)? = null,
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()

    val esDomingoOSinDatos = resumen.daysAttended == 0 && resumen.totalActivitiesCompleted == 0

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsPadre, seleccionado = 1, onSeleccionar = onTab) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header con selector de hijo/a idéntico al Inicio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Bitácora Semanal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Resumen acumulado de la semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
                SelectorNinoHeader(
                    ninos = ninos,
                    seleccionado = ninoSeleccionado,
                    onSeleccionar = onSeleccionarNino,
                    onAbrirPerfil = { onAbrirPerfilNino(it) },
                    onCerrarSesion = onCerrarSesion
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta Principal de Resumen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = resumen.childName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Text(
                                    text = resumen.weekLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoGris
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(VerdeSuave, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${resumen.daysAttended} de ${resumen.totalDays} días",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Verde
                                )
                            }
                        }

                        if (esDomingoOSinDatos) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AmarilloSuave, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Aún no hay registros suficientes esta semana. ¡El resumen se actualizará conforme la maestra registre la jornada!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoOscuro
                                )
                            }
                        } else {
                            // Barra de progreso de asistencia
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Asistencia escolar", style = MaterialTheme.typography.bodySmall, color = TextoGris)
                                    Text("${(resumen.daysAttended.toFloat() / resumen.totalDays * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                                }
                                LinearProgressIndicator(
                                    progress = { resumen.daysAttended.toFloat() / resumen.totalDays.coerceAtLeast(1) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Verde,
                                    trackColor = VerdeSuave,
                                )
                            }

                            // Estadísticas rápidas en fila
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    titulo = "Actividades",
                                    valor = "${resumen.totalActivitiesCompleted}",
                                    subtitulo = "Completadas",
                                    colorFondo = VerdeSuave,
                                    colorTexto = Verde,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    titulo = "Siesta",
                                    valor = "${resumen.averageNapMinutes} min",
                                    subtitulo = "Promedio diario",
                                    colorFondo = AmarilloSuave,
                                    colorTexto = Amarillo,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    titulo = "Comidas",
                                    valor = "${resumen.mealsEaten}",
                                    subtitulo = "Registradas",
                                    colorFondo = CoralSuave,
                                    colorTexto = Coral,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Comentario de la Maestra
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircular(tamano = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Nota de la Maestra",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Text(
                                    text = "Valoración semanal general",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextoGris
                                )
                            }
                        }
                        Text(
                            text = "\"${resumen.teacherComment}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoOscuro
                        )
                    }
                }

                // Detalles adicionales
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Detalles de la semana",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )

                        DetalleFila(etiqueta = "Ánimo predominante", valor = resumen.predominantMood)
                        DetalleFila(etiqueta = "Alimentación destacada", valor = resumen.favoriteMealNote)
                        DetalleFila(etiqueta = "Top actividades", valor = resumen.topActivities.joinToString(", "))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = titulo, style = MaterialTheme.typography.labelSmall, color = colorTexto, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = valor, style = MaterialTheme.typography.titleMedium, color = TextoOscuro, fontWeight = FontWeight.Bold)
            Text(text = subtitulo, style = MaterialTheme.typography.labelSmall, color = TextoGris)
        }
    }
}

@Composable
private fun DetalleFila(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = TextoGris)
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}

// Selector con menú desplegable idéntico al de Inicio
@Composable
private fun SelectorNinoHeader(
    ninos: List<String>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit,
    onAbrirPerfil: (Int) -> Unit = {},
    onCerrarSesion: (() -> Unit)? = null
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
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Cambiar niño",
                tint = TextoGris,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        ) {
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

            if (onCerrarSesion != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BordeCampo)
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", style = MaterialTheme.typography.bodyMedium, color = Coral, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        abierto = false
                        onCerrarSesion()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Coral
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PadreBitacoraPreview() {
    SonrisasTheme {
        PadreBitacoraScreen(
            resumen = WeeklySummary(childId = "1", childName = "Sofía Martínez"),
            ninos = listOf("Sofía", "Mateo"),
            ninoSeleccionado = 0,
            onSeleccionarNino = {}
        )
    }
}
