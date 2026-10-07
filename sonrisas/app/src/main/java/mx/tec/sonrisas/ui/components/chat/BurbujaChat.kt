package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    avatarRes: Int? = null,
    onToggleImportante: ((Message) -> Unit)? = null,
    onEliminar: ((Message) -> Unit)? = null
) {
    var menuAbierto by remember { mutableStateOf(false) }

    val fondo = if (esMio) Coral else Color.White
    val colorTexto = if (esMio) Color.White else TextoOscuro
    val colorHora = if (esMio) Color.White.copy(alpha = 0.8f) else TextoGris
    val colorBordeFijado = if (esMio) Amarillo else Coral
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
        val relleno = remember(hora, message.isAviso, estiloTexto, estiloHora, density) {
            val anchoHora = textMeasurer.measure(hora, estiloHora).size.width
            val anchoPin = if (message.isAviso) with(density) { 15.dp.roundToPx() } else 0
            val anchoTotalMeta = anchoHora + anchoPin
            val anchoEspacio = textMeasurer.measure(" ", estiloTexto).size.width.coerceAtLeast(1)
            " ".repeat(anchoTotalMeta / anchoEspacio + 4)
        }

        Box {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(fondo, forma)
                    .border(
                        width = if (message.isAviso) 2.dp else 0.dp,
                        color = if (message.isAviso) colorBordeFijado else Color.Transparent,
                        shape = forma
                    )
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onLongPress = { menuAbierto = true }
                        )
                    }
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content + relleno,
                    style = estiloTexto,
                    color = colorTexto
                )

                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isAviso) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Mensaje fijado",
                            tint = if (esMio) Amarillo else Coral,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = hora,
                        style = estiloHora,
                        color = colorHora
                    )
                }
            }

            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (message.isAviso) "Desfijar mensaje" else "Fijar mensaje",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = Coral,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        menuAbierto = false
                        onToggleImportante?.invoke(message)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Eliminar mensaje",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        menuAbierto = false
                        onEliminar?.invoke(message)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun BurbujaChatPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BurbujaChat(
                Message(chatId = "1", authorId = "padre", content = "Hola Maestra Laura, aviso que hoy llega tarde.", sentAt = LocalDateTime.now(), isAviso = true),
                esMio = false
            )
            BurbujaChat(
                Message(chatId = "1", authorId = "maestra", content = "Entendido, muchas gracias por avisar.", sentAt = LocalDateTime.now()),
                esMio = true
            )
        }
    }
}
