package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.theme.*

@Composable
fun BurbujaChat(
    mensaje: Mensaje,
    modifier: Modifier = Modifier,
    avatarRes: Int? = null
) {
    val esMio = mensaje.esMio
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
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(fondo, forma)
                .padding(if (mensaje.imagenRes != null) 6.dp else 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            mensaje.imagenRes?.let { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = "Foto enviada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(4.dp))
            }
            if (mensaje.texto.isNotEmpty()) {
                Text(
                    text = mensaje.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorTexto
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = mensaje.hora,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
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
            BurbujaChat(Mensaje("Hola Maestra Laura, muchas gracias por el aviso.", "11:15 AM", esMio = false))
            BurbujaChat(Mensaje("¡Muchas gracias por el reporte, maestra!", "2:30 PM", esMio = true))
        }
    }
}