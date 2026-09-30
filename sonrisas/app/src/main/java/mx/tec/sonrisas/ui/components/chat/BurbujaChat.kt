package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.theme.*

private val horaFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale("es", "MX"))

fun formatearHora(momento: LocalDateTime): String = momento.format(horaFormatter)

@Composable
fun BurbujaChat(
    message: Message,
    esMio: Boolean,
    modifier: Modifier = Modifier,
    avatarRes: Int? = null
) {
    val fondo = if (esMio) Coral else Color.White
    val colorTexto = if (esMio) Color.White else TextoOscuro
    val colorHora = if (esMio) Color.White.copy(alpha = 0.8f) else TextoGris
    val forma = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (esMio) 16.dp else 4.dp,
        bottomEnd = if (esMio) 4.dp else 16.dp
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!esMio) {
            AvatarCircular(tamano = 24.dp, imagenRes = avatarRes)
            Spacer(Modifier.width(8.dp))
        }
        val hora = formatearHora(message.sentAt)
        val estiloTexto = MaterialTheme.typography.bodyMedium
        val estiloHora = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val relleno = remember(hora, estiloTexto, estiloHora, density) {
            val anchoHora = textMeasurer.measure(hora, estiloHora).size.width
            val anchoEspacio = textMeasurer.measure(" ", estiloTexto).size.width.coerceAtLeast(1)
            " ".repeat(anchoHora / anchoEspacio + 4)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(fondo, forma)
                .padding(12.dp)
        ) {
            Text(
                text = message.content + relleno,
                style = estiloTexto,
                color = colorTexto
            )
            Text(
                text = hora,
                modifier = Modifier.align(Alignment.BottomEnd),
                style = estiloHora,
                color = colorHora
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun BurbujaChatPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BurbujaChat(
                Message(chatId = "1", authorId = "padre", content = "Hola Maestra Laura, muchas gracias por el aviso.", sentAt = LocalDateTime.now()),
                esMio = false
            )
            BurbujaChat(
                Message(chatId = "1", authorId = "maestra", content = "¡Muchas gracias por el reporte, maestra!", sentAt = LocalDateTime.now()),
                esMio = true
            )
        }
    }
}
