package mx.tec.sonrisas.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.screens.organizacion.CampoContrasenaTemporal
import mx.tec.sonrisas.ui.theme.*

private const val LARGO_MINIMO = 8

@Composable
fun CambiarContrasenaScreen(
    modifier: Modifier = Modifier,
    errorMensaje: String? = null,
    guardando: Boolean = false,
    onGuardar: (actual: String, nueva: String) -> Unit = { _, _ -> },
    onSalir: () -> Unit = {}
) {
    FlagSecureEnable()
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    val problema = when {
        nueva.isNotEmpty() && nueva.length < LARGO_MINIMO -> "La contraseña nueva debe tener al menos $LARGO_MINIMO caracteres."
        nueva.isNotEmpty() && nueva == actual -> "La contraseña nueva debe ser distinta de la actual."
        confirmacion.isNotEmpty() && confirmacion != nueva -> "Las contraseñas nuevas no coinciden."
        else -> null
    }
    val listo = actual.isNotEmpty() && nueva.isNotEmpty() && confirmacion.isNotEmpty() && problema == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Fondo)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo de Estancia Mariposa",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(128.dp)
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Elige tu contraseña",
            style = MaterialTheme.typography.titleLarge,
            color = TextoOscuro
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CoralSuave)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Coral, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Es tu primer inicio de sesión. Cambia la contraseña temporal por una que solo tú conozcas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoOscuro
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        CampoContrasenaTemporal("Contraseña temporal", actual, { actual = it })
        Spacer(Modifier.height(16.dp))
        CampoContrasenaTemporal("Contraseña nueva", nueva, { nueva = it })
        Spacer(Modifier.height(16.dp))
        CampoContrasenaTemporal("Confirmar contraseña nueva", confirmacion, { confirmacion = it })

        val mensaje = problema ?: errorMensaje
        if (mensaje != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = mensaje,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onGuardar(actual, nueva) },
            enabled = listo && !guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
        ) {
            Text(if (guardando) "Guardando..." else "Guardar contraseña", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSalir) {
            Text("Cerrar sesión", color = TextoGris)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CambiarContrasenaScreenPreview() {
    SonrisasTheme { CambiarContrasenaScreen() }
}
