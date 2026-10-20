package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.theme.*

/**
 * Una conversación en cualquier lista de chats: nombre, último mensaje y su hora.
 *
 * @param fijado chat con el nivel de arriba (maestra → dirección, admin → organización): va siempre primero
 *   y se resalta con un borde.
 * @param onClick null = no se puede abrir (p. ej. una instancia sin administrador).
 */
@Composable
fun FilaChat(
    titulo: String,
    ultimoMensaje: Message?,
    modifier: Modifier = Modifier,
    fijado: Boolean = false,
    textoSinMensajes: String = "Sin mensajes aún",
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (fijado) VerdeSuave else Color.White),
        border = if (fijado) BorderStroke(2.dp, Verde) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircular(tamano = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ultimoMensaje?.vistaPrevia() ?: textoSinMensajes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (ultimoMensaje != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatearHora(ultimoMensaje.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoGris
                )
            }
            if (fijado) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Fijado",
                    tint = Verde,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// A photo has no text, so the list says so instead of showing an empty line.
private fun Message.vistaPrevia(): String =
    content.ifBlank { if (attachmentFileId != null) "Foto" else "" }

@Preview(showBackground = true)
@Composable
private fun FilaChatPreview() {
    SonrisasTheme {
        Column {
            FilaChat(titulo = "Organización", ultimoMensaje = null, fijado = true, onClick = {})
            Spacer(Modifier.size(8.dp))
            FilaChat(
                titulo = "Laura Gómez",
                ultimoMensaje = Message(chatId = "1", authorId = "a", content = "Mañana hay junta", sentAt = LocalDateTime.now()),
                onClick = {}
            )
        }
    }
}
