package mx.tec.sonrisas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Fuente = FontFamily.Default

val Tipografia = Typography(
    // Títulos de pantalla
    titleLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // Títulos de sección y nombres
    titleMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Texto normal
    bodyLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Subtítulos y descripciones secundarias
    bodyMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Horas, detalles pequeños
    bodySmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Botones
    labelLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Etiquetas de la bottom bar
    labelSmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)