package com.NovaStudios.custMagisk.core

import android.content.Context
import android.os.Build
import java.io.File

object CoreInstaller {

    fun ensureCore(context: Context): File {
        val abi = pickAbi()
        val dst = File(context.filesDir, "core")
        if (dst.exists() && dst.length() > 0L) return dst
        context.assets.open("core/$abi/core").use { input ->
            dst.outputStream().use { output -> input.copyTo(output) }
        }
        Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod 755 ${dst.absolutePath}")).waitFor()
        return dst
    }

    private fun pickAbi(): String {
        val abis = Build.SUPPORTED_ABIS.toList()
        if (abis.any { it.contains("arm64") }) return "arm64-v8a"
        return "armeabi-v7a"
    }
}