package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.clickable
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
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.etiqueta
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ItemNino(
    child: Child,
    presente: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircular(tamano = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = "${child.daycareCenter} · ${child.group.etiqueta()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
            ChipAsistencia(presente = presente)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun ItemNinoPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemNino(
                Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"),
                presente = true,
                onClick = {}
            )
            ItemNino(
                Child(fullName = "Valentina Ruiz", daycareCenter = "Estancia Mariposa", group = GroupEnum.M, parentName = "Carla Ruiz"),
                presente = false,
                onClick = {}
            )
        }
    }
}
