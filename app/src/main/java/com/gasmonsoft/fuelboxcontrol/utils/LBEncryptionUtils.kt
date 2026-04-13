package com.gasmonsoft.fuelboxcontrol.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import java.util.Base64.getEncoder
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object LBEncryptionUtils {
    private const val SECRET_KEY = "G4sm0n5oft25*cxd_"
    private const val KEY_SIZE = 256
    private const val ITERATIONS = 100000
    private const val ALGORITHM = "AES/CBC/PKCS7Padding"
    private const val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA512"

    @RequiresApi(Build.VERSION_CODES.O)
    fun encrypt(plainText: String): String {
        val salt = generateRandomBytes(16)
        val iv = generateRandomBytes(16)

        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val spec: KeySpec = PBEKeySpec(
            SECRET_KEY.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE
        )
        val tmp: SecretKey = factory.generateSecret(spec)
        val key = SecretKeySpec(tmp.encoded, "AES")
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val result = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(cipherText, 0, result, salt.size + iv.size, cipherText.size)

        return getEncoder().encodeToString(result)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decrypt(encryptedText: String): String {
        val decoded = Base64.getDecoder().decode(encryptedText)

        val salt = decoded.copyOfRange(0, 16)
        val iv = decoded.copyOfRange(16, 32)
        val cipherText = decoded.copyOfRange(32, decoded.size)

        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val spec: KeySpec = PBEKeySpec(
            SECRET_KEY.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE
        )
        val tmp: SecretKey = factory.generateSecret(spec)
        val key = SecretKeySpec(tmp.encoded, "AES")
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val plainText = cipher.doFinal(cipherText)

        return String(plainText, Charsets.UTF_8)
    }

    private fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
}