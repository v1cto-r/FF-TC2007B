package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

/**
 * El admin captura los platillos de la semana (nombre y, opcional, ingredientes con posibles alérgenos).
 * Jev revisa qué alérgenos podría tener cada uno y se listan los niños de la estancia en conflicto,
 * para planear comidas especiales.
 */
@Composable
fun AdminMenuAlergiasScreen(
    platillos: List<PlatilloUi>,
    analisis: UiState<AnalisisMenuUi>?,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onAgregarPlatillo: () -> Unit = {},
    onQuitarPlatillo: (Long) -> Unit = {},
    onNombrePlatillo: (Long, String) -> Unit = { _, _ -> },
    onAgregarIngrediente: (Long, String) -> Unit = { _, _ -> },
    onQuitarIngrediente: (Long, String) -> Unit = { _, _ -> },
    onAnalizar: () -> Unit = {}
) {
    FlagSecureDisable()
    val conNombre = platillos.count { it.nombre.isNotBlank() }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = if (conNombre == 1) "Revisar alergias (1 platillo)" else "Revisar alergias ($conNombre platillos)",
                habilitado = conNombre > 0,
                guardando = analisis == UiState.Cargando,
                errorMensaje = (analisis as? UiState.Error)?.mensaje,
                onClick = onAnalizar
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Menú y alergias", subtitulo = "Comida de la semana", onAtras = onAtras)

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Agrega cada platillo de la semana. Si sabes de un ingrediente que pueda causar " +
                            "alergia, especifícalo para que la revisión sea más precisa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
                item { TituloSeccion("Platillos de la semana") }
                itemsIndexed(platillos, key = { _, p -> p.id }) { indice, platillo ->
                    TarjetaPlatillo(
                        numero = indice + 1,
                        platillo = platillo,
                        puedeQuitar = platillos.size > 1 || platillo.nombre.isNotBlank() || platillo.ingredientes.isNotEmpty(),
                        onNombre = { onNombrePlatillo(platillo.id, it) },
                        onQuitar = { onQuitarPlatillo(platillo.id) },
                        onAgregarIngrediente = { onAgregarIngrediente(platillo.id, it) },
                        onQuitarIngrediente = { onQuitarIngrediente(platillo.id, it) }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onAgregarPlatillo,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Coral)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar platillo", color = Coral)
                    }
                }

                when (analisis) {
                    UiState.Cargando -> item { Analizando() }
                    is UiState.Exito -> resultado(analisis.datos)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun TarjetaPlatillo(
    numero: Int,
    platillo: PlatilloUi,
    puedeQuitar: Boolean,
    onNombre: (String) -> Unit,
    onQuitar: () -> Unit,
    onAgregarIngrediente: (String) -> Unit,
    onQuitarIngrediente: (String) -> Unit
) {
    var ingrediente by rememberSaveable(platillo.id) { mutableStateOf("") }
    val agregar = {
        onAgregarIngrediente(ingrediente)
        ingrediente = ""
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Platillo $numero",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro,
                modifier = Modifier.weight(1f)
            )
            if (puedeQuitar) {
                IconButton(onClick = onQuitar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Quitar platillo", tint = TextoGris)
                }
            }
        }
        CampoTexto("Nombre del platillo (p. ej. Pollo con mole)", platillo.nombre, onNombre)

        Text(
            text = "Especificar ingredientes con posibles alérgenos (opcional)",
            style = MaterialTheme.typography.bodySmall,
            color = TextoGris
        )
        if (platillo.ingredientes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                platillo.ingredientes.forEach { i ->
                    InputChip(
                        selected = false,
                        onClick = { onQuitarIngrediente(i) },
                        label = { Text(i, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(Icons.Outlined.Close, contentDescription = "Quitar $i", modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ingrediente,
                onValueChange = { ingrediente = it },
                placeholder = { Text("Ingrediente (p. ej. cacahuate)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { agregar() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = BordeCampo,
                    cursorColor = Coral
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = agregar, enabled = ingrediente.isNotBlank()) {
                Icon(Icons.Outlined.Add, contentDescription = "Agregar ingrediente", tint = Coral)
            }
        }
    }
}

@Composable
private fun Analizando() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), color = Coral, strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text("Revisando alérgenos de cada platillo…", style = MaterialTheme.typography.bodySmall, color = TextoGris)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultado(datos: AnalisisMenuUi) {
    item {
        Spacer(Modifier.height(8.dp))
        TituloSeccion("Posibles alergias:")
    }
    if (datos.ninos.isEmpty()) {
        item {
            Text(
                text = "Ningún niño de la estancia tiene alergias en conflicto con este menú.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoGris
            )
        }
    }
    items(datos.ninos, key = { it.id }) { nino -> TarjetaNinoConflicto(nino) }

    if (datos.detectados.isNotEmpty()) {
        item {
            Spacer(Modifier.height(8.dp))
            TituloSeccion("Alérgenos detectados en el menú")
        }
        items(datos.detectados, key = { "detectado-${it.nombre}" }) { a ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
                    Text(a.platillos.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = TextoGris)
                }
                Text("${a.porcentaje}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Amarillo)
            }
        }
    }
}

@Composable
private fun TarjetaNinoConflicto(nino: NinoConflictoUi) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CoralSuave, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Coral, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(nino.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextoOscuro)
                Text(nino.salon ?: "Sin salón", style = MaterialTheme.typography.bodySmall, color = TextoGris)
            }
        }
        nino.conflictos.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = c.alergeno,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Coral,
                    modifier = Modifier
                        .background(CoralSuave, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "en ${c.platillos.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminMenuAlergiasPreview() {
    SonrisasTheme {
        AdminMenuAlergiasScreen(
            platillos = listOf(
                PlatilloUi(1, "Pollo con mole", listOf("cacahuate", "ajonjolí")),
                PlatilloUi(2, "Hot cakes")
            ),
            analisis = UiState.Exito(
                AnalisisMenuUi(
                    detectados = listOf(
                        AlergenoDetectadoUi("Cacahuate", 92, listOf("Pollo con mole")),
                        AlergenoDetectadoUi("Huevo", 88, listOf("Hot cakes"))
                    ),
                    ninos = listOf(
                        NinoConflictoUi(
                            "1", "Sofía López", "Maternal A",
                            listOf(ConflictoUi("Cacahuate", listOf("Pollo con mole")))
                        )
                    )
                )
            )
        )
    }
}
