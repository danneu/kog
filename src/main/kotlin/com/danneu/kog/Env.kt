package com.danneu.kog

import java.io.File


// This module provides a central place to read environment variables, merging env files, vars, and properties
// into a single lookup map.
//
// Also provides helpers for parsing env vars.


private fun readEnvFile(): Map<String, String> {
    // TODO: Ensure .env is read from project root
    val file = File(".env")
    if (!file.exists()) return emptyMap()
    return file.readLines().mapNotNull { line ->
        println("line = $line")
        val parts = line.split("=", limit = 2)
        if (parts.size != 2) return@mapNotNull null
        parts[0] to parts[1]
    }.associate { it }
}


private fun readSystemProps(): Map<String, String> {
    return System.getProperties().map { entry -> entry.key.toString() to entry.value.toString() }.toMap()
}


private fun readSystemEnv(): Map<String, String> {
    return System.getenv()
}


object Env {
    val env = mutableMapOf<String, String>()
        .apply { putAll(readEnvFile()) }
        .apply { putAll(readSystemProps()) }
        .apply { putAll(readSystemEnv()) } // Highest precedence, overwrites all other sources

    fun string(key: String): String? {
        return env[key]
    }

    fun int(key: String): Int? = try {
        env[key]?.toInt()
    } catch(e: NumberFormatException) {
        null
    }

    fun long(key: String): Long? = try {
        env[key]?.toLong()
    } catch(e: NumberFormatException) {
        null
    }

    fun float(key: String): Float? = try {
        env[key]?.toFloat()
    } catch(e: NumberFormatException) {
        null
    }

    /**
     * An env var is true iff it's the string "true".
     */
    fun bool(key: String): Boolean = env[key] == "true"
}

