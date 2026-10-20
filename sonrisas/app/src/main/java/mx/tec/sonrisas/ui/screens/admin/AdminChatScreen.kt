package mx.tec.sonrisas.ui.screens.admin

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.chat.ChatScreen
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun AdminChatScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    titulo: String,
    subtitulo: String? = null,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onEnviar: (texto: String, esAviso: Boolean) -> Unit = { _, _ -> },
    onEnviarFoto: (Uri) -> Unit = {},
    onReintentar: () -> Unit = {}
) {
    ChatScreen(
        estado = estado,
        currentUserId = currentUserId,
        titulo = titulo,
        textoVacio = "Escribe un mensaje para iniciar la conversación",
        modifier = modifier,
        subtitulo = subtitulo,
        tabs = null,
        onAtras = onAtras,
        onEnviar = onEnviar,
        onEnviarFoto = onEnviarFoto,
        // Messages in teacher↔admin chats can never be deleted.
        onEliminar = null,
        onReintentar = onReintentar
    )
}
