package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
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
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminSalonesScreen(
    salones: List<SalonUi>,
    modifier: Modifier = Modifier,
    onSalonClick: (SalonUi) -> Unit = {},
    onCrearGrupo: () -> Unit = {},
    onCerrarSesion: (() -> Unit)? = null,
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 1, onSeleccionar = onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCrearGrupo,
                containerColor = Coral,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Crear grupo", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Salones",
                subtitulo = "Panel de administración",
                mostrarAvatar = true,
                onCerrarSesion = onCerrarSesion
            )

            if (salones.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin salones",
                    subtitulo = "Crea el primer grupo con el botón de abajo",
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .background(Verde, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${salones.size} salones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${salones.sumOf { it.alumnos.size }} alumnos en total",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(salones) { salon ->
                    TarjetaSalon(salon = salon, onClick = { onSalonClick(salon) })
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminSalonesPreview() {
    SonrisasTheme { AdminSalonesScreen(salones = AdminMuestra.salones) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminSalonesVacioPreview() {
    SonrisasTheme { AdminSalonesScreen(salones = emptyList()) }
}
