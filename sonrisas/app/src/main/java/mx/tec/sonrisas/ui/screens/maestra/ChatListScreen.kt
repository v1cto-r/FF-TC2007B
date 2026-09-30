package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.formatearHora
import mx.tec.sonrisas.ui.state.ConversacionUi
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ChatListScreen(
    estado: UiState<List<ConversacionUi>>,
    modifier: Modifier = Modifier,
    onConversacionClick: (Child) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 2, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Chats", subtitulo = "Conversaciones con los padres")

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin conversaciones",
                        subtitulo = "Cuando tengas niños registrados aparecerán aquí",
                        modifier = Modifier.padding(16.dp)
                    )

                    is UiState.Error -> EstadoError(
                        titulo = "Error al cargar",
                        mensaje = "No pudimos cargar tus chats",
                        onReintentar = onReintentar
                    )

                    is UiState.Exito -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(estado.datos) { conversacion ->
                            ItemConversacion(
                                conversacion = conversacion,
                                onClick = { onConversacionClick(conversacion.child) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemConversacion(
    conversacion: ConversacionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "${conversacion.child.parentName} - ${conversacion.child.fullName.substringBefore(" ")}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = conversacion.ultimoMensaje?.content ?: "Sin mensajes aún",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (conversacion.ultimoMensaje != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatearHora(conversacion.ultimoMensaje.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatListPreview() {
    SonrisasTheme {
        ChatListScreen(
            estado = UiState.Exito(
                listOf(
                    ConversacionUi(
                        Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"),
                        Message(chatId = "1", authorId = "padre", content = "Gracias maestra", sentAt = LocalDateTime.now())
                    ),
                    ConversacionUi(
                        Child(fullName = "Mateo García", daycareCenter = "Estancia Mariposa", group = GroupEnum.M, parentName = "Jorge García"),
                        null
                    )
                )
            )
        )
    }
}
