package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.PickupKind
import mx.tec.sonrisas.domain.currentWeek
import mx.tec.sonrisas.ui.components.CampoDropdown
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

private val opcionesIdentificacion = listOf(
    "INE / Credencial para Votar",
    "Licencia de Conducir",
    "Pasaporte",
    "Cédula Profesional",
    "Otra Identificación Oficial"
)

private val espanol = Locale.forLanguageTag("es-MX")

private fun DayOfWeek.corto() = getDisplayName(TextStyle.SHORT, espanol).replaceFirstChar { it.uppercase() }

/** "Esta semana: Lun 20, Mié 22" or "Cada semana: Lun, Vie (hasta que la canceles)". */
private fun PickupAuthorization.horario(): String {
    val dias = weekdays.sorted()
    return when (kind) {
        PickupKind.THIS_WEEK -> "Esta semana: " + dias.joinToString { d ->
            "${d.corto()} ${startDate.plusDays((d.value - 1).toLong()).dayOfMonth}"
        }
        PickupKind.RECURRING -> "Cada semana: " + dias.joinToString { it.corto() } + " (hasta que la canceles)"
    }
}

/**
 * The parent manages who else may pick their child up: the ones in force, and a form for a new one
 * (or to edit one). Saving needs a connection: the school must actually receive it.
 */
@Composable
fun PadreAutorizarTerceroScreen(
    childId: String,
    childName: String,
    autorizaciones: List<PickupAuthorization>,
    guardando: Boolean,
    error: String?,
    conflicto: PickupAuthorization?,
    onAtras: () -> Unit,
    onGuardar: (PickupAuthorization, onListo: () -> Unit) -> Unit,
    onReemplazar: (onListo: () -> Unit) -> Unit,
    onDescartarConflicto: () -> Unit,
    onCancelar: (PickupAuthorization) -> Unit,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()

    // null = a new one; otherwise the one being edited (its kind can't change)
    var editando by remember { mutableStateOf<PickupAuthorization?>(null) }
    var porCancelar by remember { mutableStateOf<PickupAuthorization?>(null) }
    // A new form every time it is reset (after saving, or when picking another one to edit)
    var formulario by remember { mutableStateOf(0) }

    if (conflicto != null) {
        AlertDialog(
            onDismissRequest = onDescartarConflicto,
            title = { Text("Ya hay alguien autorizado") },
            text = { Text("Otra persona ya está autorizada en alguno de esos días. ¿Quieres reemplazarla por ${conflicto.authorizedPersonName}?") },
            confirmButton = {
                TextButton(onClick = { onReemplazar { editando = null; formulario++ } }) { Text("Reemplazar") }
            },
            dismissButton = { TextButton(onClick = onDescartarConflicto) { Text("No") } }
        )
    }
    porCancelar?.let { autorizacion ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("Cancelar autorización") },
            text = { Text("${autorizacion.authorizedPersonName} ya no podrá recoger a $childName.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancelar(autorizacion)
                    if (editando?.id == autorizacion.id) { editando = null; formulario++ }
                    porCancelar = null
                }) { Text("Cancelar autorización") }
            },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("Volver") } }
        )
    }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Personas autorizadas",
                subtitulo = "Alumno/a: $childName",
                onAtras = onAtras
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (autorizaciones.isNotEmpty()) {
                    Text("Autorizaciones vigentes", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = TextoOscuro)
                    autorizaciones.forEach { autorizacion ->
                        TarjetaAutorizacion(
                            autorizacion = autorizacion,
                            onEditar = { editando = autorizacion; formulario++ },
                            onCancelar = { porCancelar = autorizacion }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                androidx.compose.runtime.key(formulario) {
                    FormularioAutorizacion(
                        childId = childId,
                        existente = editando,
                        guardando = guardando,
                        onGuardar = { nueva -> onGuardar(nueva) { editando = null; formulario++ } },
                        onDescartarEdicion = { editando = null; formulario++ }
                    )
                }
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = Coral)
                }
            }
        }
    }
}

@Composable
private fun TarjetaAutorizacion(autorizacion: PickupAuthorization, onEditar: () -> Unit, onCancelar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CoralSuave)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${autorizacion.authorizedPersonName} (${autorizacion.relationship})",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
            Text(autorizacion.horario(), style = MaterialTheme.typography.bodySmall, color = TextoOscuro)
            Text("Tel. ${autorizacion.phone} · ${autorizacion.identificationType}",
                style = MaterialTheme.typography.bodySmall, color = TextoGris)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = onCancelar) { Text("Cancelar", color = Coral) }
            }
        }
    }
}

@Composable
private fun FormularioAutorizacion(
    childId: String,
    existente: PickupAuthorization?,
    guardando: Boolean,
    onGuardar: (PickupAuthorization) -> Unit,
    onDescartarEdicion: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var tipo by remember { mutableStateOf(existente?.kind ?: PickupKind.THIS_WEEK) }
    var dias by remember { mutableStateOf(existente?.weekdays ?: emptySet()) }
    var personaNombre by remember { mutableStateOf(existente?.authorizedPersonName ?: "") }
    var parentesco by remember { mutableStateOf(existente?.relationship ?: "") }
    var telefono by remember { mutableStateOf(existente?.phone ?: "") }
    var identificacion by remember { mutableStateOf(existente?.identificationType ?: opcionesIdentificacion.first()) }
    var observaciones by remember { mutableStateOf(existente?.notes ?: "") }

    Text(
        text = if (existente == null) "Nueva autorización" else "Editar autorización",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = TextoOscuro
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PickupKind.entries.forEach { opcion ->
            FilterChip(
                selected = tipo == opcion,
                onClick = { if (tipo != opcion) { tipo = opcion; dias = emptySet() } },
                enabled = existente == null,
                label = { Text(if (opcion == PickupKind.THIS_WEEK) "Solo esta semana" else "Cada semana") }
            )
        }
    }

    Text(
        text = if (tipo == PickupKind.THIS_WEEK) "¿Qué días de esta semana?" else "¿Qué días? (hasta que la canceles)",
        style = MaterialTheme.typography.bodySmall,
        color = TextoGris
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        if (tipo == PickupKind.THIS_WEEK) {
            currentWeek(hoy).forEach { dia ->
                val diaSemana = dia.date.dayOfWeek
                // Past days can't be added; one already chosen stays as it was
                SelectorDia(
                    texto = "${diaSemana.corto().take(2)}\n${dia.date.dayOfMonth}",
                    seleccionado = diaSemana in dias,
                    habilitado = !dia.isPast,
                    onClick = { dias = if (diaSemana in dias) dias - diaSemana else dias + diaSemana },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DayOfWeek.entries.forEach { diaSemana ->
                SelectorDia(
                    texto = diaSemana.corto().take(2),
                    seleccionado = diaSemana in dias,
                    habilitado = true,
                    onClick = { dias = if (diaSemana in dias) dias - diaSemana else dias + diaSemana },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    CampoTexto(etiqueta = "Nombre completo de quien recogerá", valor = personaNombre, onValorChange = { personaNombre = it })
    CampoTexto(etiqueta = "Parentesco o relación (ej. Abuelo, Tío)", valor = parentesco, onValorChange = { parentesco = it })
    CampoTexto(etiqueta = "Teléfono de contacto", valor = telefono, onValorChange = { telefono = it })
    CampoDropdown(
        etiqueta = "Tipo de identificación",
        valorSeleccionado = identificacion,
        opciones = opcionesIdentificacion,
        onSeleccionar = { identificacion = it }
    )
    CampoTexto(etiqueta = "Observaciones o detalles", valor = observaciones, onValorChange = { observaciones = it }, singleLine = false)

    val completo = personaNombre.isNotBlank() && telefono.isNotBlank() && dias.isNotEmpty()
    Button(
        onClick = {
            val base = existente ?: PickupAuthorization(
                childId = childId, kind = tipo, weekdays = dias,
                authorizedPersonName = "", relationship = "", phone = ""
            )
            onGuardar(
                base.copy(
                    weekdays = dias,
                    authorizedPersonName = personaNombre.trim(),
                    relationship = parentesco.trim().ifBlank { "Familiar" },
                    phone = telefono.trim(),
                    identificationType = identificacion.ifBlank { "INE" },
                    notes = observaciones.trim().ifBlank { null }
                )
            )
        },
        enabled = completo && !guardando,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
    ) {
        Text(if (guardando) "Enviando…" else "Guardar autorización")
    }
    if (existente != null) {
        TextButton(onClick = onDescartarEdicion, modifier = Modifier.fillMaxWidth()) { Text("Descartar cambios") }
    }
}

@Composable
private fun SelectorDia(texto: String, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        enabled = habilitado,
        label = { Text(texto, style = MaterialTheme.typography.labelSmall, maxLines = 2) },
        modifier = modifier
    )
}
