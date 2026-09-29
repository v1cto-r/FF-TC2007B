package mx.tec.sonrisas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AvatarCircular(
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
    imagenRes: Int? = null,
    descripcion: String? = null
) {
    if (imagenRes != null) {
        Image(
            painter = painterResource(imagenRes),
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(tamano)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(tamano)
                .background(AmarilloSuave, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = descripcion,
                tint = Amarillo,
                modifier = Modifier.size(tamano * 0.55f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarCircularPreview() {
    SonrisasTheme {
        AvatarCircular(tamano = 48.dp)
    }
}