package mx.tec.sonrisas.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import mx.tec.sonrisas.ui.theme.*

data class TabItem(
    val etiqueta: String,
    val icono: ImageVector
)

@Composable
fun BottomBar(
    tabs: List<TabItem>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White
    ) {
        tabs.forEachIndexed { indice, tab ->
            NavigationBarItem(
                selected = seleccionado == indice,
                onClick = { onSeleccionar(indice) },
                icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                label = { Text(tab.etiqueta, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Coral,
                    selectedTextColor = Coral,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = TextoGris,
                    unselectedTextColor = TextoGris
                )
            )
        }
    }
}

// Listas para usar en cada rol
val TabsMaestra = listOf(
    TabItem("Inicio", Icons.Outlined.Home),
    TabItem("Niños", Icons.Outlined.People),
    TabItem("Chat", Icons.Outlined.ChatBubbleOutline)
)

val TabsPadre = listOf(
    TabItem("Inicio", Icons.Outlined.Home),
    TabItem("Bitácora", Icons.AutoMirrored.Outlined.MenuBook),
    TabItem("Chat", Icons.Outlined.ChatBubbleOutline)
)

@Preview(showBackground = true)
@Composable
private fun BottomBarMaestraPreview() {
    SonrisasTheme {
        BottomBar(tabs = TabsMaestra, seleccionado = 1, onSeleccionar = {})
    }
}
