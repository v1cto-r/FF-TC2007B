package mx.tec.sonrisas.ui.components.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TabItem
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

/**
 * Pantalla de chat genérica. Cada rol (padre, maestra, admin, org…) la envuelve
 * pasando sus tabs y textos propios.
 *
 * @param tabs tabs de la BottomBar del rol; null = sin BottomBar.
 * @param onEliminar null = los mensajes no se pueden eliminar.
 */
@Composable
fun ChatScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    titulo: String,
    textoVacio: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    tabs: List<TabItem>? = null,
    tabSeleccionado: Int = 0,
    onTab: (Int) -> Unit = {},
    onAtras: (() -> Unit)? = null,
    onEnviar: (texto: String) -> Unit = {},
    onEnviarFoto: (Uri) -> Unit = {},
    onToggleImportante: (Message) -> Unit = {},
    onEliminar: ((Message) -> Unit)? = null,
    onReintentar: () -> Unit = {}
) {
    FlagSecureEnable()
    val context = LocalContext.current
    var texto by remember { mutableStateOf("") }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val esTecladoVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEnviarFoto(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            tempPhotoUri?.let { uri -> onEnviarFoto(uri) }
        }
    }

    val abrirCamara = {
        try {
            val file = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (mostrarMenuFoto) {
        SelectorFuenteFotoDialog(
            onDismiss = { mostrarMenuFoto = false },
            onCamara = abrirCamara,
            onGaleria = { galleryLauncher.launch("image/*") }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            if (tabs != null && !esTecladoVisible) {
                BottomBar(tabs = tabs, seleccionado = tabSeleccionado, onSeleccionar = onTab)
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
                textoVacio = textoVacio,
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
                onCamara = { mostrarMenuFoto = true },
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenPreview() {
    SonrisasTheme {
        ChatScreen(
            estado = UiState.Exito(listOf(Message(chatId = "1", authorId = "maestra", content = "Hola", sentAt = LocalDateTime.now()))),
            currentUserId = "maestra",
            titulo = "Chat",
            textoVacio = "Envía un mensaje para iniciar la conversación",
            tabs = TabsMaestra,
            tabSeleccionado = 2
        )
    }
}
