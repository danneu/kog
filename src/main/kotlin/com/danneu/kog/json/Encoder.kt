package com.danneu.kog.json

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.github.salomonbrys.kotson.jsonObject as _jsonObject
import com.github.salomonbrys.kotson.jsonArray as _jsonArray

sealed class JsonValue {
    class Object(val internal: JsonObject): JsonValue() {
        override fun toString(): String = internal.toString()
    }
    class Array(val internal: JsonArray): JsonValue() {
        override fun toString(): String = internal.toString()
    }
}

object Encoder {
    fun jsonObject(vararg values: Pair<String, *>): JsonValue.Object = JsonValue.Object(com.github.salomonbrys.kotson.jsonObject(*values))
    fun jsonObject(values: Iterable<Pair<String, *>>): JsonValue.Object = JsonValue.Object(com.github.salomonbrys.kotson.jsonObject(values))
    fun jsonObject(values: Sequence<Pair<String, *>>): JsonValue.Object = JsonValue.Object(com.github.salomonbrys.kotson.jsonObject(values))
    fun jsonArray(vararg values: Any?): JsonValue.Array = JsonValue.Array(com.github.salomonbrys.kotson.jsonArray(*values))
    fun jsonArray(values: Iterable<*>): JsonValue.Array = JsonValue.Array(com.github.salomonbrys.kotson.jsonArray(values))
    fun jsonArray(values: Sequence<*>): JsonValue.Array = JsonValue.Array(com.github.salomonbrys.kotson.jsonArray(values))
}

