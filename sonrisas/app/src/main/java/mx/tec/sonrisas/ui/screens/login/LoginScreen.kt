package mx.tec.sonrisas.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.theme.*

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    errorMensaje: String? = null,
    onIniciarSesion: (correo: String, contrasena: String) -> Unit = { _, _ -> },
    onOlvideContrasena: () -> Unit = {},
    onContactarSoporte: () -> Unit = {}
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var verContrasena by remember { mutableStateOf(false) }

    val forma = RoundedCornerShape(12.dp)
    val colorCampos = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Coral,
        unfocusedBorderColor = BordeCampo,
        cursorColor = Coral
    )

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
            modifier = Modifier.size(96.dp)
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Estancia Mariposa",
            style = MaterialTheme.typography.titleLarge,
            color = TextoOscuro
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CoralSuave)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Usuarios para pruebas:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        text = "• Maestra: maestra@sonrisas.edu.mx (o 'maestra') / 123",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoOscuro
                    )
                    Text(
                        text = "• Padre: padre@sonrisas.edu.mx (o 'padre') / 123",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoOscuro
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Correo electrónico",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("correo@ejemplo.com", color = TextoGris) },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = TextoGris) },
            singleLine = true,
            shape = forma,
            colors = colorCampos,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Contraseña",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("••••••••", color = TextoGris) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextoGris) },
            trailingIcon = {
                IconButton(onClick = { verContrasena = !verContrasena }) {
                    Icon(
                        imageVector = if (verContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (verContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = TextoGris
                    )
                }
            },
            visualTransformation = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = forma,
            colors = colorCampos,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (errorMensaje != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMensaje,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "¿Olvidaste tu contraseña?",
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = onOlvideContrasena),
            style = MaterialTheme.typography.bodySmall,
            color = Coral
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onIniciarSesion(correo, contrasena) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = forma,
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
        ) {
            Text("Iniciar Sesión", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "¿No tienes una cuenta? ",
                style = MaterialTheme.typography.bodySmall,
                color = TextoGris
            )
            Text(
                text = "Contacta a soporte",
                modifier = Modifier.clickable(onClick = onContactarSoporte),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Coral
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    SonrisasTheme { LoginScreen() }
}
