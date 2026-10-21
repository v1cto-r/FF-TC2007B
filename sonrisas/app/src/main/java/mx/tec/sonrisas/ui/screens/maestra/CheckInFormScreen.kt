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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import mx.tec.sonrisas.domain.CheckInRecord
import mx.tec.sonrisas.domain.Injury
import mx.tec.sonrisas.ui.components.CampoBooleano
import mx.tec.sonrisas.ui.components.CampoBooleanoOpcional
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo

@Composable
fun CheckInFormScreen(
    nombreMaestra: String,
    onAtras: () -> Unit,
    onGuardar: (CheckInRecord) -> Unit,
    checkInExistente: CheckInRecord? = null,
    modifier: Modifier = Modifier
) {
    var droppedOffByName by remember { mutableStateOf(checkInExistente?.droppedOffByName ?: "") }
    var isSick by remember { mutableStateOf(checkInExistente?.isSick ?: false) }
    var medicamento by remember { mutableStateOf(checkInExistente?.medicationLeftWithPrescription) }
    var arrivesWithSymptoms by remember { mutableStateOf(checkInExistente?.arrivesWithSymptoms ?: false) }
    var symptomsDescription by remember { mutableStateOf(checkInExistente?.symptomsDescription ?: "") }
    var arrivedClean by remember { mutableStateOf(checkInExistente?.arrivedClean ?: true) }
    var broughtCompleteBackpack by remember { mutableStateOf(checkInExistente?.broughtCompleteBackpack ?: true) }
    var goodPhysicalCondition by remember { mutableStateOf(checkInExistente?.goodPhysicalCondition ?: true) }
    var hayLesion by remember { mutableStateOf(checkInExistente?.injury != null) }
    var lesionParte by remember { mutableStateOf(checkInExistente?.injury?.bodyPart ?: "") }
    var lesionDescripcion by remember { mutableStateOf(checkInExistente?.injury?.description ?: "") }
    var parentsNotes by remember { mutableStateOf(checkInExistente?.parentsNotes ?: "") }
    FlagSecureDisable()

    LaunchedEffect(checkInExistente) {
        if (checkInExistente != null) {
            droppedOffByName = checkInExistente.droppedOffByName
            isSick = checkInExistente.isSick
            medicamento = checkInExistente.medicationLeftWithPrescription
            arrivesWithSymptoms = checkInExistente.arrivesWithSymptoms
            symptomsDescription = checkInExistente.symptomsDescription ?: ""
            arrivedClean = checkInExistente.arrivedClean
            broughtCompleteBackpack = checkInExistente.broughtCompleteBackpack
            goodPhysicalCondition = checkInExistente.goodPhysicalCondition
            hayLesion = checkInExistente.injury != null
            lesionParte = checkInExistente.injury?.bodyPart ?: ""
            lesionDescripcion = checkInExistente.injury?.description ?: ""
            parentsNotes = checkInExistente.parentsNotes ?: ""
        }
    }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Registrar Asistencia", onAtras = onAtras)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampoTexto("Quién entrega", droppedOffByName, { droppedOffByName = it })
                CampoBooleano("¿Llega enfermo?", isSick, { isSick = it })
                CampoBooleanoOpcional("Medicamento dejado con receta", medicamento, { medicamento = it })
                CampoBooleano("¿Llega con síntomas?", arrivesWithSymptoms, { arrivesWithSymptoms = it })
                if (arrivesWithSymptoms) {
                    CampoTexto("¿Cuáles síntomas?", symptomsDescription, { symptomsDescription = it })
                }
                CampoBooleano("Se presentó limpio", arrivedClean, { arrivedClean = it })
                CampoBooleano("Trajo mochila completa", broughtCompleteBackpack, { broughtCompleteBackpack = it })
                CampoBooleano("Buen estado físico", goodPhysicalCondition, { goodPhysicalCondition = it })
                CampoBooleano("¿Tiene alguna lesión?", hayLesion, { hayLesion = it })
                if (hayLesion) {
                    CampoTexto("Parte del cuerpo", lesionParte, { lesionParte = it })
                    CampoTexto("Descripción de la lesión", lesionDescripcion, { lesionDescripcion = it })
                }
                CampoTexto("Observaciones de los padres", parentsNotes, { parentsNotes = it }, singleLine = false)

                Button(
                    onClick = {
                        onGuardar(
                            CheckInRecord(
                                checkInTime = checkInExistente?.checkInTime ?: LocalTime.now(),
                                droppedOffByName = droppedOffByName.trim(),
                                receivedByStaffName = nombreMaestra,
                                isSick = isSick,
                                medicationLeftWithPrescription = medicamento,
                                arrivesWithSymptoms = arrivesWithSymptoms,
                                symptomsDescription = symptomsDescription.ifBlank { null },
                                arrivedClean = arrivedClean,
                                broughtCompleteBackpack = broughtCompleteBackpack,
                                goodPhysicalCondition = goodPhysicalCondition,
                                injury = if (hayLesion) Injury(lesionParte, lesionDescripcion) else null,
                                parentsNotes = parentsNotes.ifBlank { null }
                            )
                        )
                    },
                    // The server rejects a check-in without who dropped the child off
                    enabled = droppedOffByName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar Asistencia")
                }
            }
        }
    }
}
