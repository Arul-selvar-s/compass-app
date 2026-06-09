package com.compass.diary.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM encryption using the Android Keystore.
 *
 * All diary content at rest is encrypted by SQLCipher (AES-256-CBC).
 * This utility encrypts INDIVIDUAL FIELDS that need extra protection —
 * e.g. the Anthropic API key stored in DataStore preferences, or
 * the database passphrase when exported.
 *
 * Usage:
 *   val encrypted = EncryptionUtil.encrypt("my secret")
 *   val plain     = EncryptionUtil.decrypt(encrypted)
 */
object EncryptionUtil {

    private const val KEYSTORE_ALIAS = "compass_diary_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128

    // ── Key management ───────────────────────────────────────────

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        ks.getEntry(KEYSTORE_ALIAS, null)?.let {
            return (it as KeyStore.SecretKeyEntry).secretKey
        }

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .also { it.init(spec) }
            .generateKey()
    }

    // ── Encrypt ──────────────────────────────────────────────────

    /**
     * Encrypts [plaintext] and returns a Base64-encoded string containing
     * the IV prepended to the ciphertext.
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val iv         = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined   = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // ── Decrypt ──────────────────────────────────────────────────

    /**
     * Decrypts a value previously produced by [encrypt].
     */
    fun decrypt(encoded: String): String {
        val combined   = Base64.decode(encoded, Base64.NO_WRAP)
        val iv         = combined.sliceArray(0 until IV_LENGTH)
        val ciphertext = combined.sliceArray(IV_LENGTH until combined.size)

        val spec   = GCMParameterSpec(TAG_LENGTH, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        }
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /**
     * Derives a database passphrase from a user secret (e.g. their compass angles + device ID).
     * Used as the SQLCipher database key.
     */
    fun deriveDbPassphrase(secret: String): ByteArray {
        val raw  = encrypt(secret)                               // Keystore-backed AES-GCM
        val hash = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        return hash                                              // 32-byte key for SQLCipher
    }
}
