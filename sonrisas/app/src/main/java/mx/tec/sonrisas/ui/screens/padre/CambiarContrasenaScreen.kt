package mx.tec.sonrisas.ui.screens.padre

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.theme.*

@Composable
fun CambiarContrasenaScreen(
    onGuardarContrasena: (nuevaContrasena: String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()

    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var verNueva by remember { mutableStateOf(false) }
    var verConfirmar by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val forma = RoundedCornerShape(12.dp)
    val colorCampos = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Coral,
        unfocusedBorderColor = BordeCampo,
        cursorColor = Coral
    )

    Scaffold(
        modifier = modifier,
        containerColor = Fondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo de Sonrisas de Cristal",
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "🔐 Actualiza tu Contraseña",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Por seguridad, al ser tu primer inicio de sesión debes establecer una contraseña personal para proteger la información de tu familia.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoGris
            )

            Spacer(Modifier.height(28.dp))

            // Nueva Contraseña
            Text(
                text = "Nueva Contraseña",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = nuevaContrasena,
                onValueChange = {
                    nuevaContrasena = it
                    errorMensaje = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe tu nueva contraseña", color = TextoGris) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextoGris) },
                trailingIcon = {
                    IconButton(onClick = { verNueva = !verNueva }) {
                        Icon(
                            imageVector = if (verNueva) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (verNueva) "Ocultar" else "Mostrar",
                            tint = TextoGris
                        )
                    }
                },
                visualTransformation = if (verNueva) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = forma,
                colors = colorCampos,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(16.dp))

            // Confirmar Contraseña
            Text(
                text = "Confirmar Nueva Contraseña",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = confirmarContrasena,
                onValueChange = {
                    confirmarContrasena = it
                    errorMensaje = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Repite tu nueva contraseña", color = TextoGris) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextoGris) },
                trailingIcon = {
                    IconButton(onClick = { verConfirmar = !verConfirmar }) {
                        Icon(
                            imageVector = if (verConfirmar) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (verConfirmar) "Ocultar" else "Mostrar",
                            tint = TextoGris
                        )
                    }
                },
                visualTransformation = if (verConfirmar) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = forma,
                colors = colorCampos,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (errorMensaje != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMensaje!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (nuevaContrasena.length < 6) {
                        errorMensaje = "La contraseña debe tener al menos 6 caracteres"
                    } else if (nuevaContrasena != confirmarContrasena) {
                        errorMensaje = "Las contraseñas no coinciden"
                    } else {
                        onGuardarContrasena(nuevaContrasena)
                    }
                },
                enabled = nuevaContrasena.isNotBlank() && confirmarContrasena.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = forma,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
            ) {
                Text("Guardar y Continuar ➔", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CambiarContrasenaPreview() {
    SonrisasTheme {
        CambiarContrasenaScreen(onGuardarContrasena = {})
    }
}
