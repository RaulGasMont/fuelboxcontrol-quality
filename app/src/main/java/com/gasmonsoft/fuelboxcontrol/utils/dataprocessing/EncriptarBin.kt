package com.gasmonsoft.fuelboxcontrol.utils.dataprocessing

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncriptarBin {

    companion object {
        private val key = "432dwfeasfscdsd2".toByteArray(Charsets.UTF_8)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(IOException::class, GeneralSecurityException::class)
    fun encryptText(inputText: String): ByteArray {
        //
        val plaintext = inputText.toByteArray(Charsets.UTF_8)


        val paddedPlaintext = applyPadding(plaintext, 16)


        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)


        return cipher.doFinal(paddedPlaintext)
    }

    private fun applyPadding(data: ByteArray, blockSize: Int): ByteArray {
        val paddingRequired = blockSize - (data.size % blockSize)
        val effectivePadding = if (paddingRequired == 0) blockSize else paddingRequired

        val paddedData = ByteArray(data.size + effectivePadding)
        System.arraycopy(data, 0, paddedData, 0, data.size)


        for (i in data.size until paddedData.size) {
            paddedData[i] = effectivePadding.toByte()
        }

        return paddedData
    }
}