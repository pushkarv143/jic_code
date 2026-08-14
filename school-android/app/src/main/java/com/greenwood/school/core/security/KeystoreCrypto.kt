package com.greenwood.school.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption backed by a key that never leaves the Android Keystore.
 *
 * Used to encrypt the JWT pair before it is written to DataStore. Plain DataStore
 * (like SharedPreferences) is readable by anyone with filesystem access on a rooted
 * or backed-up device; the key here is non-exportable hardware-backed material on
 * devices with a TEE/StrongBox.
 *
 * Ciphertext format: `base64(iv) : base64(ciphertext)`.
 */
@Singleton
class KeystoreCrypto @Inject constructor() {

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return "${iv.base64()}$SEPARATOR${cipherText.base64()}"
    }

    /**
     * Returns null rather than throwing when the payload cannot be decrypted — that
     * happens legitimately when the Keystore key is invalidated (device credentials
     * removed, app restored to a new device). The caller treats it as "no session".
     */
    fun decrypt(payload: String): String? = runCatching {
        val (ivPart, cipherPart) = payload.split(SEPARATOR, limit = 2)
            .also { require(it.size == 2) { "Malformed ciphertext" } }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, ivPart.decodeBase64()))
        String(cipher.doFinal(cipherPart.decodeBase64()), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                // Deliberately not user-authentication-bound: the session has to survive
                // an app restart without a biometric prompt, matching the web experience.
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "greenwood_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
        const val SEPARATOR = ":"
    }
}
