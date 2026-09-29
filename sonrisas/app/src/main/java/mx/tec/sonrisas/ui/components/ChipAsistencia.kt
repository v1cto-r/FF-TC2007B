package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.model.EstadoAsistencia
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ChipAsistencia(
    estado: EstadoAsistencia,
    modifier: Modifier = Modifier
) {
    val presente = estado == EstadoAsistencia.Presente
    val color = if (presente) Verde else Coral
    val fondo = if (presente) VerdeSuave else CoralSuave
    val texto = if (presente) "Presente" else "Ausente"

    Row(
        modifier = modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChipAsistenciaPreview() {
    SonrisasTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipAsistencia(EstadoAsistencia.Presente)
            ChipAsistencia(EstadoAsistencia.Ausente)
        }
    }
}