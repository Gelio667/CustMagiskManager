package com.NovaStudios.custMagisk.core

import java.io.File

data class CoreLine(val level: String, val msg: String, val json: String?)

object CoreRunner {

    fun runAsRoot(core: File, args: List<String>, onLine: (CoreLine) -> Unit): Int {
        val cmd = buildString {
            append(core.absolutePath)
            for (a in args) {
                append(" ")
                append(escape(a))
            }
        }
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        p.inputStream.bufferedReader().forEachLine { parseLine(it, onLine) }
        p.errorStream.bufferedReader().forEachLine { parseLine("0|ERROR|$it", onLine) }
        return p.waitFor()
    }

    fun runNoRoot(core: File, args: List<String>, onLine: (CoreLine) -> Unit): Int {
        val arr = ArrayList<String>()
        arr.add(core.absolutePath)
        arr.addAll(args)
        val p = Runtime.getRuntime().exec(arr.toTypedArray())
        p.inputStream.bufferedReader().forEachLine { parseLine(it, onLine) }
        p.errorStream.bufferedReader().forEachLine { parseLine("0|ERROR|$it", onLine) }
        return p.waitFor()
    }

    private fun parseLine(raw: String, onLine: (CoreLine) -> Unit) {
        val parts = raw.split("|", limit = 3)
        if (parts.size < 3) return
        val level = parts[1].trim()
        val msg = parts[2]
        val json = if (level == "DATA") msg else null
        val outMsg = if (level == "DATA") "" else msg
        onLine(CoreLine(level, outMsg, json))
    }

    private fun escape(s: String): String {
        return "'" + s.replace("'", "'\\''") + "'"
    }
}