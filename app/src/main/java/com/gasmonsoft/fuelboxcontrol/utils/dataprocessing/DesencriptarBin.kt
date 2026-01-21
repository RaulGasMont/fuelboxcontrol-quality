package com.gasmonsoft.fuelboxcontrol.utils.dataprocessing

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class DesencriptarBin {

    private val key = "432dwfeasfscdsd2".toByteArray(Charsets.UTF_8)

    fun decryptFile(inputBytes: ByteArray): String {
        return try {

            val aes = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, aes)


            val decryptedBytes = cipher.doFinal(inputBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (ex: Exception) {
            throw Exception("Error al desencriptar el archivo: ${ex.message}")
        }
    }

}