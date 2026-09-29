package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.model.Mocks
import mx.tec.sonrisas.model.UiState
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.CampoMensaje
import mx.tec.sonrisas.ui.components.chat.ChatContenido
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ChatMaestraScreen(
    estado: UiState<List<Mensaje>>,
    modifier: Modifier = Modifier,
    titulo: String = "Chat con María",
    subtitulo: String = "Madre de Sofía",
    onAtras: () -> Unit = {},
    onEnviar: (String) -> Unit = {},
    onCamara: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    var texto by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 2, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = titulo,
                subtitulo = subtitulo,
                onAtras = onAtras,
                mostrarAvatar = true
            )
            ChatContenido(
                estado = estado,
                textoVacio = "Envía un mensaje para iniciar la conversación",
                onReintentar = onReintentar,
                modifier = Modifier.weight(1f),
                tituloAviso = "AVISO GENERAL ENVIADO"
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
                onCamara = onCamara
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatMaestraPreview() {
    SonrisasTheme {
        ChatMaestraScreen(estado = UiState.Exito(Mocks.mensajesMaestra))
    }
}