package mx.tec.sonrisasdecristal.ui.theme.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores aproximados tomados del diseño (ajusta con los hex exactos de Figma)
private val Coral = Color(0xFFE07B5F)
private val CoralSuave = Color(0xFFFDE8E1)
private val TextoOscuro = Color(0xFF3D3D5C)
private val TextoGris = Color(0xFF6B6B80)
private val BordeCampo = Color(0xFFE3E3EA)

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(CoralSuave, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Coral
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Título
        Text(
            text = "Estancia Mariposa",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoOscuro
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Correo electrónico
        Text(
            text = "Correo electrónico",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = TextoGris)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral,
                unfocusedBorderColor = BordeCampo
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contraseña
        Text(
            text = "Contraseña",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextoGris)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        contentDescription = "Mostrar u ocultar contraseña",
                        tint = TextoGris
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral,
                unfocusedBorderColor = BordeCampo
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ¿Olvidaste tu contraseña?
        Text(
            text = "¿Olvidaste tu contraseña?",
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPasswordClick() },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Coral
        )

        // Espacio flexible: empuja el botón hacia abajo
        Spacer(modifier = Modifier.weight(1f))

        // Botón
        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text(
                text = "Iniciar Sesión",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pie
        Text(
            text = buildAnnotatedString {
                append("¿No tienes una cuenta? ")
                withStyle(SpanStyle(color = Coral, fontWeight = FontWeight.SemiBold)) {
                    append("Contacta tu guardería")
                }
            },
            fontSize = 13.sp,
            color = TextoGris
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLoginClick = { _, _ -> },
        onForgotPasswordClick = {}
    )
}