package com.danneu.kog.cookies

import com.danneu.kog.util.Util

val pairSplitRegExp = Regex("; *")

fun parse(str: String): MutableMap<String, String> {
    val obj = mutableMapOf<String, String>()

    str.split(pairSplitRegExp).forEach { pair ->
        pair.indexOf('=').let { idx ->
            if (idx == -1) return@forEach
            val k = pair.substring(0, idx).trim()
            // Ignore additional appearances of the same key
            if (obj.containsKey(k)) return@forEach
            val v = pair.substring(idx + 1).trim().let { v ->
                if (v.startsWith('"')) {
                    v.drop(1).dropLast(1)
                } else {
                    v
                }
            }

            obj.put(k, tryDecode(v))
        }
    }

    return obj
}

private fun tryDecode(s: String) = try {
    Util.urlDecode(s)
} catch (e: IllegalArgumentException) {
    s
}
