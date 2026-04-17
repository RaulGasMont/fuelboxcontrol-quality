package com.gasmonsoft.fbccalidad.utils.dataprocessing

//import android.content.Context
//import android.net.Uri
//import android.os.Build
//import androidx.annotation.RequiresApi
//import com.gasmonsoft.fuelboxcontrol.ui.sensor.encryptData
//import com.gasmonsoft.fuelboxcontrol.ui.sensor.fetchFileFromServer
//import com.gasmonsoft.fuelboxcontrol.ui.sensor.reduceData
//import com.gasmonsoft.fuelboxcontrol.ui.sensor.saveFileToAppWifi
//import com.gasmonsoft.fuelboxcontrol.ui.sensor.showToastOnMain
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//@RequiresApi(Build.VERSION_CODES.O)
//suspend fun readFile(
//    ip: String, port: String, filePath: String, fileName: String, context: Context, dia: String
//): Uri? = withContext(Dispatchers.IO) {
//    try {
//
//        // Descargar archivo del servidor
//        val downloadedUri =
//            fetchFileFromServer(ip, port.toInt(), filePath, fileName, context) ?: run {
//                showToastOnMain(context, "Error al descargar archivo")
//                return@withContext null
//            }
//
//        val decryptedData = decryptFile(downloadedUri, context)
//
//        val originalFileName = fileName
//
//        val decryptedFileName = if (originalFileName.contains('.')) {
//            "desencriptado_${originalFileName.substringBeforeLast('.')}.txt"
//        } else {
//            "desencriptado_$originalFileName.txt"
//        }
//        deleteFileIfExists(context, decryptedFileName)
//
//        saveFileToAppWifi(
//            context, decryptedData, decryptedFileName
//        )
//
//        val reducedData = reduceData(decryptedData)
//
//        val finalEncryptedData = encryptData(reducedData)
//
//        val encryptedFileName = "$dia.bin"
//        deleteFileIfExists(context, encryptedFileName)
//        val encryptedFileUri = saveFileToAppWifi(
//            context, finalEncryptedData, encryptedFileName
//        )
//        encryptedFileUri
//    } catch (e: Exception) {
//        showToastOnMain(context, "Error: ${e.message}")
//        null
//    }
//}