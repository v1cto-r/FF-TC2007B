package mx.tec.sonrisas.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens the camera; [onFoto] gets the shot as a FileProvider uri in the cache directory. Whoever
 * reads it should delete it afterwards (contentResolver.delete).
 */
@Composable
fun rememberTomarFoto(onFoto: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pendiente by remember { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = pendiente
        pendiente = null
        if (uri != null) {
            if (exito) onFoto(uri) else context.contentResolver.delete(uri, null, null)
        }
    }
    return {
        val archivo = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        pendiente = uri
        camara.launch(uri)
    }
}
