package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminChatTeacherListScreen(
    maestros: List<MaestroUi>,
    modifier: Modifier = Modifier,
    onMaestroClick: (MaestroUi) -> Unit = {},
    onAtras: () -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Profesores", subtitulo = "Selecciona un profesor para chatear", onAtras = onAtras)

            if (maestros.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin profesores",
                    subtitulo = "No hay profesores registrados en esta estancia",
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(maestros) { maestro ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onMaestroClick(maestro) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircular(tamano = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = maestro.nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Text(
                                    text = maestro.correo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoGris
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
