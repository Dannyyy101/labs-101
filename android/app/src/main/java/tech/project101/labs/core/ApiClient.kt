package tech.project101.labs.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import tech.project101.labs.api.infrastructure.Serializer
import tech.project101.labs.auth.AuthException
import tech.project101.labs.auth.AuthSession
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An answer of the backend outside of 2xx, [message] is its localized error message if it sent one. */
class ApiException(val status: Int, message: String?) :
    IOException(message ?: if (status == 401) "Die Anmeldung ist abgelaufen, bitte melde dich neu an" else "Serverfehler ($status)")

/**
 * The client of the backend generated from api/openapi.json (build/generated/openapi), every request
 * carries the access token of the signed in user. A changed DTO breaks the build of the app.
 */
class ApiClient(http: OkHttpClient, auth: AuthSession) {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.serverUrl)
        .client(
            http.newBuilder()
                .addInterceptor(BearerInterceptor(auth))
                // the text extraction asks a language model, that can take a while
                .readTimeout(60, TimeUnit.SECONDS)
                .build(),
        )
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(Serializer.kotlinxSerializationJson.asConverterFactory("application/json".toMediaType()))
        .build()

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    inline fun <reified T> create(): T = create(T::class.java)
}

/** The body of a successful answer, answers outside of 2xx are thrown as [ApiException]. */
fun <T> retrofit2.Response<T>.result(): T {
    if (!isSuccessful) throw apiException()
    return body() ?: throw ApiException(code(), "Ungültige Antwort vom Server")
}

/** Like [result] for answers without a body. */
fun retrofit2.Response<*>.ensureSuccess() {
    if (!isSuccessful) throw apiException()
}

/** The body, or null for 204 No Content. */
fun <T> retrofit2.Response<T>.resultOrNull(): T? {
    if (!isSuccessful) throw apiException()
    return body()
}

@Serializable
private class ErrorBody(val errorMessage: String? = null, val message: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

// the backend answers with {"errorMessage": ...} for known errors and {"message": ...} otherwise
private fun retrofit2.Response<*>.apiException(): ApiException {
    val text = errorBody()?.string().orEmpty()
    val body = runCatching { errorJson.decodeFromString<ErrorBody>(text) }.getOrNull()
    val message = listOfNotNull(body?.errorMessage, body?.message).firstOrNull { it.isNotBlank() }
    return ApiException(code(), message)
}

/** Adds the access token and tries once more with a fresh one when the backend rejects it. */
private class BearerInterceptor(private val auth: AuthSession) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // interceptors run on the threads of okhttp, blocking them for the token is fine
        val token = runBlocking { auth.accessToken() }
        val response = chain.proceed(chain.request().withToken(token))
        if (response.code != 401) return response

        response.close()
        val refreshed = runBlocking { auth.accessToken(forceRefresh = true) }
        return chain.proceed(chain.request().withToken(refreshed))
    }

    private fun okhttp3.Request.withToken(token: String) =
        newBuilder().header("Authorization", "Bearer $token").build()
}

/** A short German text for the user. */
val Throwable.userMessage: String
    get() = when (this) {
        is ApiException, is AuthException -> message.orEmpty()
        is IOException -> "Keine Verbindung zum Server"
        else -> message ?: "Unbekannter Fehler"
    }
