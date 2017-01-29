
package com.danneu.kog.json

import com.danneu.kog.result.Result
import com.danneu.kog.result.Validation
import com.danneu.kog.result.flatMap
import com.danneu.kog.result.map
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.Json
import java.io.Reader


fun Result.Companion.all(vararg results: Result<*, Exception>): Result<List<*>, Exception> {
    val validation = Validation(*results)
    return if (validation.hasFailure) {
        error(validation.failures.first())
    } else {
        Result.of(results.map(Result<*, Exception>::get))
    }
}


class Decoder <out T> (val decode: (JsonValue) -> Result<T, Exception>) {
    operator fun invoke(value: JsonValue): Result<T, Exception> {
        return decode(value)
    }

    /** Specify the decoder to use based on the result of the previous decoder.
     */
    fun <B> flatMap(f: (T) -> Decoder<B>): Decoder<B> = Decoder { value ->
        this.decode(value).flatMap { success: T ->
            f(success).decode(value)
        }
    }


    /** Apply a function to the decoded value on successful decode.
     */
    fun <B> map(f: (T) -> B): Decoder<B> = Decoder { value ->
        this.decode(value).map { success: T ->
            f(success)
        }
    }

    companion object {

        // PARSING

        fun tryParse(reader: Reader): Result<JsonValue, Exception> {
            return Result.of(Json.parse(reader))
        }

        fun tryParse(string: String): Result<JsonValue, Exception> {
            return Result.of(Json.parse(string))
        }

        // DECODERS

        val int: Decoder<Int> = Decoder {
            when {
                it.isNumber -> Result.of(it.asInt())
                else -> Result.error(Exception("Expected Int but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A> listOf(d1: Decoder<A>): Decoder<List<A>> = Decoder {
            when {
                it.isArray -> {
                    val coll = it.asArray().toList()
                    val results: List<Result<A, Exception>> = coll.map { item: JsonValue -> d1(item) }
                    val validation = Validation(*results.toTypedArray())
                    if (!validation.hasFailure) {
                        Result.of(results.map(Result<A, Exception>::get))
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected List but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A> get(key: String, d1: Decoder<A>): Decoder<A> = Decoder {
            when {
                it.isObject -> {
                    val obj = it.asObject()
                    val fieldVal = obj.get(key)
                    if (fieldVal != null) {
                        d1(fieldVal)
                    } else {
                        Result.error(Exception("Expected field \"$key\" but it was missing"))
                    }
                }
                else -> Result.error(Exception("Expected object but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A, B> map(d1: Decoder<A>, f: (A) -> B): Decoder<B> = Decoder { value ->
            d1.decode(value).map { success: A ->
                f(success)
            }
        }

        // Decoder.map2(int, int, { a, b -> a + b })
        fun <A, B, C> map2(d1: Decoder<A>, d2: Decoder<B>, f: (A, B) -> C): Decoder<C> = Decoder { value ->
            d1.decode(value).flatMap { a ->
                d2.decode(value).map { b ->
                    f(a, b)
                }
            }
        }

        val string: Decoder<String> = Decoder {
            when {
                it.isString -> Result.of(it.asString())
                else -> Result.error(Exception("Expected String but got ${it.javaClass.simpleName}"))
            }
        }


        val float: Decoder<Float> = Decoder {
            when {
                it.isNumber -> Result.of(it.asFloat())
                else -> Result.error(Exception("Expected Float but got ${it.javaClass.simpleName}"))
            }
        }

        val double: Decoder<Double> = Decoder {
            when {
                it.isNumber -> Result.of(it.asDouble())
                else -> Result.error(Exception("Expected Double but got ${it.javaClass.simpleName}"))
            }
        }

        val long: Decoder<Long> = Decoder {
            when {
                it.isNumber -> Result.of(it.asLong())
                else -> Result.error(Exception("Expected Long but got ${it.javaClass.simpleName}"))
            }
        }


        val bool: Decoder<Boolean> = Decoder {
            when {
                it.isBoolean -> Result.of(it.asBoolean())
                else -> Result.error(Exception("Expected Bool but got ${it.javaClass.simpleName}"))
            }
        }

        fun <T> `null`(defaultValue: T): Decoder<T> = Decoder {
            when {
                it.isNull -> Result.of(defaultValue)
                else -> Result.error(Exception("Expected null but got ${it.javaClass.simpleName}"))
            }
        }

        fun <T> nullable(d1: Decoder<T>): Decoder<T?> = Decoder { value ->
            when {
                value.isNull -> Result.of { null }
                else -> d1.decode(value) //.map { Option.Some(it) }
            }

        }


        inline fun <reified A> arrayOf(d1: Decoder<A>): Decoder<Array<A>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    val results: Array<Result<A, Exception>> = array.map { item: JsonValue -> d1(item) }.toTypedArray()
                    val validation = Validation(*results)
                    if (!validation.hasFailure) {
                        Result.of(results.map(Result<A, Exception>::get).toTypedArray())
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected Array but got ${it.javaClass.simpleName}"))
            }
        }

        // TODO: pairsOf(left, right)?
        fun <A> keyValuePairs(d1: Decoder<A>): Decoder<List<Pair<String, A>>> = Decoder {
            when {
                it.isObject -> {
                    val obj = it.asObject()
                    // TODO: Rewrite these so they short-circuit on failure instead of failing after parsing the whole list
                    val results = obj.toList().map { member ->
                        d1(member.value).map { Pair(member.name, it) }
                    }
                    val validation = Validation(*results.toTypedArray())
                    if (!validation.hasFailure) {
                        Result.of(results.map { result -> result.get() })
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected Object but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A> mapOf(d1: Decoder<A>): Decoder<Map<String, A>> = keyValuePairs(d1).map { it.toMap() }

        fun <A, B> pairOf(left: Decoder<A>, right: Decoder<B>): Decoder<Pair<A, B>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    if (array.size() == 2) {
                        Result.all(left(array[0]), right(array[1])).map { vals ->
                            @Suppress("UNCHECKED_CAST")
                            (vals[0] as A) to (vals[1] as B)
                        }
                    } else {
                        Result.error(Exception("Expected Pair but got array with ${array.size()} items"))
                    }
                }
                else -> Result.error(Exception("Expected Pair but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A, B, C> triple(d1: Decoder<A>, d2: Decoder<B>, d3: Decoder<C>): Decoder<Triple<A, B, C>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    if (array.size() == 3) {
                        Result.all(d1(array[0]), d2(array[1]), d3(array[2])).map { vals ->
                            @Suppress("UNCHECKED_CAST")
                            Triple(vals[0] as A, vals[1] as B, vals[2] as C)
                        }
                    } else {
                        Result.error(Exception("Expected Triple but got array with ${array.size()} items"))
                    }
                }
                else -> Result.error(Exception("Expected Pair but got ${it.javaClass.simpleName}"))
            }
        }


        fun <A> getIn(keys: List<String>, d1: Decoder<A>): Decoder<A> {
            return keys.foldRight(d1, { k, a -> get(k ,a) })
        }


        fun <A> index(i: Int, d1: Decoder<A>): Decoder<A> {
            return Decoder {
                when {
                    it.isArray -> {
                        val array = it.asArray()
                        if (i >= 0 && i < array.size()) {
                            d1(array.get(i))
                        } else {
                            Result.error(Exception("Expected index $i to be in bounds of array"))
                        }
                    }
                    else -> Result.error(Exception("Expected Array but got ${it.javaClass.simpleName}"))
                }
            }
        }


        // FIXME: Billy's first loop
        fun <A> oneOf(vararg ds: Decoder<A>): Decoder<A> = Decoder { value ->
            var result: Result<A, Exception> = Result.Failure(Exception("None of the decoders matched"))
            var bail = false
            for (decoder in ds.iterator()) {
                decoder(value).map { decoded ->
                    result = Result.of(decoded)
                    // break - can't break here
                    bail = true
                }
                if (bail) { break }
            }
            result
        }

        // TODO: Generalize `object` function and allow chaining.
        fun <A, Z> object1(f: (A) -> Z, d1: Decoder<A>): Decoder<Z> {
            return Decoder { value ->
                Result.all(d1(value)).map { vals ->
                    @Suppress("UNCHECKED_CAST")
                    f(vals[0] as A)
                }
            }
        }

        fun <A, B, Z> object2(f: (A, B) -> Z, d1: Decoder<A>, d2: Decoder<B>): Decoder<Z> {
            return Decoder { value ->
                Result.all(d1(value), d2(value)).map { vals ->
                    @Suppress("UNCHECKED_CAST")
                    f(vals[0] as A, vals[1] as B)
                }
            }
        }

        fun <A, B, C, Z> object3(f: (A, B, C) -> Z, d1: Decoder<A>, d2: Decoder<B>, d3: Decoder<C>): Decoder<Z> {
            return Decoder { value ->
                Result.all(d1(value), d2(value), d3(value)).map { vals ->
                    @Suppress("UNCHECKED_CAST")
                    f(vals[0] as A, vals[1] as B, vals[2] as C)
                }
            }
        }

        fun <T> succeed(value: T): Decoder<T> = Decoder {
            Result.of(value)
        }

        fun <T> fail(message: String): Decoder<T> = Decoder {
            Result.error(Exception(message))
        }
    }
}
