package mx.tec.sonrisas.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.ChangePasswordBody
import mx.tec.sonrisas.data.remote.RefreshBody
import mx.tec.sonrisas.data.remote.TokenPairDto

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiClient,
    private val json: Json
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sonrisas_auth",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // one refresh at a time: concurrent rotations look like token reuse to the server
    private val mutex = Mutex()
    private var sessionToken: String? = null

    // The server refused the refresh token: the account was deleted, archived or signed out remotely
    private val _revocada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val revocada: SharedFlow<Unit> = _revocada.asSharedFlow()

    val hasStoredSession: Boolean get() = prefs.getString(KEY_REFRESH, null) != null

    suspend fun login(email: String, password: String): TokenPairDto =
        store(
            json.decodeFromString(
                api.send("POST", "/auth/login", body = api.form("username" to email.trim(), "password" to password))
            )
        )

    suspend fun changePassword(current: String, new: String) {
        val pair: TokenPairDto = authed { token ->
            api.post("/auth/change-password", ChangePasswordBody(current, new), token)
        }
        store(pair)
    }

    suspend fun logout() {
        val refresh = prefs.getString(KEY_REFRESH, null)
        forget()
        if (refresh != null) {
            runCatching { api.send("POST", "/auth/logout", body = api.jsonBody(RefreshBody(refresh))) }
        }
    }

    suspend fun validSessionToken(): String = mutex.withLock {
        sessionToken?.takeIf { secondsLeft(it) > 60 } ?: refreshLocked()
    }

    suspend fun <T> authed(block: suspend (token: String) -> T): T {
        val token = validSessionToken()
        return try {
            block(token)
        } catch (e: ApiException) {
            if (e.code != 401) throw e
            block(mutex.withLock { if (sessionToken == token) refreshLocked() else sessionToken ?: refreshLocked() })
        }
    }

    private suspend fun refreshLocked(): String {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: throw ApiException(401, "No hay sesión guardada")
        val pair: TokenPairDto = try {
            api.post("/auth/refresh", RefreshBody(refresh))
        } catch (e: ApiException) {
            if (e.code == 401) {
                forget()
                _revocada.tryEmit(Unit)
            }
            throw e
        }
        return store(pair).accessToken
    }

    private fun store(pair: TokenPairDto): TokenPairDto {
        sessionToken = pair.accessToken
        prefs.edit().putString(KEY_REFRESH, pair.refreshToken).apply()
        return pair
    }

    private fun forget() {
        sessionToken = null
        prefs.edit().remove(KEY_REFRESH).apply()
    }

    private fun secondsLeft(token: String): Long {
        val payload = String(Base64.decode(token.split(".")[1], Base64.URL_SAFE))
        val exp = json.parseToJsonElement(payload).jsonObject.getValue("exp").jsonPrimitive.long
        return exp - System.currentTimeMillis() / 1000
    }

    private companion object {
        const val KEY_REFRESH = "refresh_token"
    }
}
