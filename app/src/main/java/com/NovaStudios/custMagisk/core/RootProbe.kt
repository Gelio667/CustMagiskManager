package com.NovaStudios.custMagisk.core

import android.content.Context

data class RootState(
    val suPath: String?,
    val suV: String?,
    val isMagiskSu: Boolean,
    val rootGranted: Boolean
)

object RootProbe {

    fun probe(context: Context): RootState {
        val core = CoreInstaller.ensureCore(context)
        val suPath = sh("command -v su")?.trim()?.ifEmpty { null }
        val suV = cmd("su", "-v")?.trim()?.ifEmpty { null }
        val isMagiskSu = (suV ?: "").uppercase().contains("MAGISKSU")
        val rootGranted = if (suPath != null) requestRoot(core.absolutePath) else false
        return RootState(suPath, suV, isMagiskSu, rootGranted)
    }

    fun requestRoot(corePath: String): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "$corePath id"))
            p.waitFor() == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun sh(cmd: String): String? {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            if (p.waitFor() == 0) out else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun cmd(vararg args: String): String? {
        return try {
            val p = Runtime.getRuntime().exec(args)
            val out = p.inputStream.bufferedReader().readText()
            if (p.waitFor() == 0) out else null
        } catch (_: Throwable) {
            null
        }
    }
}