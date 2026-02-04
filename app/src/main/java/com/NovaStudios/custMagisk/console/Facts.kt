package com.NovaStudios.custMagisk.console

import android.content.Context

fun loadFacts(context: Context): List<String> {
    val text = context.assets.open("facts.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
    return parseQuotedBlocks(text).map { it.trim() }.filter { it.isNotEmpty() }
}

private fun parseQuotedBlocks(text: String): List<String> {
    val out = ArrayList<String>()
    val sb = StringBuilder()
    var inQuote = false
    var escape = false

    for (ch in text) {
        if (!inQuote) {
            if (ch == '"') {
                inQuote = true
                sb.setLength(0)
                escape = false
            }
        } else {
            if (escape) {
                sb.append(ch)
                escape = false
            } else {
                when (ch) {
                    '\\' -> escape = true
                    '"' -> {
                        out.add(sb.toString())
                        inQuote = false
                    }
                    else -> sb.append(ch)
                }
            }
        }
    }
    return out
}