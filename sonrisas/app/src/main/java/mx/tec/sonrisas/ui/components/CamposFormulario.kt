package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.BordeCampo
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.CoralSuave
import mx.tec.sonrisas.ui.theme.TextoGris
import mx.tec.sonrisas.ui.theme.TextoOscuro

private val coloresCampo
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Coral,
        unfocusedBorderColor = BordeCampo,
        cursorColor = Coral
    )

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = coloresCampo,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CampoDropdown(
    etiqueta: String,
    valorSeleccionado: String,
    opciones: List<String>,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valorSeleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = {
                IconButton(onClick = { expandido = !expandido }) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Desplegar opciones",
                        tint = TextoGris
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = coloresCampo,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = true }
        )

        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoOscuro
                        )
                    },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
fun CampoEntero(
    etiqueta: String,
    valor: Int,
    onValorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CampoTexto(
        etiqueta = etiqueta,
        valor = valor.toString(),
        onValorChange = { texto -> onValorChange(texto.toIntOrNull() ?: 0) },
        modifier = modifier
    )
}

@Composable
fun CampoContador(
    etiqueta: String,
    valor: Int,
    onValorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 0
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
        IconButton(
            onClick = { if (valor > minimo) onValorChange(valor - 1) },
            modifier = Modifier.size(32.dp).background(CoralSuave, CircleShape)
        ) {
            Icon(Icons.Outlined.Remove, contentDescription = "Restar", tint = Coral, modifier = Modifier.size(18.dp))
        }
        Text(
            text = valor.toString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = TextoOscuro
        )
        IconButton(
            onClick = { onValorChange(valor + 1) },
            modifier = Modifier.size(32.dp).background(CoralSuave, CircleShape)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Sumar", tint = Coral, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CampoBooleano(
    etiqueta: String,
    valor: Boolean,
    onValorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
        Switch(checked = valor, onCheckedChange = onValorChange)
    }
}

@Composable
fun CampoBooleanoOpcional(
    etiqueta: String,
    valor: Boolean?,
    onValorChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
            if (valor != null) {
                Switch(checked = valor, onCheckedChange = { onValorChange(it) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = valor == null, onCheckedChange = { marcado -> onValorChange(if (marcado) null else false) })
            Text("No aplica", style = MaterialTheme.typography.bodySmall, color = TextoGris)
        }
    }
}

@Composable
fun <T> CampoOpciones(
    etiqueta: String,
    valor: T,
    opciones: List<T>,
    onValorChange: (T) -> Unit,
    etiquetaOpcion: (T) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = opcion == valor,
                    onClick = { onValorChange(opcion) },
                    label = { Text(etiquetaOpcion(opcion), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
