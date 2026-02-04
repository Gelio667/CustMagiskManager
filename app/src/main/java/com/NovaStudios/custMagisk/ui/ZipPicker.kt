package com.NovaStudios.custMagisk.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import java.io.File

object ZipPicker {
    fun copyToCache(context: Context, uri: Uri): File {
        val dst = File(context.cacheDir, "module_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dst.outputStream().use { output -> input.copyTo(output) }
        }
        return dst
    }
}