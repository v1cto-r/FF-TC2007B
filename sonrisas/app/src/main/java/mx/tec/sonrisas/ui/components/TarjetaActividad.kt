package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.ui.components.chat.FotoMensaje
import mx.tec.sonrisas.ui.theme.*

enum class TipoActividadUi { Comida, Actividad, Aviso, Salida }

data class ActividadUi(
    val tipo: TipoActividadUi,
    val titulo: String,
    val detalle: String,
    val hora: String = "",
    // Server file ids: activity photos, or the ID of whoever picked the child up
    val fotos: List<String> = emptyList()
)

private data class EstiloActividad(
    val etiqueta: String,
    val icono: ImageVector,
    val acento: Color,
    val suave: Color
)

@Composable
fun TarjetaActividad(
    actividad: ActividadUi,
    modifier: Modifier = Modifier
) {
    val estilo = when (actividad.tipo) {
        TipoActividadUi.Comida -> EstiloActividad("COMIDA", Icons.Outlined.Restaurant, Verde, VerdeSuave)
        TipoActividadUi.Actividad -> EstiloActividad("ACTIVIDAD", Icons.Outlined.Description, Amarillo, AmarilloSuave)
        TipoActividadUi.Aviso -> EstiloActividad("AVISO IMPORTANTE", Icons.Outlined.WarningAmber, Coral, CoralSuave)
        TipoActividadUi.Salida -> EstiloActividad("SALIDA", Icons.Outlined.HowToReg, Verde, VerdeSuave)
    }
    val esAviso = actividad.tipo == TipoActividadUi.Aviso
    val detalle = if (actividad.hora.isNotEmpty()) "${actividad.detalle} · ${actividad.hora}" else actividad.detalle

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (esAviso) AmarilloSuave else Color.White),
        border = if (esAviso) BorderStroke(1.dp, Amarillo.copy(alpha = 0.4f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (esAviso) 0.dp else 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (esAviso) Color.White else estilo.suave, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(estilo.icono, contentDescription = null, tint = estilo.acento)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = estilo.etiqueta,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = estilo.acento
                )
                Text(
                    text = actividad.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
        if (actividad.fotos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actividad.fotos.forEach { FotoMensaje(fileId = it, modifier = Modifier.size(96.dp)) }
            }
        }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun TarjetaActividadPreview() {
    SonrisasTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaActividad(ActividadUi(TipoActividadUi.Comida, "Comió su comida", "Sopa de fideo y tacos de pollo", "1:00 PM"))
            TarjetaActividad(ActividadUi(TipoActividadUi.Actividad, "Jugó con un rompecabezas", "Desarrollo motriz e interacción", "11:00 AM"))
            TarjetaActividad(ActividadUi(TipoActividadUi.Aviso, "Mañana traer cambio de ropa", "Se programó juego con agua al aire libre"))
        }
    }
}
