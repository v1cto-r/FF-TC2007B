package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.AmountEnum
import mx.tec.sonrisas.domain.Feeding
import mx.tec.sonrisas.domain.Stay
import mx.tec.sonrisas.domain.etiqueta
import mx.tec.sonrisas.ui.components.CampoBooleano
import mx.tec.sonrisas.ui.components.CampoBooleanoOpcional
import mx.tec.sonrisas.ui.components.CampoContador
import mx.tec.sonrisas.ui.components.CampoEntero
import mx.tec.sonrisas.ui.components.CampoOpciones
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo
import mx.tec.sonrisas.ui.theme.TextoOscuro

@Composable
fun StayFormScreen(
    stayExistente: Stay?,
    hayIngreso: Boolean,
    onAtras: () -> Unit,
    onGuardar: (Stay) -> Unit,
    onIrACheckIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FlagSecureEnable()
    // Claves en `remember(stayExistente)`: los datos de hoy llegan de Room de forma
    // asíncrona (empiezan en null mientras carga), así que los campos deben
    // reinicializarse cuando ese valor cambie, no solo en la primera composición.
    var breakfast by remember(stayExistente) { mutableStateOf(stayExistente?.feeding?.breakfast ?: AmountEnum.NONE) }
    var snack by remember(stayExistente) { mutableStateOf(stayExistente?.feeding?.snack ?: AmountEnum.NONE) }
    var lunch by remember(stayExistente) { mutableStateOf(stayExistente?.feeding?.lunch ?: AmountEnum.NONE) }
    var feedingNotes by remember(stayExistente) { mutableStateOf(stayExistente?.feedingNotes ?: "") }
    var slept by remember(stayExistente) { mutableStateOf(stayExistente?.slept ?: false) }
    var napMinutes by remember(stayExistente) { mutableStateOf(stayExistente?.napMinutes ?: 0) }
    var restComments by remember(stayExistente) { mutableStateOf(stayExistente?.restComments ?: "") }
    var toiletingNotice by remember(stayExistente) { mutableStateOf(stayExistente?.toiletingNotice) }
    var peeCount by remember(stayExistente) { mutableStateOf(stayExistente?.peeCount ?: 0) }
    var poopCount by remember(stayExistente) { mutableStateOf(stayExistente?.poopCount ?: 0) }
    var toiletingNotes by remember(stayExistente) { mutableStateOf(stayExistente?.toiletingNotes ?: "") }
    var mood by remember(stayExistente) { mutableStateOf(stayExistente?.mood ?: "") }
    var cried by remember(stayExistente) { mutableStateOf(stayExistente?.cried ?: false) }
    var fought by remember(stayExistente) { mutableStateOf(stayExistente?.fought ?: false) }
    var participated by remember(stayExistente) { mutableStateOf(stayExistente?.participated ?: false) }
    var moodNotes by remember(stayExistente) { mutableStateOf(stayExistente?.moodNotes ?: "") }
    var hadAccident by remember(stayExistente) { mutableStateOf(stayExistente?.hadAccident ?: false) }
    var accidentDescription by remember(stayExistente) { mutableStateOf(stayExistente?.accidentDescription ?: "") }
    var accidentReportFolio by remember(stayExistente) { mutableStateOf(stayExistente?.accidentReportFolio ?: "") }
    var healthIssue by remember(stayExistente) { mutableStateOf(stayExistente?.healthIssue ?: false) }
    var healthIssueDescription by remember(stayExistente) { mutableStateOf(stayExistente?.healthIssueDescription ?: "") }
    var careProvided by remember(stayExistente) { mutableStateOf(stayExistente?.careProvided ?: "") }
    var generalHealthNotes by remember(stayExistente) { mutableStateOf(stayExistente?.generalHealthNotes ?: "") }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Registrar Estadía", onAtras = onAtras)

            if (!hayIngreso) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    EstadoVacio(
                        titulo = "Registra el ingreso primero",
                        subtitulo = "Antes de capturar la estadía, este niño necesita un registro de ingreso hoy"
                    )
                    Button(
                        onClick = onIrACheckIn,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                    ) {
                        Text("Ir a Registrar Asistencia")
                    }
                }
                return@Scaffold
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Titulo("Alimentación")
                CampoOpciones("Desayuno", breakfast, AmountEnum.entries, { breakfast = it }, { it.etiqueta() })
                CampoOpciones("Colación", snack, AmountEnum.entries, { snack = it }, { it.etiqueta() })
                CampoOpciones("Comida", lunch, AmountEnum.entries, { lunch = it }, { it.etiqueta() })
                CampoTexto("Observaciones de alimentación", feedingNotes, { feedingNotes = it })

                Titulo("Descanso")
                CampoBooleano("¿Durmió?", slept, { slept = it })
                if (slept) CampoEntero("Minutos de siesta", napMinutes, { napMinutes = it })
                CampoTexto("Comentarios de descanso", restComments, { restComments = it })

                Titulo("Baño")
                CampoBooleanoOpcional("Aviso de esfínteres", toiletingNotice, { toiletingNotice = it })
                CampoContador("Veces que hizo pipí", peeCount, { peeCount = it })
                CampoContador("Veces que hizo popó", poopCount, { poopCount = it })
                CampoTexto("Observaciones excretoras", toiletingNotes, { toiletingNotes = it })

                Titulo("Ánimo")
                CampoTexto("Estado de ánimo", mood, { mood = it })
                CampoBooleano("Lloró", cried, { cried = it })
                CampoBooleano("Peleó", fought, { fought = it })
                CampoBooleano("Participó", participated, { participated = it })
                CampoTexto("Observaciones de ánimo", moodNotes, { moodNotes = it })

                Titulo("Salud")
                CampoBooleano("¿Tuvo accidente?", hadAccident, { hadAccident = it })
                if (hadAccident) {
                    CampoTexto("Descripción del accidente", accidentDescription, { accidentDescription = it })
                    CampoTexto("Folio del reporte", accidentReportFolio, { accidentReportFolio = it })
                }
                CampoBooleano("¿Problema de salud?", healthIssue, { healthIssue = it })
                if (healthIssue) {
                    CampoTexto("¿Cuál problema?", healthIssueDescription, { healthIssueDescription = it })
                    CampoTexto("Atención proporcionada", careProvided, { careProvided = it })
                }
                CampoTexto("Observaciones generales de salud", generalHealthNotes, { generalHealthNotes = it })

                Button(
                    onClick = {
                        onGuardar(
                            Stay(
                                feeding = Feeding(breakfast, snack, lunch),
                                feedingNotes = feedingNotes.ifBlank { null },
                                slept = slept,
                                napMinutes = if (slept) napMinutes else null,
                                restComments = restComments.ifBlank { null },
                                toiletingNotice = toiletingNotice,
                                peeCount = peeCount,
                                poopCount = poopCount,
                                toiletingNotes = toiletingNotes.ifBlank { null },
                                mood = mood,
                                cried = cried,
                                fought = fought,
                                participated = participated,
                                moodNotes = moodNotes.ifBlank { null },
                                hadAccident = hadAccident,
                                accidentDescription = accidentDescription.ifBlank { null },
                                accidentReportFolio = accidentReportFolio.ifBlank { null },
                                healthIssue = healthIssue,
                                healthIssueDescription = healthIssueDescription.ifBlank { null },
                                careProvided = careProvided.ifBlank { null },
                                generalHealthNotes = generalHealthNotes.ifBlank { null }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar Estadía")
                }
            }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = TextoOscuro
    )
}
