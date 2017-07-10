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

class Decoder<out A>(val decode: (FormValue) -> Result<A, String>) {
    operator fun invoke(value: FormValue) = decode(value)

    fun <B> map(f: (A) -> B) = Decoder { value ->
        this(value).map { f(it) }
    }

    fun <B> flatMap(f: (A) -> Decoder<B>) = Decoder { value ->
        this.decode(value).flatMap { f(it).decode(value) }
    }

    companion object {
        fun parse(string: String): Result<FormValue, String> {
            return FormValue.decode(string).let { Result.ok(it) }
        }

        val string: Decoder<String> = Decoder { value ->
            when (value) {
                is FormString -> Result.ok(value.underlying)
                else -> Result.err("Expected string")
            }
        }

        val int: Decoder<Int> = Decoder { value ->
            when (value) {
                is FormString -> value.underlying.toIntOrNull()?.let { Result.ok(it) }
                    ?: Result.err("Expected in")
                else -> Result.err("Expected int")
            }
        }

        val bool: Decoder<Boolean> = Decoder { value ->
            when (value) {
                is FormString ->
                    when (value.underlying) {
                        "true" -> Result.ok(true)
                        "false" -> Result.ok(false)
                        else -> Result.err("Expected boolean")
                    }
                else -> Result.err("Expected boolean")
            }
        }

        fun <T> listOf(decoder: Decoder<T>): Decoder<List<T>> = Decoder { formValue ->
            when (formValue) {
                is FormList ->
                    formValue.underlying.map { value ->
                        decoder(value).let { result ->
                            when (result) {
                                is Result.Err ->
                                    return@Decoder Result.err(result.error)
                                is Result.Ok ->
                                    result.value
                            }
                        }
                    }.let(Result.Companion::ok)
                else ->
                    Result.err("Expected list")
            }
        }

        fun <T> get(key: String, decoder: Decoder<T>): Decoder<T> = Decoder { formValue ->
            when (formValue) {
                is FormMap ->
                    formValue.underlying[key]?.let { decoder(it) } ?:
                        Result.err("Expected map to have key: $key")
                else ->
                    Result.err("Excepted a map")
            }
        }

        fun <A> oneOf(decoders: Iterable<Decoder<A>>): Decoder<A> = Decoder { formValue ->
            for (decoder in decoders) {
                decoder(formValue).let { result ->
                    if (result is Result.Ok) {
                        return@Decoder result
                    }
                }
            }

            Result.err("None of the decoders matched")
        }

        fun <A> oneOf(vararg decoders: Decoder<A>): Decoder<A> = Companion.oneOf(decoders.asIterable())

        fun <T> succeed(value: T) = Decoder {
            Result.ok(value)
        }

        fun fail(message: String) = Decoder {
            Result.err(message)
        }
    }
}

