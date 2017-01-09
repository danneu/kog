package com.danneu.kog.batteries.multipart


// User either wants to process all file fields (rare) or just a set of expected fields (common)
sealed class Whitelist {
    object all: Whitelist() {
        override fun contains(field: String): Boolean = true
    }
    class only(val fields: Set<String> = emptySet()): Whitelist() {
        override fun contains(field: String): Boolean = fields.contains(field)
    }

    abstract fun contains(field: String): Boolean
}
