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
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    ChatScreen(
        estado = estado,
        currentUserId = currentUserId,
        titulo = titulo,
        modifier = modifier,
        subtitulo = subtitulo,
        tabs = TabsAdmin,
        tabSeleccionado = 3,
        onTab = onTab,
        onAtras = onAtras,
        onEnviar = onEnviar,
        onEnviarFoto = onEnviarFoto,
        // Messages in peer chats (teacher↔admin, admin↔org) can never be deleted.
        onEliminar = null,
        onReintentar = onReintentar
    )
}
