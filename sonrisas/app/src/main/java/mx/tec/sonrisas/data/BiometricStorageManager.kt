package mx.tec.sonrisas.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import mx.tec.sonrisas.domain.Rol

@Singleton
class BiometricStorageManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonrisas_biometrics_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REGISTERED = "biometric_registered"
        private const val KEY_USUARIO = "biometric_usuario"
        private const val KEY_NOMBRE = "biometric_nombre"
        private const val KEY_ROL = "biometric_rol"
    }

    fun isBiometricRegistered(): Boolean =
        prefs.getBoolean(KEY_REGISTERED, false)

    fun registrarBiometrico(usuario: String, nombre: String, rol: Rol) {
        prefs.edit()
            .putBoolean(KEY_REGISTERED, true)
            .putString(KEY_USUARIO, usuario)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol.name)
            .apply()
    }

    fun cancelarBiometrico() {
        prefs.edit()
            .putBoolean(KEY_REGISTERED, false)
            .remove(KEY_USUARIO)
            .remove(KEY_NOMBRE)
            .remove(KEY_ROL)
            .apply()
    }

    fun getUsuarioGuardado(): String? = prefs.getString(KEY_USUARIO, null)
    fun getNombreGuardado(): String? = prefs.getString(KEY_NOMBRE, null)
    fun getRolGuardado(): Rol? {
        val rolStr = prefs.getString(KEY_ROL, null) ?: return null
        return try {
            Rol.valueOf(rolStr)
        } catch (e: Exception) {
            null
        }
    }
}
