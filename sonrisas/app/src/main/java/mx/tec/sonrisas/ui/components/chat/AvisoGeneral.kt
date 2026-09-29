package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AvisoGeneral(
    texto: String,
    hora: String,
    modifier: Modifier = Modifier,
    titulo: String = "AVISO GENERAL"
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AmarilloSuave, forma)
            .border(1.dp, Amarillo.copy(alpha = 0.4f), forma)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Campaign,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Coral
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoOscuro
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = hora,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = TextoGris,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvisoGeneralPreview() {
    SonrisasTheme {
        AvisoGeneral(
            titulo = "AVISO GENERAL ENVIADO",
            texto = "Este viernes habrá taller de pintura. Favor de enviar delantal protector.",
            hora = "8:30 AM",
            modifier = Modifier.padding(16.dp)
        )
    }
}