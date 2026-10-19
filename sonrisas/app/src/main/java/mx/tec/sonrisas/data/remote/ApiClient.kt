package mx.tec.sonrisas.data.remote

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import mx.tec.sonrisas.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ApiException(val code: Int, val detail: String?) : RuntimeException("HTTP $code: $detail")

@Singleton
class ApiClient @Inject constructor(
    val http: OkHttpClient,
    val json: Json
) {
    @PublishedApi internal val jsonType = "application/json".toMediaType()

    suspend fun send(
        method: String,
        path: String,
        token: String? = null,
        body: RequestBody? = null
    ): String {
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .method(method, body ?: if (method == "GET" || method == "DELETE") null else ByteArray(0).toRequestBody())
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        http.newCall(request).await().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw ApiException(response.code, detailOf(text))
            return text
        }
    }

    suspend inline fun <reified T> get(path: String, token: String?): T =
        json.decodeFromString(send("GET", path, token))

    suspend inline fun <reified B, reified T> post(path: String, body: B, token: String? = null): T =
        json.decodeFromString(send("POST", path, token, jsonBody(body)))

    suspend inline fun <reified B, reified T> put(path: String, body: B, token: String? = null): T =
        json.decodeFromString(send("PUT", path, token, jsonBody(body)))

    suspend fun delete(path: String, token: String?) {
        send("DELETE", path, token)
    }

    fun form(vararg fields: Pair<String, String>): RequestBody =
        FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()

    inline fun <reified B> jsonBody(body: B): RequestBody =
        json.encodeToString(body).toRequestBody(jsonType)

    private fun detailOf(text: String): String? = runCatching {
        (json.parseToJsonElement(text) as? JsonObject)?.get("detail")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
    })
}
