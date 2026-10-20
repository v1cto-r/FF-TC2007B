package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.layout.*
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
import java.time.LocalDate
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.PickupKind
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

@Composable
fun PadreAutorizarTerceroScreen(
    childId: String,
    childName: String,
    parentName: String,
    onAtras: () -> Unit,
    onGuardar: (PickupAuthorization) -> Unit,
    autorizacionExistente: PickupAuthorization? = null,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()

    var personaNombre by remember(autorizacionExistente) { mutableStateOf(autorizacionExistente?.authorizedPersonName ?: "") }
    var parentesco by remember(autorizacionExistente) { mutableStateOf(autorizacionExistente?.relationship ?: "") }
    var telefono by remember(autorizacionExistente) { mutableStateOf(autorizacionExistente?.phone ?: "") }
    var identificacion by remember(autorizacionExistente) {
        mutableStateOf(autorizacionExistente?.identificationType ?: opcionesIdentificacion.first())
    }
    var observaciones by remember(autorizacionExistente) { mutableStateOf(autorizacionExistente?.notes ?: "") }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Autorizar salida a tercero",
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
                Text(
                    text = "Datos de la persona autorizada",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )

                CampoTexto(
                    etiqueta = "Nombre completo de quien recogerá",
                    valor = personaNombre,
                    onValorChange = { personaNombre = it }
                )

                CampoTexto(
                    etiqueta = "Parentesco o relación (ej. Abuelo, Tío)",
                    valor = parentesco,
                    onValorChange = { parentesco = it }
                )

                CampoTexto(
                    etiqueta = "Teléfono de contacto",
                    valor = telefono,
                    onValorChange = { telefono = it }
                )

                CampoDropdown(
                    etiqueta = "Tipo de identificación",
                    valorSeleccionado = identificacion,
                    opciones = opcionesIdentificacion,
                    onSeleccionar = { identificacion = it }
                )

                CampoTexto(
                    etiqueta = "Observaciones o detalles",
                    valor = observaciones,
                    onValorChange = { observaciones = it },
                    singleLine = false
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (personaNombre.isNotBlank() && telefono.isNotBlank()) {
                            // Until the week / recurring form exists: a new one covers today only
                            val base = autorizacionExistente ?: PickupAuthorization(
                                childId = childId,
                                kind = PickupKind.THIS_WEEK,
                                weekdays = setOf(LocalDate.now().dayOfWeek),
                                authorizedPersonName = "",
                                relationship = "",
                                phone = ""
                            )
                            onGuardar(
                                base.copy(
                                    authorizedPersonName = personaNombre,
                                    relationship = parentesco.ifBlank { "Familiar" },
                                    phone = telefono,
                                    identificationType = identificacion.ifBlank { "INE" },
                                    notes = observaciones.ifBlank { null }
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar autorización")
                }
            }
        }
    }
}
