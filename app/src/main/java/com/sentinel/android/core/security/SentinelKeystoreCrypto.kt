package com.sentinel.android.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM utility for local secret encryption
 * and cryptographically strong enrollment code generation.
 */
class SentinelKeystoreCrypto {

    private val keyAlias = "sentinel_aes_master_key_v1"
    private val keyStoreProvider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val ivSizeBytes = 12
    private val tagBitLength = 128

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun isKeystoreReady(): Boolean = runCatching { getOrCreateKey(); true }.getOrDefault(false)

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encodedPayload: String): String {
        val combined = Base64.decode(encodedPayload, Base64.NO_WRAP)
        require(combined.size > ivSizeBytes) { "Invalid encrypted payload" }
        val iv = combined.copyOfRange(0, ivSizeBytes)
        val ciphertext = combined.copyOfRange(ivSizeBytes, combined.size)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(tagBitLength, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun generateEnrollmentCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val partA = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        val partB = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "SNTL-$partA-$partB"
    }

    fun sha256ShortFingerprint(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString(":") { "%02X".format(it) }
    }
}

class ActionRateLimiter(
    private val maxActionsPerWindow: Int = 5,
    private val windowMillis: Long = 30_000L
) {
    private val timestamps = mutableMapOf<String, ArrayDeque<Long>>()

    @Synchronized
    fun allow(actionKey: String): Boolean {
        val now = System.currentTimeMillis()
        val queue = timestamps.getOrPut(actionKey) { ArrayDeque() }
        while (queue.isNotEmpty() && now - queue.first() > windowMillis) {
            queue.removeFirst()
        }
        return if (queue.size < maxActionsPerWindow) {
            queue.addLast(now)
            true
        } else {
            false
        }
    }
}
