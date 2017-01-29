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

/**
 * Represents an immutable group of environment variables.
 *
 * NOTE: I factored the singleton object into a singleton instance EnvContainer so that I could implement a .fork()
 *       override for testing. Not yet sure if I want a different API.
 */
class EnvContainer(val env: Map<String, String> = emptyMap()) {
    /**
     * Create a new container from an existing one and merge in additional key/vals.
     * Any null values in the override will delete those keys from the map.
     *
     * Created this to help with testing.
     */
    fun fork(overrides: Map<String, String?>): EnvContainer {
        return EnvContainer(env.plus(overrides).filterValues { it != null } as Map<String, String>)
    }

    // READERS

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

    fun bool(key: String): Boolean = env[key] == "true" || env[key] == "1"
}

/**
 * Singleton env container that reads from .env, system properties, and the system environment variables.
 *
 * Usually you just want to use this.
 */
val Env = emptyMap<String, String>()
    .plus(readEnvFile())
    .plus(readSystemProps())
    .plus(readSystemEnv()) // Highest precedence, overwrites all other sources
    .let(::EnvContainer)
