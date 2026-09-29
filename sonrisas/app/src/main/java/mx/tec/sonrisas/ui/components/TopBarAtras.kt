package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    avatarRes: Int? = null
) {
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
        if (mostrarAvatar) {
            AvatarCircular(tamano = 36.dp, imagenRes = avatarRes)
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