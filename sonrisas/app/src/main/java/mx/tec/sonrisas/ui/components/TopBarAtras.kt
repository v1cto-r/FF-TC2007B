package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.*

@Composable
fun TopBarAtras(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,     // null = sin flecha
    mostrarAvatar: Boolean = false,
    avatarRes: Int? = null,
    onCerrarSesion: (() -> Unit)? = null
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onAtras != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar",
                tint = TextoOscuro,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onAtras() }
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro,
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        if (mostrarAvatar) {
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { if (onCerrarSesion != null) mostrarMenu = true }
                ) {
                    AvatarCircular(tamano = 36.dp, imagenRes = avatarRes)
                }
                DropdownMenu(
                    expanded = mostrarMenu,
                    onDismissRequest = { mostrarMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        onClick = {
                            mostrarMenu = false
                            onCerrarSesion?.invoke()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = Coral
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun TopBarAtrasPreview() {
    SonrisasTheme {
        Column {
            TopBarAtras(
                titulo = "Chat con María",
                subtitulo = "Madre de Sofía",
                onAtras = {},
                mostrarAvatar = true
            )
            TopBarAtras(titulo = "Perfil de Sofía", onAtras = {})
            TopBarAtras(titulo = "Mis Niños", subtitulo = "Lista de asistencia general")
        }
    }
}
