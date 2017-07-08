package com.danneu.kog.form

import com.danneu.kog.form.FormValue.FormList
import com.danneu.kog.form.FormValue.FormMap
import com.danneu.kog.form.FormValue.FormString
import com.danneu.result.Result
import com.danneu.result.flatMap
import java.net.URLDecoder

// A work in progress decoder for forms based on the more complete json.Decoder
//
// Experimental. Might find a way to DRY up some concepts between json.Decoder and form.Decoder
// as I make progress.

sealed class FormValue {
    // FormMap is only the top-level FormValue. FormValue does not parse nested maps like some parsers out there.
    class FormMap(val underlying: Map<String, FormValue>) : FormValue() {
        override fun toString() = underlying.toString()
    }
    class FormList(val underlying: MutableList<FormValue>) : FormValue() {
        override fun toString() = "[" + underlying.map { it.toString() }.joinToString(", ") + "]"
    }
    class FormString(val underlying: String) : FormValue() {
        override fun toString() = "\"" + underlying + "\""
    }

    companion object {
        fun decode(encoded: String?): FormValue {
            if (encoded == null) return FormMap(emptyMap())

            val data = mutableMapOf<String, FormValue>()

            return encoded
                .split("&", limit = 100)
                .map { it.split("=", limit = 2) }
                // drop "&a" and "&a="
                .filter { it.size == 2 && it[1].isNotEmpty() }
                .map { URLDecoder.decode(it[0], "utf-8") to it[1] }
                .forEach { (k, v) ->
                    if (k !in data) return@forEach run { data[k] = FormString(v) }
                    data[k]!!.let { prev ->
                        when (prev) {
                            is FormList -> prev.underlying.add(FormString(v))
                            else -> data[k] = FormList(mutableListOf(prev, FormString(v)))
                        }
                    }
                }
                .let { FormMap(data) }
        }
    }
}

class Decoder<out A>(val decode: (FormValue) -> Result<A, Exception>) {
    operator fun invoke(value: FormValue) = decode(value)

    fun <B> map(f: (A) -> B) = Decoder { value ->
        this(value).map { f(it) }
    }

    fun <B> flatMap(f: (A) -> Decoder<B>) = Decoder { value ->
        this.decode(value).flatMap { f(it).decode(value) }
    }

    companion object {
        fun parse(string: String): Result<FormValue, Exception> {
            return FormValue.decode(string).let { Result.ok(it) }
        }

        val string: Decoder<String> = Decoder { value ->
            when (value) {
                is FormString -> Result.ok(value.underlying)
                else -> Result.err(Exception("Expected string"))
            }
        }

        val int: Decoder<Int> = Decoder { value ->
            when (value) {
                is FormString -> value.underlying.toIntOrNull()?.let { Result.ok(it) }
                    ?: Result.err(Exception("Expected in"))
                else -> Result.err(Exception("Expected int"))
            }
        }

        val bool: Decoder<Boolean> = Decoder { value ->
            when (value) {
                is FormString ->
                    when (value.underlying) {
                        "true" -> Result.ok(true)
                        "false" -> Result.ok(false)
                        else -> Result.err(Exception("Expected boolean"))
                    }
                else -> Result.err(Exception("Expected boolean"))
            }
        }

        fun <T> listOf(d: Decoder<T>): Decoder<List<T>> = Decoder { value ->
            when (value) {
                is FormList ->
                    value.underlying.map { value ->
                        val result = d(value)
                        when (result) {
                            is Result.Err ->
                                @Suppress("UNCHECKED_CAST")
                                return@Decoder result as Result.Err<List<T>, Exception>
                            is Result.Ok ->
                                result.value
                        }
                    }.let { Result.ok(it) }
                else ->
                    Result.err(Exception("Expected list"))
            }
        }

        fun <T> get(key: String, decoder: Decoder<T>): Decoder<T> = Decoder { value ->
            when (value) {
                is FormMap ->
                    value.underlying[key]?.let { decoder(it) } ?: Result.err(Exception("Expected map to have key: $key"))
                else ->
                    Result.err(Exception("Excepted a map"))
            }
        }

        fun <A> oneOf(vararg ds: Decoder<A>): Decoder<A> = Decoder { value ->
            ds.asSequence().map { it(value) }.find { it is Result.Ok }
                ?: Result.Err(Exception("None of the decoders matched"))
        }

        fun <T> succeed(value: T) = Decoder { Result.ok(value) }

        fun fail(e: Exception) = Decoder { Result.err(e) }
    }
}

