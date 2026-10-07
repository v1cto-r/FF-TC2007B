package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.WarningAmber
import mx.tec.sonrisas.ui.components.TabItem

val TabsOrg = listOf(
    TabItem("Inicio", Icons.Outlined.Home),
    TabItem("Instancias", Icons.Outlined.AccountBalance),
    TabItem("Avisos", Icons.Outlined.WarningAmber)
)