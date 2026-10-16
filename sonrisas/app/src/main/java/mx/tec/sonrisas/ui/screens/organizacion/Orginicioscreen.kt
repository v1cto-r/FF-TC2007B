package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun OrgInicioScreen(
    instancias: List<InstanciaUi>,
    modifier: Modifier = Modifier,
    nombre: String = "Sofía",
    onEnviarAviso: () -> Unit = {},
    onNuevaInstancia: () -> Unit = {},
    onTab: (Int) -> Unit = {},
    onSalir: (() -> Unit)? = null
) {
    FlagSecureDisable()
    val activas = instancias.count { it.activa }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsOrg, seleccionado = 0, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Hola, $nombre",
                subtitulo = "Administradora",
                mostrarAvatar = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Contador de instancias activas
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
                        Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (activas == 1) "1 instancia" else "$activas instancias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Activas actualmente",
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
                        texto = "Enviar aviso",
                        icono = Icons.Outlined.WarningAmber,
                        colorIcono = Amarillo,
                        colorFondo = AmarilloSuave,
                        onClick = onEnviarAviso,
                        modifier = Modifier.weight(1f)
                    )
                    AccionRapida(
                        texto = "Nueva instancia",
                        icono = Icons.Outlined.Add,
                        colorIcono = Verde,
                        colorFondo = VerdeSuave,
                        onClick = onNuevaInstancia,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Cerrar sesión
                if (onSalir != null) {
                    Spacer(Modifier.height(24.dp))
                    TextButton(
                        onClick = onSalir,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Cerrar sesión",
                            style = MaterialTheme.typography.labelLarge,
                            color = Coral
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgInicioPreview() {
    SonrisasTheme { OrgInicioScreen(instancias = OrgMuestra.instancias, onSalir = {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgInicioSinInstanciasPreview() {
    SonrisasTheme { OrgInicioScreen(instancias = emptyList()) }
}