package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.model.UiState
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun ChatContenido(
    estado: UiState<List<Mensaje>>,
    textoVacio: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarRes: Int? = null,
    tituloAviso: String = "AVISO GENERAL"
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (estado) {
            UiState.Cargando -> EstadoCargando()

            UiState.Vacio -> EstadoVacio(
                titulo = "Sin mensajes",
                subtitulo = textoVacio,
                modifier = Modifier.padding(16.dp)
            )

            is UiState.Error -> EstadoError(
                titulo = "Error de conexión",
                mensaje = "No pudimos cargar la conversación",
                onReintentar = onReintentar
            )

            is UiState.Exito -> {
                val mensajes = estado.datos
                val lista = rememberLazyListState()

                LaunchedEffect(mensajes.size) {
                    if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex)
                }

                LazyColumn(
                    state = lista,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mensajes) { mensaje ->
                        if (mensaje.esAviso) {
                            AvisoGeneral(
                                titulo = tituloAviso,
                                texto = mensaje.texto,
                                hora = mensaje.hora
                            )
                        } else {
                            BurbujaChat(mensaje = mensaje, avatarRes = avatarRes)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun ChatContenidoPreview() {
    SonrisasTheme {
        ChatContenido(
            estado = UiState.Exito(
                listOf(
                    Mensaje("Este viernes habrá taller de pintura.", "8:30 AM", esMio = false, esAviso = true),
                    Mensaje("Hola, Sofía se divirtió mucho hoy.", "2:15 PM", esMio = false),
                    Mensaje("¡Muchas gracias, maestra!", "2:30 PM", esMio = true)
                )
            ),
            textoVacio = "Envía un mensaje al padre de familia",
            onReintentar = {}
        )
    }
}