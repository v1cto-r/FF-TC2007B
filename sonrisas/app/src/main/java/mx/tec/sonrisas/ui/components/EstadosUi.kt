package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.*

@Composable
fun EstadoCargando(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Coral)
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    ilustracionRes: Int? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (ilustracionRes != null) {
                Image(
                    painter = painterResource(ilustracionRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                // Placeholder mientras no exportes la ilustración de Figma
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(CoralSuave, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inbox,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = TextoGris,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EstadoError(
    titulo: String,
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    colorAcento: Color = Coral,
    colorTinte: Color = CoralSuave
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(colorTinte, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = colorAcento,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodySmall,
            color = TextoGris,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onReintentar,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text("Reintentar", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun EstadoVacioPreview() {
    SonrisasTheme {
        EstadoVacio(
            titulo = "Sin actividades registradas",
            subtitulo = "Registra la primera actividad del día",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun EstadoErrorPreview() {
    SonrisasTheme {
        EstadoError(
            titulo = "Algo salió mal",
            mensaje = "No pudimos cargar la información",
            onReintentar = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun EstadoErrorVerdePreview() {
    SonrisasTheme {
        EstadoError(
            titulo = "Error al cargar",
            mensaje = "No pudimos cargar la lista de niños",
            onReintentar = {},
            colorAcento = Verde,
            colorTinte = VerdeSuave
        )
    }
}