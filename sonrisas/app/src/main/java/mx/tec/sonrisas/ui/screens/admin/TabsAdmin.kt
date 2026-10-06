package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HowToReg
import mx.tec.sonrisas.ui.components.TabItem

val TabsAdmin = listOf(
    TabItem("Inicio", Icons.Outlined.Home),
    TabItem("Salones", Icons.Outlined.Groups),
    TabItem("Inscripción", Icons.Outlined.HowToReg)
)