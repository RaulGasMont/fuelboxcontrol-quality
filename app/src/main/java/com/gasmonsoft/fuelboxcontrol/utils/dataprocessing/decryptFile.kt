package com.gasmonsoft.fuelboxcontrol.utils.dataprocessing

import android.content.Context
import android.net.Uri
import java.io.IOException

fun decryptFile(uri: Uri, context: Context): ByteArray {
    val fileBytes: ByteArray =
        readAndDecryptFile(context, uri) ?: throw IOException("No se pudo leer el archivo")

    return try {
        val decryptedString = DesencriptarBin().decryptFile(fileBytes)

        decryptedString.toByteArray()
    } catch (e: Exception) {
        throw Exception("Error al desencriptar el archivo: ${e.message}")
    }
}

private fun readAndDecryptFile(context: Context, uri: Uri): ByteArray? {
    return try {

        context.contentResolver.openInputStream(uri)?.readBytes()
    } catch (e: Exception) {
        null
    }
}