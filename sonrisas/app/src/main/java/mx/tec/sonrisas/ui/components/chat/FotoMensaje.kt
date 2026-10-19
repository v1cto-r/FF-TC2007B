package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import mx.tec.sonrisas.ui.state.FotoViewModel

@Composable
fun FotoMensaje(fileId: String, modifier: Modifier = Modifier, viewModel: FotoViewModel = hiltViewModel()) {
    var url by remember(fileId) { mutableStateOf<String?>(null) }
    var intento by remember(fileId) { mutableIntStateOf(0) }
    LaunchedEffect(fileId, intento) {
        url = try { viewModel.url(fileId) } catch (e: Exception) { null }
    }
    AsyncImage(
        model = url,
        contentDescription = "Foto del mensaje",
        contentScale = ContentScale.Crop,
        onError = { if (intento == 0) intento = 1 },
        modifier = modifier.size(200.dp).clip(RoundedCornerShape(12.dp))
    )
}
