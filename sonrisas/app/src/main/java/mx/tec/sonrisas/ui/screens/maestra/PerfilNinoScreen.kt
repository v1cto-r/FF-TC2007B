package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.ChipAsistencia
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.ItemAccionRegistro
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.NinoConEstado
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun PerfilNinoScreen(
    estado: UiState<NinoConEstado>,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onAbrirChat: () -> Unit = {},
    onCheckIn: () -> Unit = {},
    onStay: () -> Unit = {},
    onActividad: () -> Unit = {},
    onCheckOut: () -> Unit = {},
    onCancelarSalida: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {

    val primerNombre = if (estado is UiState.Exito) estado.datos.child.fullName.substringBefore(" ") else null

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
                        val nino = estado.datos.child
                        val autorizacionHoy = estado.datos.autorizacionHoy
                        val logHoy = estado.datos.dailyLog

                        val nombreTutor = when {
                            nino.parentName.isNotBlank() && nino.parentName != "Padre / Tutor" -> nino.parentName
                            autorizacionHoy != null -> "${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship})"
                            logHoy?.checkIn?.droppedOffByName?.isNotBlank() == true -> logHoy.checkIn.droppedOffByName
                            logHoy?.checkOut?.pickedUpByName?.isNotBlank() == true -> logHoy.checkOut.pickedUpByName
                            else -> "Sin tutor registrado"
                        }

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
                                AvatarCircular(tamano = 72.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = nino.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChipAsistencia(presente = estado.datos.presente)
                                    Text(
                                        text = "${nino.daycareCenter} · ${nino.group.etiqueta()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoGris
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(BordeCampo))
                                Spacer(Modifier.height(10.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = "Padre / Tutor Registrado",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextoGris
                                            )
                                            Text(
                                                text = nombreTutor,
                                                style = MaterialTheme.typography.bodyMedium,
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

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = "Cumpleaños / Fecha de Nacimiento",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextoGris
                                            )
                                            Text(
                                                text = nino.birthdate,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextoOscuro
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = "Alergias o Cuidados Especiales",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextoGris
                                            )
                                            Text(
                                                text = nino.allergies ?: "Sin alergias registradas",
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
                }

                if (estado is UiState.Exito) {
                    val nino = estado.datos.child
                    val autorizacionHoy = estado.datos.autorizacionHoy
                    val logHoy = estado.datos.dailyLog

                    val nombreTutor = when {
                        nino.parentName.isNotBlank() && nino.parentName != "Padre / Tutor" -> nino.parentName
                        autorizacionHoy != null -> "${autorizacionHoy.authorizedPersonName} (${autorizacionHoy.relationship})"
                        logHoy?.checkIn?.droppedOffByName?.isNotBlank() == true -> logHoy.checkIn.droppedOffByName
                        logHoy?.checkOut?.pickedUpByName?.isNotBlank() == true -> logHoy.checkOut.pickedUpByName
                        else -> "Sin tutor registrado"
                    }

                    val terminado = estado.datos.dailyLog?.checkOut != null
                    Spacer(Modifier.height(20.dp))
                    if (terminado) {
                        Text(
                            text = "Resumen del día",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    text = "${estado.datos.child.fullName.substringBefore(" ")} tuvo un gran día",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Spacer(Modifier.height(10.dp))
                                if (estado.datos.activities.isEmpty()) {
                                    Text(
                                        text = "Sin actividades registradas hoy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoGris
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        estado.datos.activities.forEach { actividad ->
                                            Text(
                                                text = "• ${actividad.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextoOscuro
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCancelarSalida,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Coral),
                            colors = ButtonDefaults.buttonColors(contentColor = Coral)
                        ) {
                            Text("Cancelar salida")
                        }
                    } else {
                        Text(
                            text = "Acciones de Registro",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ItemAccionRegistro(
                                texto = if (nombreTutor != "Sin tutor registrado") "Abrir Chat con ${nombreTutor.substringBefore(" ")}" else "Abrir Chat",
                                icono = Icons.Outlined.ChatBubbleOutline,
                                colorIcono = Coral,
                                colorFondo = CoralSuave,
                                iconoFinal = Icons.Outlined.ChevronRight,
                                onClick = onAbrirChat
                            )
                            if (!estado.datos.presente) {
                                ItemAccionRegistro(
                                    texto = "Registrar Asistencia",
                                    icono = Icons.Outlined.ChevronRight,
                                    colorIcono = Verde,
                                    colorFondo = VerdeSuave,
                                    onClick = onCheckIn
                                )
                            } else {
                                ItemAccionRegistro(
                                    texto = "Registrar Estadía",
                                    icono = Icons.Outlined.Restaurant,
                                    colorIcono = Verde,
                                    colorFondo = VerdeSuave,
                                    onClick = onStay
                                )
                                ItemAccionRegistro(
                                    texto = "Registrar Actividad",
                                    icono = Icons.Outlined.Extension,
                                    colorIcono = Amarillo,
                                    colorFondo = AmarilloSuave,
                                    onClick = onActividad
                                )
                                ItemAccionRegistro(
                                    texto = "Registrar Salida",
                                    icono = Icons.Outlined.Campaign,
                                    colorIcono = Coral,
                                    colorFondo = CoralSuave,
                                    onClick = onCheckOut
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PerfilNinoPreview() {
    SonrisasTheme {
        PerfilNinoScreen(
            estado = UiState.Exito(
                NinoConEstado(Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"), true)
            )
        )
    }
}
