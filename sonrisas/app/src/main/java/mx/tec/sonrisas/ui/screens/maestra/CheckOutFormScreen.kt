package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.Injury
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.ui.components.CampoBooleano
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.CoralSuave
import mx.tec.sonrisas.ui.theme.Fondo
import mx.tec.sonrisas.ui.theme.TextoGris
import mx.tec.sonrisas.ui.theme.TextoOscuro

@Composable
fun CheckOutFormScreen(
    nombreMaestra: String,
    onAtras: () -> Unit,
    onGuardar: (CheckOutRecord) -> Unit,
    autorizacionHoy: PickupAuthorization? = null,
    modifier: Modifier = Modifier
) {
    var pickedUpByName by remember { mutableStateOf("") }
    var releasedInGoodPhysicalCondition by remember { mutableStateOf(true) }
    var releasedClean by remember { mutableStateOf(true) }
    var belongingsComplete by remember { mutableStateOf(true) }
    var hayLesion by remember { mutableStateOf(false) }
    var lesionParte by remember { mutableStateOf("") }
    var lesionDescripcion by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    FlagSecureDisable()

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Registrar Salida", onAtras = onAtras)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (autorizacionHoy != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CoralSuave)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.HowToReg,
                                    contentDescription = null,
                                    tint = Coral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "📌 Tercero Autorizado por los Padres hoy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "• Nombre: ${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoOscuro
                            )
                            Text(
                                text = "• Teléfono: ${autorizacionHoy.phone} | ID: ${autorizacionHoy.identificationType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoOscuro
                            )
                            if (!autorizacionHoy.notes.isNullOrBlank()) {
                                Text(
                                    text = "• Observaciones: ${autorizacionHoy.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoGris
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    pickedUpByName = "${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship} - ${autorizacionHoy.identificationType})"
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                            ) {
                                Text("Cargar datos de persona autorizada", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                CampoTexto("Quién recoge", pickedUpByName, { pickedUpByName = it })
                CampoBooleano("Buen estado físico al salir", releasedInGoodPhysicalCondition, { releasedInGoodPhysicalCondition = it })
                CampoBooleano("Entregado limpio", releasedClean, { releasedClean = it })
                CampoBooleano("Pertenencias completas", belongingsComplete, { belongingsComplete = it })
                CampoBooleano("¿Tiene alguna lesión?", hayLesion, { hayLesion = it })
                if (hayLesion) {
                    CampoTexto("Parte del cuerpo", lesionParte, { lesionParte = it })
                    CampoTexto("Descripción de la lesión", lesionDescripcion, { lesionDescripcion = it })
                }
                CampoTexto("Observaciones", notes, { notes = it }, singleLine = false)

                Button(
                    onClick = {
                        onGuardar(
                            CheckOutRecord(
                                checkOutTime = LocalTime.now(),
                                releasedInGoodPhysicalCondition = releasedInGoodPhysicalCondition,
                                injury = if (hayLesion) Injury(lesionParte, lesionDescripcion) else null,
                                releasedClean = releasedClean,
                                belongingsComplete = belongingsComplete,
                                notes = notes.ifBlank { null },
                                pickedUpByName = pickedUpByName,
                                releasedByStaffName = nombreMaestra
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar Salida")
                }
            }
        }
    }
}
