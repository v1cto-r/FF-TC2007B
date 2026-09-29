package mx.tec.sonrisasdecristal.ui.theme.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sonrisas.model.Evento

// Colores aproximados del diseño (ajusta con los hex exactos de Figma)
private val Coral = Color(0xFFE07B5F)
private val CoralSuave = Color(0xFFFDE8E1)
private val Verde = Color(0xFF7FAE96)
private val VerdeSuave = Color(0xFFE3F0E9)
private val AmarilloSuave = Color(0xFFFFF1D6)
private val Amarillo = Color(0xFFE8A93B)
private val Fondo = Color(0xFFF7F4EE)
private val TextoOscuro = Color(0xFF3D3D5C)
private val TextoGris = Color(0xFF6B6B80)

// Modelo simple para los eventos de la lista


@Composable
fun InicioMaestraScreen(
    nombreMaestra: String = "Maestra Laura",
    grupo: String = "Prescolares B",
    presentes: Int = 18,
    total: Int = 20,
    eventos: List<Evento> = listOf(
        Evento("VIE", "Reunión de Padres", "Taller mensual de entrega de reportes - 4:00 PM", true),
        Evento("LUN", "Vacunación programada", "Campaña nacional de salud infantil - 9:00 AM", false)
    ),
    onComidaClick: () -> Unit = {},
    onActividadClick: () -> Unit = {},
    onAvisoClick: () -> Unit = {}
) {
    var seccionSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Fondo,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf(
                    Pair("Inicio", Icons.Outlined.Home),
                    Pair("Niños", Icons.Outlined.People),
                    Pair("Chat", Icons.Outlined.ChatBubbleOutline)
                )
                items.forEachIndexed { index, (etiqueta, icono) ->
                    NavigationBarItem(
                        selected = seccionSeleccionada == index,
                        onClick = { seccionSeleccionada = index },
                        icon = { Icon(icono, contentDescription = etiqueta) },
                        label = { Text(etiqueta, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Coral,
                            selectedTextColor = Coral,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = TextoGris,
                            unselectedTextColor = TextoGris
                        )
                    )
                }
            }
        }
    ) { paddingInterno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Encabezado: saludo + foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, $nombreMaestra",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "Grupo: $grupo",
                        fontSize = 13.sp,
                        color = TextoGris
                    )
                }
                // Placeholder de la foto de perfil
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AmarilloSuave, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = "Foto de perfil", tint = Amarillo)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.People, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$presentes / $total niños",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Presentes el día de hoy",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Acciones rápidas
            Text(
                text = "Acciones Rápidas",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccionRapida(
                    texto = "Comida",
                    icono = Icons.Outlined.Restaurant,
                    colorIcono = Verde,
                    colorFondo = VerdeSuave,
                    onClick = onComidaClick,
                    modifier = Modifier.weight(1f)
                )
                AccionRapida(
                    texto = "Actividad",
                    icono = Icons.Outlined.Description,
                    colorIcono = Coral,
                    colorFondo = CoralSuave,
                    onClick = onActividadClick,
                    modifier = Modifier.weight(1f)
                )
                AccionRapida(
                    texto = "Aviso",
                    icono = Icons.Outlined.WarningAmber,
                    colorIcono = Amarillo,
                    colorFondo = AmarilloSuave,
                    onClick = onAvisoClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Eventos y avisos próximos
            Text(
                text = "Eventos y Avisos Próximos",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
            Spacer(modifier = Modifier.height(12.dp))
            eventos.forEach { evento ->
                EventoItem(evento)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AccionRapida(
    texto: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondo: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorFondo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = texto,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
        }
    }
}

@Composable
private fun EventoItem(evento: Evento) {
    val colorFondo = if (evento.esCoral) CoralSuave else VerdeSuave
    val colorTexto = if (evento.esCoral) Coral else Verde

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colorFondo, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = evento.dia,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = evento.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = evento.descripcion,
                    fontSize = 12.sp,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InicioMaestraScreenPreview() {
    InicioMaestraScreen()
}