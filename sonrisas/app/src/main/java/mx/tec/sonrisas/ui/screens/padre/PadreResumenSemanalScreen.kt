package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.WeeklySummary
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun PadreResumenSemanalScreen(
    resumen: WeeklySummary,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()

    var seccionActual by remember { mutableIntStateOf(0) }
    val secciones = listOf("Asistencia", "Nutrición", "Actividades", "Descanso", "Evaluación")

    Scaffold(
        modifier = modifier,
        containerColor = Fondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopBarAtras(
                titulo = "Reporte Semanal de Desarrollo",
                subtitulo = "Alumno/a: ${resumen.childName}",
                onAtras = onCerrar
            )

            // Pestañas formales de navegación entre secciones
            TabRow(
                selectedTabIndex = seccionActual,
                containerColor = Color.White,
                contentColor = Coral
            ) {
                secciones.forEachIndexed { index, titulo ->
                    Tab(
                        selected = seccionActual == index,
                        onClick = { seccionActual = index },
                        text = {
                            Text(
                                text = titulo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (seccionActual == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (seccionActual == index) Coral else TextoGris
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (seccionActual) {
                    0 -> SeccionAsistencia(resumen)
                    1 -> SeccionNutricion(resumen)
                    2 -> SeccionActividades(resumen)
                    3 -> SeccionDescanso(resumen)
                    4 -> SeccionEvaluacionDocente(resumen)
                }

                // Control formal para navegar entre secciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (seccionActual > 0) {
                        OutlinedButton(
                            onClick = { seccionActual-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anterior")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (seccionActual < secciones.size - 1) {
                        Button(
                            onClick = { seccionActual++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                        ) {
                            Text("Siguiente")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = onCerrar,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                        ) {
                            Text("Finalizar reporte")
                        }
                    }
                }
            }
        }
    }
}

// --- SECCIÓN 1: ASISTENCIA Y PUNTUALIDAD ---
@Composable
private fun SeccionAsistencia(resumen: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Verde,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Asistencia y Puntualidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Registro correspondiente a la semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VerdeSuave, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "${resumen.daysAttended} de ${resumen.totalDays} Días Asistidos (100% de cumplimiento)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "El alumno asistió puntualmente durante todas las jornadas hábiles programadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoGris
                    )
                }
            }
        }
    }
}

// --- SECCIÓN 2: NUTRICIÓN Y ALIMENTACIÓN ---
@Composable
private fun SeccionNutricion(resumen: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Alimentación y Nutrición",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Monitoreo de alimentos servidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "• Registros de alimentación completados: ${resumen.mealsEaten}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CoralSuave, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Observación de aceptación de alimentos:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = resumen.favoriteMealNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoOscuro
                    )
                }
            }
        }
    }
}

// --- SECCIÓN 3: ACTIVIDADES PEDAGÓGICAS ---
@Composable
private fun SeccionActividades(resumen: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Actividades Pedagógicas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "${resumen.totalActivitiesCompleted} actividades registradas esta semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                resumen.topActivities.forEach { actividad ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Fondo, RoundedCornerShape(10.dp))
                            .border(1.dp, BordeCampo, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $actividad",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoOscuro
                        )
                    }
                }
            }
        }
    }
}

// --- SECCIÓN 4: DESCANSO Y CONDUCTA ---
@Composable
private fun SeccionDescanso(resumen: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Nightlight,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Descanso y Estado de Ánimo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Bienestar y hábitos de reposo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "• Tiempo promedio de siesta: ${resumen.averageNapMinutes} minutos diarios",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = "• Disposición y conducta general:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoGris
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmarilloSuave, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = resumen.predominantMood,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
            }
        }
    }
}

// --- SECCIÓN 5: EVALUACIÓN Y FIRMA DOCENTE ---
@Composable
private fun SeccionEvaluacionDocente(resumen: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarCircular(tamano = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Evaluación de la Docente Titular",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Maestra Laura · Prescolares B",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Fondo, RoundedCornerShape(12.dp))
                    .border(1.dp, BordeCampo, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "\"${resumen.teacherComment}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoOscuro,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { /* Compartir o guardar en PDF */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir o descargar reporte")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PadreResumenSemanalPreview() {
    SonrisasTheme {
        PadreResumenSemanalScreen(
            resumen = WeeklySummary(childId = "1", childName = "Sofía Martínez"),
            onCerrar = {}
        )
    }
}
