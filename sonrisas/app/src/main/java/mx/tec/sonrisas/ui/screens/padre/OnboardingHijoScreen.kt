package mx.tec.sonrisas.ui.screens.padre

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.SelectorFuenteFotoDialog
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingHijoScreen(
    child: Child,
    catalogoAlergias: UiState<List<String>>,
    onGuardarInformacion: (birthdate: String, allergies: List<String>, actaUri: Uri?) -> Unit,
    modifier: Modifier = Modifier,
    onReintentarCatalogo: () -> Unit = {}
) {
    FlagSecureDisable()

    // Solo una fecha ISO sirve para el servidor: "Sin fecha registrada" o el valor por defecto de Child
    // significan que el padre todavía tiene que elegirla
    var birthdate by remember(child.id, child.birthdate) {
        mutableStateOf(child.birthdate.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }.orEmpty())
    }
    // Solo nombres del catálogo: así el admin puede cruzarlos con los alérgenos del menú semanal
    var alergias by remember(child.id) { mutableStateOf(setOf<String>()) }
    var sinAlergias by remember(child.id) { mutableStateOf(false) }
    var actaUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var errorValidacion by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState()

    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            birthdate = localDate.format(formatter)
                        }
                        mostrarDatePicker = false
                    }
                ) {
                    Text("Aceptar", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = TextoGris)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val formularioCompleto = actaUri != null && birthdate.isNotBlank() && (sinAlergias || alergias.isNotEmpty())

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
                titulo = "Registro de Expediente",
                subtitulo = child.fullName
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                        AvatarCircular(tamano = 64.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Bienvenido/a a Estancia Mariposa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Text(
                            text = "Por favor completa el expediente inicial de ${child.fullName.substringBefore(" ")} para finalizar el registro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoGris
                        )
                    }
                }

                // Formulario
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
                            text = "Documentación e Información de Salud",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )

                        // Adjuntar Acta de Nacimiento (Genera automáticamente "acta.pdf")
                        if (actaUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VerdeSuave, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Verde,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "acta.pdf adjuntada correctamente",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextoOscuro
                                        )
                                    }
                                    TextButton(onClick = { actaUri = null }) {
                                        Text("Cambiar", color = Coral, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    actaUri = Uri.parse("content://dummy/acta.pdf")
                                    errorValidacion = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Acta de Nacimiento")
                            }
                        }

                        // Fecha de Nacimiento (Picker calendario)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Fecha de Nacimiento",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextoOscuro
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, BordeCampo, RoundedCornerShape(12.dp))
                                    .clickable { mostrarDatePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = birthdate.ifBlank { "Selecciona una fecha" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (birthdate.isNotBlank()) TextoOscuro else TextoGris
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = "Abrir calendario",
                                    tint = Coral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        SelectorAlergias(
                            catalogo = catalogoAlergias,
                            seleccion = alergias,
                            sinAlergias = sinAlergias,
                            onAlternar = { nombre ->
                                alergias = if (nombre in alergias) alergias - nombre else alergias + nombre
                                sinAlergias = false
                                errorValidacion = null
                            },
                            onSinAlergias = {
                                sinAlergias = !sinAlergias
                                if (sinAlergias) alergias = emptySet()
                                errorValidacion = null
                            },
                            onReintentar = onReintentarCatalogo
                        )
                    }
                }

                if (errorValidacion != null) {
                    Text(
                        text = errorValidacion!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (!formularioCompleto) {
                            errorValidacion = "Por favor adjunta el acta de nacimiento (acta.pdf), selecciona la fecha de nacimiento y marca sus alergias (o \"Sin alergias\") para continuar."
                        } else {
                            onGuardarInformacion(birthdate, if (sinAlergias) emptyList() else alergias.sorted(), actaUri)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Completar Expediente e Iniciar ➔", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SelectorAlergias(
    catalogo: UiState<List<String>>,
    seleccion: Set<String>,
    sinAlergias: Boolean,
    onAlternar: (String) -> Unit,
    onSinAlergias: () -> Unit,
    onReintentar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Alergias",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        when (catalogo) {
            is UiState.Exito -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FilterChip(
                    selected = sinAlergias,
                    onClick = onSinAlergias,
                    label = { Text("Sin alergias", style = MaterialTheme.typography.labelSmall) }
                )
                catalogo.datos.forEach { nombre ->
                    FilterChip(
                        selected = nombre in seleccion,
                        onClick = { onAlternar(nombre) },
                        label = { Text(nombre, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            is UiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = catalogo.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReintentar) { Text("Reintentar", color = Coral) }
            }
            else -> Text("Cargando alergias…", style = MaterialTheme.typography.bodySmall, color = TextoGris)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingHijoPreview() {
    SonrisasTheme {
        OnboardingHijoScreen(
            child = Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"),
            catalogoAlergias = UiState.Exito(listOf("Cacahuate", "Huevo", "Leche y lácteos", "Trigo y gluten")),
            onGuardarInformacion = { _, _, _ -> }
        )
    }
}
