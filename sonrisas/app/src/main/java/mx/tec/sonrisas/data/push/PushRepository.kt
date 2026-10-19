package mx.tec.sonrisas.data.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.DeviceBody

@Singleton
class PushRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiClient,
    private val auth: AuthRepository
) {
    private val prefs = context.getSharedPreferences("sonrisas_push", Context.MODE_PRIVATE)

    // The app picks the id of this installation once and keeps it; the server keys the FCM token on it.
    private val deviceId: String
        get() = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }

    /** API: PUT /devices/{id} {token, platform}. Registers this installation or refreshes its token. */
    suspend fun register(token: String? = null) {
        runCatching {
            val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
            auth.authed { api.send("PUT", "/devices/$deviceId", it, api.jsonBody(DeviceBody(fcmToken))) }
        }
    }

    /** API: DELETE /devices/{id}. Call before logout so this phone stops getting the user's pushes. */
    suspend fun unregister() {
        runCatching { auth.authed { api.delete("/devices/$deviceId", it) } }
    }

    private companion object {
        const val KEY_ID = "device_id"
    }
}
