package com.danneu.kog.json

import com.eclipsesource.json.Json as MJson
import com.eclipsesource.json.JsonObject as MJsonObject
import com.eclipsesource.json.JsonArray as MJsonArray
import com.eclipsesource.json.JsonValue as MJsonValue

/**
 * Represents a json-serializable value.
 *
 * Use com.danneu.kog.Encoder instead of this directly for a more convenient API.
 *
 * TODO: Eventually I'd like to replace `JE.array(JE.int(1), JE.int(2))` with `JE.array(1, 2)`
 */
sealed class JsonValue {
    // COMMON

    abstract val internal: MJsonValue

    override fun toString() = internal.toString()

    // MEMBERS

    class Object(override val internal: MJsonObject): JsonValue()

    class Array(override val internal: MJsonArray): JsonValue()

    class String(v: kotlin.String): JsonValue() {
        override val internal: MJsonValue = MJson.value(v)
    }

    class Long(v: kotlin.Long): JsonValue() {
        override val internal: MJsonValue = MJson.value(v)
    }

    class Double(v: kotlin.Double): JsonValue() {
        override val internal: MJsonValue = MJson.value(v)
    }

    class Boolean(v: kotlin.Boolean): JsonValue() {
        override val internal: MJsonValue = if (v) MJson.TRUE else MJson.FALSE
    }

    object Null: JsonValue() {
        override val internal: MJsonValue = MJson.NULL
    }
}

/**
 * The intended way to produce JsonValues from Kotlin values.
 */
object Encoder {
    fun str(v: kotlin.String) = JsonValue.String(v)
    fun num(v: kotlin.Short) = JsonValue.Long(v.toLong())
    fun num(v: kotlin.Int) = JsonValue.Long(v.toLong())
    fun num(v: kotlin.Long) = JsonValue.Long(v)
    fun num(v: kotlin.Float) = JsonValue.Double(v.toDouble())
    fun num(v: kotlin.Double) = JsonValue.Double(v)
    fun bool(v: kotlin.Boolean) = JsonValue.Boolean(v)
    val `null` = JsonValue.Null

    fun obj(values: Iterable<Pair<String, JsonValue>>) = MJson.`object`().apply {
        values.forEach { (k, v) -> this.add(k, v.internal) }
    }.let { obj -> JsonValue.Object(obj) }
    fun obj(vararg values: Pair<String, JsonValue>) = obj(values.asList())
    fun obj(values: Sequence<Pair<String, JsonValue>>) = obj(values.asIterable())

    fun array(values: Iterable<JsonValue>) = (MJson.`array`() as MJsonArray).apply {
        values.forEach { v -> this.add(v.internal) }
    }.let(JsonValue::Array)
    fun array(vararg values: JsonValue) = array(values.asList())
    fun array(values: Sequence<JsonValue>) = array(values.asIterable())
}
