package mx.tec.sonrisas.ui.state

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.data.ChatRepository

private const val LADO_MAXIMO = 1600
private const val CALIDAD_JPEG = 80

suspend fun Context.leerFoto(uri: Uri): Pair<String, ByteArray>? = withContext(Dispatchers.IO) {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // With inJustDecodeBounds, decodeStream always returns null and only fills in medidas
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
    if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return@withContext null
    var reduccion = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (reduccion * 2) >= LADO_MAXIMO) reduccion *= 2

    val opciones = BitmapFactory.Options().apply { inSampleSize = reduccion }
    val leida = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        ?: return@withContext null
    val giro = contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(leida.width, leida.height))
    val transformacion = Matrix().apply { postScale(escala, escala); postRotate(giro.toFloat()) }
    val foto = Bitmap.createBitmap(leida, 0, 0, leida.width, leida.height, transformacion, true)

    val salida = ByteArrayOutputStream()
    foto.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
    "image/jpeg" to salida.toByteArray()
}

@HiltViewModel
class FotoViewModel @Inject constructor(private val chatRepository: ChatRepository) : ViewModel() {
    suspend fun url(fileId: String): String = chatRepository.photoUrl(fileId)
}
