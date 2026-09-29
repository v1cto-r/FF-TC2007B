package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.model.Evento
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ItemEvento(
    evento: Evento,
    modifier: Modifier = Modifier
) {
    val colorFondo = if (evento.esCoral) CoralSuave else VerdeSuave
    val colorTexto = if (evento.esCoral) Coral else Verde

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colorFondo, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = evento.dia,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = evento.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = evento.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun ItemEventoPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemEvento(Evento("VIE", "Reunión de Padres", "Taller mensual de entrega de reportes - 4:00 PM", true))
            ItemEvento(Evento("LUN", "Vacunación programada", "Campaña nacional de salud infantil - 9:00 AM", false))
        }
    }
}