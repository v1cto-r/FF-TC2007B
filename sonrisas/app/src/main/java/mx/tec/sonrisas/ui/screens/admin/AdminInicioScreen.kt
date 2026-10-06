package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.PersonAdd
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
import mx.tec.sonrisas.ui.components.AccionRapida
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminInicioScreen(
    salones: List<SalonUi>,
    alumnosSinSalon: Int,
    modifier: Modifier = Modifier,
    onCrearGrupo: () -> Unit = {},
    onInscripcion: () -> Unit = {},
    onSalonClick: (SalonUi) -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    val salonesSinMaestra = salones.filter { it.maestro == null }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 0, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Hola, Administrador",
                subtitulo = "Panel de administración",
                mostrarAvatar = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Resumen general
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

                // Acciones rápidas
                Spacer(Modifier.height(20.dp))
                TituloSeccion("Acciones Rápidas")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccionRapida(
                        texto = "Crear grupo",
                        icono = Icons.Outlined.GroupAdd,
                        colorIcono = Coral,
                        colorFondo = CoralSuave,
                        onClick = onCrearGrupo,
                        modifier = Modifier.weight(1f)
                    )
                    AccionRapida(
                        texto = "Inscripción",
                        icono = Icons.Outlined.HowToReg,
                        colorIcono = Amarillo,
                        colorFondo = AmarilloSuave,
                        onClick = onInscripcion,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Requiere atención
                Spacer(Modifier.height(20.dp))
                TituloSeccion("Requiere atención")
                Spacer(Modifier.height(8.dp))

                if (salonesSinMaestra.isEmpty() && alumnosSinSalon == 0) {
                    Text(
                        text = "Todo en orden, no hay pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (alumnosSinSalon > 0) {
                            AvisoPendiente(
                                texto = if (alumnosSinSalon == 1) "1 alumno sin salón asignado"
                                else "$alumnosSinSalon alumnos sin salón asignado"
                            )
                        }
                        salonesSinMaestra.forEach { salon ->
                            TarjetaSalon(salon = salon, onClick = { onSalonClick(salon) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoPendiente(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AmarilloSuave, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Amarillo, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminInicioPreview() {
    SonrisasTheme {
        AdminInicioScreen(
            salones = AdminMuestra.salones,
            alumnosSinSalon = AdminMuestra.alumnosSinSalon.size
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminInicioSinPendientesPreview() {
    SonrisasTheme {
        AdminInicioScreen(
            salones = AdminMuestra.salones.filter { it.maestro != null },
            alumnosSinSalon = 0
        )
    }
}