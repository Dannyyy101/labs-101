package tech.project101.labs.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens of the signed in user. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    /** epoch millis */
    val expiresAt: Long,
)

/**
 * Keeps the tokens encrypted with a key of the Android keystore, the counterpart of the keychain on iOS.
 * The key can't be read out of the phone, so the stored tokens are worthless anywhere else.
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun read(): AuthTokens? {
        val stored = prefs.getString(TOKENS_KEY, null) ?: return null
        // a key lost with a reset of the lock screen makes the tokens unreadable, then sign in again
        return runCatching { Json.decodeFromString<AuthTokens>(decrypt(stored)) }.getOrNull()
    }

    fun write(tokens: AuthTokens?) {
        if (tokens == null) {
            prefs.edit().remove(TOKENS_KEY).apply()
            return
        }
        prefs.edit().putString(TOKENS_KEY, encrypt(Json.encodeToString(tokens))).apply()
    }

    fun hasTokens(): Boolean = prefs.contains(TOKENS_KEY)

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.iv + cipher.doFinal(plain.toByteArray())
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val data = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, IV_LENGTH))
        return String(cipher.doFinal(data, IV_LENGTH, data.size - IV_LENGTH))
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "labs101-tokens"
        const val TOKENS_KEY = "tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}
