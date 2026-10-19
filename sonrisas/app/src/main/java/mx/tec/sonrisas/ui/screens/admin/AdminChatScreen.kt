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
    onEnviar: (String) -> Unit = {},
    onEnviarFoto: (Uri) -> Unit = {},
    onToggleImportante: (Message) -> Unit = {},
    onEliminar: (Message) -> Unit = {},
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
        onToggleImportante = onToggleImportante,
        onEliminar = onEliminar,
        onReintentar = onReintentar
    )
}
