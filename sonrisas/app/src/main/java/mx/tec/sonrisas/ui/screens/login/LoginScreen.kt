package mx.tec.sonrisas.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.R
import mx.tec.sonrisas.ui.biometric.findFragmentActivity
import mx.tec.sonrisas.ui.biometric.isBiometricHardwareAvailable
import mx.tec.sonrisas.ui.biometric.launchBiometricPrompt
import mx.tec.sonrisas.ui.theme.*

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    errorMensaje: String? = null,
    esBiometricoRegistrado: Boolean = false,
    mostrarDialogoRegistroBiometrico: Boolean = false,
    onIniciarSesion: (correo: String, contrasena: String) -> Unit = { _, _ -> },
    onIniciarBiometricoExitoso: () -> Unit = {},
    onConfirmarRegistroBiometrico: () -> Unit = {},
    onOmitirRegistroBiometrico: () -> Unit = {}
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var verContrasena by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometriaDisponible = remember(context) { isBiometricHardwareAvailable(context) }

    // Al abrir la app, si hay biométricos registrados, solicita huella/rostro automáticamente
    LaunchedEffect(esBiometricoRegistrado) {
        if (esBiometricoRegistrado && biometriaDisponible && activity != null) {
            launchBiometricPrompt(
                activity = activity,
                onSuccess = onIniciarBiometricoExitoso,
                onError = { /* Fallback a contraseña manual si se cancela o falla */ }
            )
        }
    }

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
            modifier = Modifier.size(128.dp)
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
            placeholder = { Text("juan@correo.com", color = TextoGris) },
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
            placeholder = { Text("contraseña123", color = TextoGris) },
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

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onIniciarSesion(correo, contrasena) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = forma,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
            ) {
                Text("Iniciar Sesión", style = MaterialTheme.typography.labelLarge)
            }

            if (biometriaDisponible && activity != null) {
                OutlinedButton(
                    onClick = {
                        launchBiometricPrompt(
                            activity = activity,
                            onSuccess = onIniciarBiometricoExitoso,
                            onError = { /* Error or cancel */ }
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    shape = forma,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = "Iniciar con huella / biometría",
                        tint = Coral,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TextoGris,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "¿Olvidaste tu contraseña o no tienes cuenta?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                    Text(
                        text = "Ponte en contacto con tu estancia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoOscuro
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Diálogo emergente para registrar biométricos al iniciar sesión por primera vez
    if (mostrarDialogoRegistroBiometrico) {
        AlertDialog(
            onDismissRequest = onOmitirRegistroBiometrico,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "🔐 Activar Inicio Biométrico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Deseas guardar tu inicio de sesión con tu huella o rostro para ingresar automáticamente la próxima vez que abrías la app?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (activity != null) {
                            launchBiometricPrompt(
                                activity = activity,
                                titulo = "Confirmar Biométricos",
                                subtitulo = "Escanea tu huella para completar el registro",
                                onSuccess = {
                                    onConfirmarRegistroBiometrico()
                                },
                                onError = {
                                    onOmitirRegistroBiometrico()
                                }
                            )
                        } else {
                            onConfirmarRegistroBiometrico()
                        }
                    }
                ) {
                    Text("Activar Biométricos", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onOmitirRegistroBiometrico) {
                    Text("Ahora no", color = TextoGris)
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    SonrisasTheme { LoginScreen() }
}
