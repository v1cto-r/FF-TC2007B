package mx.tec.sonrisas.ui.screens.organizacion

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.chat.ChatScreen
import mx.tec.sonrisas.ui.state.UiState

@Composable
fun OrgChatScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    onAtras: (() -> Unit)? = null,
    onEnviar: (texto: String, esAviso: Boolean) -> Unit = { _, _ -> },
    onEnviarFoto: (Uri) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    ChatScreen(
        estado = estado,
        currentUserId = currentUserId,
        titulo = "Chat con Admin",
        textoVacio = "Escribe un mensaje al administrador de la estancia",
        modifier = modifier,
        subtitulo = "Comunicación institucional",
        tabs = TabsOrg,
        tabSeleccionado = 3,
        onTab = onTab,
        onAtras = onAtras,
        onEnviar = onEnviar,
        onEnviarFoto = onEnviarFoto,
        onEliminar = null,
        onReintentar = onReintentar
    )
}
