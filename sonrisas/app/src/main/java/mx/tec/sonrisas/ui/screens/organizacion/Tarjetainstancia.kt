package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.ui.theme.*

/**
 * Misma idea que TarjetaSalon: círculo de color con ícono, nombre, subtítulo y etiqueta.
 * Si se pasa [trailing] (por ejemplo un Checkbox), reemplaza a la etiqueta de estado.
 */
@Composable
fun TarjetaInstancia(
    instancia: InstanciaUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resaltado: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (resaltado) BorderStroke(1.5.dp, Coral) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(VerdeSuave, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = Verde,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instancia.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (instancia.admin == null) "Sin administrador asignado"
                    else "${instancia.ciudad} · ${instancia.ninos} niños",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (instancia.admin == null) Coral else TextoGris
                )
            }
            if (trailing != null) trailing() else EtiquetaEstadoInstancia(activa = instancia.activa)
        }
    }
}

/** Etiqueta redondeada "Activa" / "Suspendida". */
@Composable
fun EtiquetaEstadoInstancia(activa: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (activa) "Activa" else "Suspendida",
        modifier = modifier
            .background(if (activa) VerdeSuave else AmarilloSuave, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        fontWeight = FontWeight.SemiBold,
        color = if (activa) Verde else Amarillo
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun TarjetaInstanciaPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OrgMuestra.instancias.forEach { TarjetaInstancia(it, onClick = {}) }
            TarjetaInstancia(
                instancia = OrgMuestra.instancias[0],
                onClick = {},
                resaltado = true,
                trailing = {
                    Checkbox(
                        checked = true,
                        onCheckedChange = {},
                        colors = CheckboxDefaults.colors(checkedColor = Coral)
                    )
                }
            )
        }
    }
}