package tech.project101.labs.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import tech.project101.labs.core.AppConfig
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

sealed class AuthException(message: String) : IOException(message) {
    data object NotConfigured : AuthException("AUTH_ISSUER und AUTH_CLIENT_ID fehlen in der Konfiguration")
    data object NotSignedIn : AuthException("Du bist nicht angemeldet")
    data object InvalidCallback : AuthException("Die Anmeldung wurde abgebrochen")
    class Server(message: String) : AuthException(message)

    /** the code or refresh token is not valid (anymore), only then the user has to sign in again */
    class Rejected(message: String) : AuthException(message)
}

/**
 * Sign in at Zitadel with the authorization code flow and PKCE, the app is a public client without secret.
 * Hands out access tokens for the backend and refreshes them before they run out.
 */
class AuthSession(context: Context, private val store: TokenStore, private val http: OkHttpClient) {
    private val prefs = context.getSharedPreferences("auth-pending", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private var tokens: AuthTokens? = store.read()
    private var endpoints: Endpoints? = null
    // concurrent requests share one refresh, zitadel rotates the refresh token
    private val refreshLock = Mutex()

    private val _isSignedIn = MutableStateFlow(tokens != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    // MARK: sign in and out

    /**
     * The login page to open in a custom tab, zitadel redirects to [REDIRECT_URI] afterwards.
     * Verifier and state are stored, android may end the app while the user is on the login page.
     */
    suspend fun startSignIn(): Uri {
        if (!AppConfig.isComplete) throw AuthException.NotConfigured
        val endpoints = discover()
        val verifier = randomString()
        val state = randomString()
        prefs.edit().putString(VERIFIER_KEY, verifier).putString(STATE_KEY, state).apply()

        return Uri.parse(endpoints.authorization).buildUpon()
            .appendQueryParameter("client_id", AppConfig.authClientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            // offline_access for the refresh token
            .appendQueryParameter("scope", "openid profile email offline_access")
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            // like the ephemeral session on iOS, don't sign in silently with an old session of the browser
            .appendQueryParameter("prompt", "login")
            .build()
    }

    /** Exchanges the code of the redirect for tokens. */
    suspend fun completeSignIn(callback: Uri) {
        val verifier = prefs.getString(VERIFIER_KEY, null)
        val state = prefs.getString(STATE_KEY, null)
        prefs.edit().clear().apply()

        val code = callback.getQueryParameter("code")
        if (verifier == null || state == null || callback.getQueryParameter("state") != state || code == null) {
            throw AuthException.InvalidCallback
        }

        val received = requestTokens(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "code_verifier" to verifier,
            ),
            previous = null,
        )
        store(received)
    }

    fun signOut() {
        store(null)
    }

    // MARK: tokens

    /** A valid access token, refreshed if needed. Throws [AuthException.NotSignedIn] if the refresh token was rejected. */
    suspend fun accessToken(forceRefresh: Boolean = false): String {
        val current = tokens ?: throw AuthException.NotSignedIn
        if (!forceRefresh && current.expiresAt - System.currentTimeMillis() > REFRESH_MARGIN_MS) {
            return current.accessToken
        }

        return refreshLock.withLock {
            val latest = tokens ?: throw AuthException.NotSignedIn
            // another request refreshed while this one waited for the lock
            if (latest.accessToken != current.accessToken) return@withLock latest.accessToken
            refresh(latest).accessToken
        }
    }

    private suspend fun refresh(current: AuthTokens): AuthTokens {
        val refreshToken = current.refreshToken
        if (refreshToken == null) {
            store(null)
            throw AuthException.NotSignedIn
        }

        try {
            // zitadel rotates the refresh token, a cancelled request would lose the new one and sign the user out
            val refreshed = withContext(NonCancellable) {
                requestTokens(mapOf("grant_type" to "refresh_token", "refresh_token" to refreshToken), previous = current)
            }
            store(refreshed)
            return refreshed
        } catch (_: AuthException.Rejected) {
            // the refresh token expired or was revoked, the user has to sign in again.
            // any other error (offline, zitadel down) keeps the tokens for the next try
            store(null)
            throw AuthException.NotSignedIn
        }
    }

    private fun store(tokens: AuthTokens?) {
        this.tokens = tokens
        store.write(tokens)
        _isSignedIn.value = tokens != null
    }

    // MARK: requests

    private class Endpoints(val authorization: String, val token: String)

    @Serializable
    private class Discovery(
        @SerialName("authorization_endpoint") val authorizationEndpoint: String,
        @SerialName("token_endpoint") val tokenEndpoint: String,
    )

    @Serializable
    private class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
    )

    @Serializable
    private class ErrorResponse(
        val error: String,
        @SerialName("error_description") val errorDescription: String? = null,
    )

    private suspend fun discover(): Endpoints {
        endpoints?.let { return it }
        if (!AppConfig.isComplete) throw AuthException.NotConfigured

        val body = withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url("${AppConfig.authIssuer}/.well-known/openid-configuration").build())
                .execute()
                .use { response ->
                    if (!response.isSuccessful) throw AuthException.Server("Anmeldeserver nicht erreichbar (${response.code})")
                    response.body.string()
                }
        }
        val discovery = json.decodeFromString<Discovery>(body)
        return Endpoints(discovery.authorizationEndpoint, discovery.tokenEndpoint).also { endpoints = it }
    }

    private suspend fun requestTokens(parameters: Map<String, String>, previous: AuthTokens?): AuthTokens {
        val tokenUrl = discover().token
        val form = FormBody.Builder().apply {
            (parameters + ("client_id" to AppConfig.authClientId)).forEach { (key, value) -> add(key, value) }
        }.build()

        val (code, body) = withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url(tokenUrl).post(form).build()).execute().use { it.code to it.body.string() }
        }

        if (code !in 200..299) {
            val error = runCatching { json.decodeFromString<ErrorResponse>(body) }.getOrNull()
            if (error?.error == "invalid_grant") {
                throw AuthException.Rejected(error.errorDescription ?: "Die Anmeldung ist abgelaufen, bitte melde dich neu an")
            }
            throw AuthException.Server(error?.errorDescription ?: error?.error ?: "Anmeldung fehlgeschlagen")
        }

        val received = json.decodeFromString<TokenResponse>(body)
        return AuthTokens(
            accessToken = received.accessToken,
            // keep the old one if zitadel sent none
            refreshToken = received.refreshToken ?: previous?.refreshToken,
            expiresAt = System.currentTimeMillis() + (received.expiresIn ?: 300) * 1000,
        )
    }

    // MARK: PKCE

    private fun randomString(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return base64Url(bytes)
    }

    private fun codeChallenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    companion object {
        const val REDIRECT_URI = "labs101://auth/callback"
        // refresh a minute early, so the token doesn't run out on the way to the backend
        private const val REFRESH_MARGIN_MS = 60_000L
        private const val VERIFIER_KEY = "verifier"
        private const val STATE_KEY = "state"
    }
}
