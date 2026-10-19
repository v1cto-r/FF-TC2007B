package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminAvisosScreen(
    avisos: List<AvisoUi>,
    modifier: Modifier = Modifier,
    onNuevoAviso: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 3, onSeleccionar = onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoAviso,
                containerColor = Coral,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nuevo aviso", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Avisos", subtitulo = "Comunicados de la estancia", mostrarAvatar = true)

            if (avisos.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin avisos",
                    subtitulo = "Publica el primero con el botón de abajo",
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(avisos) { aviso -> TarjetaAviso(aviso) }
            }
        }
    }
}

@Composable
private fun TarjetaAviso(aviso: AvisoUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = aviso.titulo,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = aviso.destino,
                    modifier = Modifier
                        .background(AmarilloSuave, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = TextoOscuro
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = aviso.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoOscuro
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = aviso.fecha,
                style = MaterialTheme.typography.bodySmall,
                color = TextoGris
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminAvisosPreview() {
    SonrisasTheme { AdminAvisosScreen(avisos = AdminMuestra.avisos) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminAvisosVacioPreview() {
    SonrisasTheme { AdminAvisosScreen(avisos = emptyList()) }
}