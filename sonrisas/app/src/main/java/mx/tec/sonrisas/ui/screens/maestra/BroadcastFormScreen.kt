package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo
import mx.tec.sonrisas.ui.theme.TextoGris

@Composable
fun BroadcastFormScreen(
    titulo: String,
    descripcionAyuda: String,
    textoBoton: String,
    onAtras: () -> Unit,
    onEnviar: (titulo: String, mensaje: String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()
    var tituloMensaje by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = titulo, onAtras = onAtras)
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(descripcionAyuda, style = MaterialTheme.typography.bodySmall, color = TextoGris)
                CampoTexto("Título", tituloMensaje, { tituloMensaje = it })
                CampoTexto("Mensaje", mensaje, { mensaje = it }, singleLine = false)
                Button(
                    onClick = { onEnviar(tituloMensaje, mensaje) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text(textoBoton)
                }
            }
        }
    }
}
