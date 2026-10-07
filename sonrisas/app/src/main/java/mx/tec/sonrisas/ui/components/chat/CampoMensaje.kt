package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.*

@Composable
fun CampoMensaje(
    valor: String,
    onValorChange: (String) -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier,
    onCamara: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Fondo)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, BordeCampo, RoundedCornerShape(20.dp))
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = valor,
                onValueChange = onValorChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextoOscuro),
                cursorBrush = SolidColor(Coral),
                decorationBox = { campo ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (valor.isEmpty()) {
                            Text(
                                text = "Escribe un mensaje...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoGris
                            )
                        }
                        campo()
                    }
                }
            )
            IconButton(onClick = onCamara, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Adjuntar foto",
                    tint = TextoGris,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (valor.isNotBlank()) {
            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Coral)
                    .clickable(onClick = onEnviar),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar mensaje",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CampoMensajePreview() {
    SonrisasTheme {
        CampoMensaje(valor = "", onValorChange = {}, onEnviar = {})
    }
}
