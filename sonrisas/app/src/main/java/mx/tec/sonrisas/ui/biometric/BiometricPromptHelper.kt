package mx.tec.sonrisas.ui.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

fun isBiometricHardwareAvailable(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    val res = manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    return res == BiometricManager.BIOMETRIC_SUCCESS
}

fun launchBiometricPrompt(
    activity: FragmentActivity,
    titulo: String = "Inicio de Sesión Biométrico",
    subtitulo: String = "Usa tu huella o rostro para ingresar a Sonrisas",
    botonCancelar: String = "Usar Contraseña",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onError(errString.toString())
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onError("Huella o rostro no reconocido")
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(titulo)
        .setSubtitle(subtitulo)
        .setNegativeButtonText(botonCancelar)
        .build()

    prompt.authenticate(promptInfo)
}
