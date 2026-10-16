package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.CampoMensaje
import mx.tec.sonrisas.ui.components.chat.ChatContenido
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ChatMaestraScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    titulo: String = "Chat",
    subtitulo: String? = null,
    onAtras: () -> Unit = {},
    onEnviar: (texto: String) -> Unit = {},
    onToggleImportante: (Message) -> Unit = {},
    onEliminar: ((Message) -> Unit)? = null,
    onCamara: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureEnable()
    var texto by remember { mutableStateOf("") }
    val esTecladoVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            if (!esTecladoVisible) {
                BottomBar(tabs = TabsMaestra, seleccionado = 2, onSeleccionar = onTab)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = if (esTecladoVisible) 0.dp else padding.calculateBottomPadding()
                )
        ) {
            TopBarAtras(
                titulo = titulo,
                subtitulo = subtitulo,
                onAtras = onAtras,
                mostrarAvatar = true
            )
            ChatContenido(
                estado = estado,
                currentUserId = currentUserId,
                textoVacio = "Envía un mensaje para iniciar la conversación",
                onReintentar = onReintentar,
                modifier = Modifier.weight(1f),
                onToggleImportante = onToggleImportante,
                onEliminar = onEliminar
            )
            CampoMensaje(
                valor = texto,
                onValorChange = { texto = it },
                onEnviar = {
                    if (texto.isNotBlank()) {
                        onEnviar(texto)
                        texto = ""
                    }
                },
                onCamara = onCamara,
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatMaestraPreview() {
    SonrisasTheme {
        ChatMaestraScreen(
            estado = UiState.Exito(listOf(Message(chatId = "1", authorId = "maestra", content = "Hola", sentAt = LocalDateTime.now()))),
            currentUserId = "maestra"
        )
    }
}
