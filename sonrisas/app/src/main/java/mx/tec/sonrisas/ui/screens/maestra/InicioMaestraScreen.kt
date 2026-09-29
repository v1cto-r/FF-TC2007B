package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
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
import mx.tec.sonrisas.model.Evento
import mx.tec.sonrisas.model.Mocks
import mx.tec.sonrisas.model.UiState
import mx.tec.sonrisas.ui.components.AccionRapida
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.ItemEvento
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun InicioMaestraScreen(
    estado: UiState<List<Evento>>,
    modifier: Modifier = Modifier,
    presentes: Int = 0,
    total: Int = 0,
    onComida: () -> Unit = {},
    onActividad: () -> Unit = {},
    onAviso: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 0, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Hola, Maestra Laura",
                subtitulo = "Sala 3 · Prescolares B",
                mostrarAvatar = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    is UiState.Error -> EstadoError(
                        titulo = "Algo salió mal",
                        mensaje = "No pudimos cargar la información",
                        onReintentar = onReintentar
                    )

                    UiState.Vacio -> InicioMaestraContenido(
                        tituloAsistencia = "Sin asistencia",
                        subtituloAsistencia = "Aún no se ha pasado lista",
                        eventos = emptyList(),
                        onComida = onComida,
                        onActividad = onActividad,
                        onAviso = onAviso
                    )

                    is UiState.Exito -> InicioMaestraContenido(
                        tituloAsistencia = "$presentes / $total niños",
                        subtituloAsistencia = "Presentes hoy en tu sala",
                        eventos = estado.datos,
                        onComida = onComida,
                        onActividad = onActividad,
                        onAviso = onAviso
                    )
                }
            }
        }
    }
}

@Composable
private fun InicioMaestraContenido(
    tituloAsistencia: String,
    subtituloAsistencia: String,
    eventos: List<Evento>,
    onComida: () -> Unit,
    onActividad: () -> Unit,
    onAviso: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Tarjeta verde de asistencia
        Row(
            modifier = Modifier
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
                Icon(Icons.Outlined.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = tituloAsistencia,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtituloAsistencia,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccionRapida(
                texto = "Comida",
                icono = Icons.Outlined.Restaurant,
                colorIcono = Verde,
                colorFondo = VerdeSuave,
                onClick = onComida,
                modifier = Modifier.weight(1f)
            )
            AccionRapida(
                texto = "Actividad",
                icono = Icons.Outlined.Extension,
                colorIcono = Amarillo,
                colorFondo = AmarilloSuave,
                onClick = onActividad,
                modifier = Modifier.weight(1f)
            )
            AccionRapida(
                texto = "Aviso",
                icono = Icons.Outlined.Campaign,
                colorIcono = Coral,
                colorFondo = CoralSuave,
                onClick = onAviso,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Eventos y Avisos Próximos",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(8.dp))
        if (eventos.isEmpty()) {
            Text(
                text = "No hay eventos próximos",
                style = MaterialTheme.typography.bodySmall,
                color = TextoGris
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eventos.forEach { ItemEvento(evento = it) }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioMaestraPreview() {
    SonrisasTheme {
        InicioMaestraScreen(
            estado = UiState.Exito(Mocks.eventos),
            presentes = 18,
            total = 20
        )
    }
}