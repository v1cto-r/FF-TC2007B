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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import mx.tec.sonrisas.domain.Notice
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ItemEvento(
    notice: Notice,
    modifier: Modifier = Modifier
) {
    val dia = notice.date.dayOfWeek
        .getDisplayName(TextStyle.SHORT, Locale("es", "MX"))
        .uppercase(Locale("es", "MX"))

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
                    .background(VerdeSuave, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dia,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Verde
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = notice.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = notice.description,
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
            ItemEvento(Notice(name = "Reunión de Padres", description = "Taller mensual de entrega de reportes - 4:00 PM", date = LocalDate.now()))
        }
    }
}
