package com.gasmonsoft.fuelboxcontrol.utils.dataprocessing

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

fun deleteFileIfExists(context: Context, fileName: String): Boolean {
    val filesCollection = MediaStore.Files.getContentUri("external")
    val selection =
        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " + "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(
        fileName, Environment.DIRECTORY_DOCUMENTS + "/AppWifi"
    )

    val deleted = context.contentResolver.query(
        filesCollection, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val uri = ContentUris.withAppendedId(filesCollection, id)
            context.contentResolver.delete(uri, null, null) > 0
        } else false
    } ?: false

    if (!deleted) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, fileName)
        return file.delete()
    }
    return true
}
