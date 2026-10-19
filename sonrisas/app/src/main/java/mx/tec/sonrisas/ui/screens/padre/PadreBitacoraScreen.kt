package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Restaurant
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
                    onAbrirPerfil = { onAbrirPerfilNino(it) }
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
                if (esDomingoOSinDatos) {
                    EstadoVacio(
                        titulo = "Sin registros acumulados esta semana",
                        subtitulo = "La bitácora semanal se reinició el domingo. Los registros comenzarán a acumularse nuevamente a partir del lunes.",
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    // Tarjeta 1: Asistencia
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Verde, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Asistencia y Puntualidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VerdeSuave, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${resumen.daysAttended} de ${resumen.totalDays} Días Asistidos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                            }
                        }
                    }

                    // Tarjeta 2: Alimentación Acumulada
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Alimentación Registrada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralSuave, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = resumen.favoriteMealNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoOscuro
                                )
                            }
                        }
                    }

                    // Tarjeta 3: Actividades Acumuladas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Extension, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Actividades Registradas (${resumen.totalActivitiesCompleted})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                            }
                            Spacer(Modifier.height(10.dp))
                            if (resumen.topActivities.isEmpty()) {
                                Text(
                                    text = "Sin actividades registradas por la maestra esta semana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoGris
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    resumen.topActivities.forEach { actividad ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Fondo, RoundedCornerShape(8.dp))
                                                .border(1.dp, BordeCampo, RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(text = "• $actividad", style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta 4: Descanso y Conducta
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Nightlight, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Descanso y Estado de Ánimo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "• Promedio de siesta diaria: ${resumen.averageNapMinutes} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoOscuro
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AmarilloSuave, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Ánimo: ${resumen.predominantMood}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                            }
                        }
                    }

                    // Tarjeta 5: Observaciones de la Docente
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Observaciones de la Maestra", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Fondo, RoundedCornerShape(10.dp))
                                    .border(1.dp, BordeCampo, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "\"${resumen.teacherComment}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoOscuro
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

// Selector con comportamiento inteligente idéntico al de Inicio
@Composable
private fun SelectorNinoHeader(
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
