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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalTime
import mx.tec.sonrisas.data.StoredPhoto
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.Injury
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.ui.components.CampoBooleano
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.rememberTomarFoto
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.CoralSuave
import mx.tec.sonrisas.ui.theme.Fondo
import mx.tec.sonrisas.ui.theme.TextoGris
import mx.tec.sonrisas.ui.theme.TextoOscuro

@Composable
fun CheckOutFormScreen(
    nombreMaestra: String,
    onAtras: () -> Unit,
    onGuardar: (CheckOutRecord, PickupAuthorization?) -> Unit,
    autorizacionHoy: PickupAuthorization? = null,
    fotoIdentificacion: StoredPhoto? = null,
    leyendoFoto: Boolean = false,
    error: String? = null,
    onTomarFoto: (Uri) -> Unit = {},
    onDescartarFoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pickedUpByName by remember { mutableStateOf("") }
    // The child leaves with the person the parent authorized: their ID photo is then required
    var recogeAutorizado by remember { mutableStateOf(false) }
    val tomarFoto = rememberTomarFoto(onTomarFoto)
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
                            if (!recogeAutorizado) {
                                Button(
                                    onClick = {
                                        recogeAutorizado = true
                                        pickedUpByName = "${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship})"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                                ) {
                                    Text("Lo recoge la persona autorizada", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                FotoIdentificacion(
                                    foto = fotoIdentificacion,
                                    leyendo = leyendoFoto,
                                    tipoIdentificacion = autorizacionHoy.identificationType,
                                    onTomar = tomarFoto
                                )
                                OutlinedButton(
                                    onClick = {
                                        recogeAutorizado = false
                                        pickedUpByName = ""
                                        onDescartarFoto()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("No es la persona autorizada", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (!recogeAutorizado) {
                    CampoTexto("Quién recoge", pickedUpByName, { pickedUpByName = it })
                }
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
                            ),
                            autorizacionHoy.takeIf { recogeAutorizado }
                        )
                    },
                    enabled = pickedUpByName.isNotBlank() && !leyendoFoto,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar Salida")
                }
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = Coral)
                }
            }
        }
    }
}

@Composable
private fun FotoIdentificacion(foto: StoredPhoto?, leyendo: Boolean, tipoIdentificacion: String, onTomar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Toma una foto de su identificación ($tipoIdentificacion)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        if (foto != null) {
            AsyncImage(
                model = File(foto.path),
                contentDescription = "Identificación de quien recoge",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp))
            )
        }
        Button(
            onClick = onTomar,
            enabled = !leyendo,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
        ) {
            Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = when {
                    leyendo -> "Procesando foto…"
                    foto == null -> "Tomar foto de la identificación"
                    else -> "Tomar otra foto"
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
