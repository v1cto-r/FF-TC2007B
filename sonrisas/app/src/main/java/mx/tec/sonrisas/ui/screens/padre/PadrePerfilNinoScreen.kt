package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.etiqueta
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun PadrePerfilNinoScreen(
    child: Child,
    onAtras: () -> Unit,
    onSolicitarCambio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()

    var mostrarDialogoCambio by remember { mutableStateOf(false) }
    var textoSolicitud by remember { mutableStateOf("") }
    var solicitudEnviada by remember { mutableStateOf(false) }

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
                titulo = "Información de mi hij@",
                subtitulo = child.fullName,
                onAtras = onAtras
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta 1: Resumen del Alumno
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AvatarCircular(tamano = 72.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = child.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${child.daycareCenter} · ${child.group.etiqueta()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoGris
                        )
                    }
                }

                // Tarjeta 2: Información Personal y de Salud
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Datos Personales y de Salud",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )

                        // Cumpleaños
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Cake, contentDescription = null, tint = Coral, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Fecha de nacimiento / Cumpleaños", style = MaterialTheme.typography.bodySmall, color = TextoGris)
                                Text(child.birthdate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
                            }
                        }

                        // Alergias
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MedicalInformation, contentDescription = null, tint = Coral, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Alergias o Cuidados Especiales", style = MaterialTheme.typography.bodySmall, color = TextoGris)
                                Text(child.allergies ?: "Ninguna registrada", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
                            }
                        }
                    }
                }

                // Tarjeta 3: Solicitud de Actualización
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "¿Deseas corregir o actualizar un dato?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Envía una solicitud a la maestra para solicitar el cambio de cumpleaños o alergias.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                        Spacer(Modifier.height(14.dp))

                        if (solicitudEnviada) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VerdeSuave, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "✓ Solicitud de cambio enviada correctamente a la docente.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                            }
                        } else {
                            Button(
                                onClick = { mostrarDialogoCambio = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Solicitar cambio de información")
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCambio) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCambio = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Solicitar actualización de datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Escribe el dato que deseas cambiar (ejemplo: 'Alergia a penicilina' o 'Cambiar fecha de nacimiento a 15/05/2021'):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoGris
                    )
                    CampoTexto(
                        etiqueta = "Detalle del cambio",
                        valor = textoSolicitud,
                        onValorChange = { textoSolicitud = it },
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textoSolicitud.isNotBlank()) {
                            onSolicitarCambio("Solicitud de actualización de datos para ${child.fullName}: $textoSolicitud")
                            mostrarDialogoCambio = false
                            solicitudEnviada = true
                            textoSolicitud = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Enviar Solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCambio = false }) {
                    Text("Cancelar", color = TextoGris)
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PadrePerfilNinoPreview() {
    SonrisasTheme {
        PadrePerfilNinoScreen(
            child = Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"),
            onAtras = {},
            onSolicitarCambio = {}
        )
    }
}
