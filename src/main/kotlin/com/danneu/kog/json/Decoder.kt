
package com.danneu.kog.json

import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.Json
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.Validation
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import org.funktionale.option.Option
import java.io.Reader


// TODO: Wrap initial parse in result (Is this TODO still relevant?)


fun Result.Companion.all(vararg results: Result<*, Exception>): Result<List<*>, Exception> {
    val validation = Validation(*results)
    if (validation.hasFailure) {
        return error(validation.failures.first())
    } else {
        val vals: List<Any> = results.map { it.get() }
        return Result.of { vals }
    }
}


class Decoder <out T : Any> (val decode: (JsonValue) -> Result<T, Exception>) {
    operator fun invoke(value: JsonValue): Result<T, Exception> {
        return decode(value)
    }

    fun <B : Any> flatMap(f: (T) -> Decoder<B>): Decoder<B> = Decoder { value ->
        this.decode(value).flatMap { success: T ->
            f(success).decode(value)
        }
    }


    fun <B : Any> map(f: (T) -> B): Decoder<B> = Decoder { value ->
        this.decode(value).map { success: T ->
            f(success)
        }
    }

    companion object {

        // PARSING

        fun tryParse(reader: Reader): Result<JsonValue, Exception> {
            return Result.of { Json.parse(reader) }
        }
        fun tryParse(string: String): Result<JsonValue, Exception> {
            return Result.of { Json.parse(string) }
        }

        // DECODERS

        val int: Decoder<Int> = Decoder {
            when {
                it.isNumber -> Result.of { it.asInt() }
                else -> Result.error(Exception("Expected Int but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A : Any> listOf(d1: Decoder<A>): Decoder<List<A>> = Decoder {
            when {
                it.isArray -> {
                    val coll = it.asArray().toList()
                    val results: List<Result<A, Exception>> = coll.map { item: JsonValue -> d1(item) }
                    val validation = Validation(*results.toTypedArray())
                    if (!validation.hasFailure) {
                        Result.of { results.map { result -> result.get() } }
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected List but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A : Any> get(key: String, d1: Decoder<A>): Decoder<A> = Decoder {
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

        fun <A : Any, B : Any> map(d1: Decoder<A>, f: (A) -> B): Decoder<B> = Decoder { value ->
            d1.decode(value).map { success: A ->
                f(success)
            }
        }

        // Decoder.map2(int, int, { a, b -> a + b })
        fun <A : Any, B : Any, C : Any> map2(d1: Decoder<A>, d2: Decoder<B>, f: (A, B) -> C): Decoder<C> = Decoder { value ->
            d1.decode(value).flatMap { a ->
                d2.decode(value).map { b ->
                    f(a, b)
                }
            }
        }

        val string: Decoder<String>
            get() = Decoder {
                when {
                    it.isString -> Result.of { it.asString() }
                    else -> Result.error(Exception("Expected String but got ${it.javaClass.simpleName}"))
                }
            }



        val float: Decoder<Float>
            get() = Decoder {
                when {
                    it.isNumber -> Result.of { it.asFloat() }
                    else -> Result.error(Exception("Expected Float but got ${it.javaClass.simpleName}"))
                }
            }

        val double: Decoder<Double>
            get() = Decoder {
                when {
                    it.isNumber -> Result.of { it.asDouble() }
                    else -> Result.error(Exception("Expected Double but got ${it.javaClass.simpleName}"))
                }
            }

        val long: Decoder<Long>
            get() = Decoder {
                when {
                    it.isNumber -> Result.of { it.asLong() }
                    else -> Result.error(Exception("Expected Long but got ${it.javaClass.simpleName}"))
                }
            }


        val bool: Decoder<Boolean>
            get() = Decoder {
                when {
                    it.isBoolean -> Result.of { it.asBoolean() }
                    else -> Result.error(Exception("Expected Bool but got ${it.javaClass.simpleName}"))
                }
            }

        fun <T : Any> `null`(defaultValue: T): Decoder<T> = Decoder {
            when {
                it.isNull -> Result.of(defaultValue)
                else -> Result.error(Exception("Expected null but got ${it.javaClass.simpleName}"))
            }
        }

        // TODO: (Maybe?) Make this return V? once Result removes <V: Any> restriction.
        fun <T: Any> nullable(d1: Decoder<T>): Decoder<Option<T>> = Decoder { value ->
            when {
                value.isNull -> Result.of { Option.None }
                else -> d1.decode(value).map { Option.Some(it) }
            }

        }


        inline fun <reified A : Any> arrayOf(d1: Decoder<A>): Decoder<Array<A>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    val results: Array<Result<A, Exception>> = array.map { item: JsonValue -> d1(item) }.toTypedArray()
                    val validation = Validation(*results)
                    if (!validation.hasFailure) {
                        Result.of { results.map { result -> result.get() }.toTypedArray() }
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected Array but got ${it.javaClass.simpleName}"))
            }
        }

        // TODO: pairsOf(left, right)?
        fun <A : Any> keyValuePairs(d1: Decoder<A>): Decoder<List<Pair<String, A>>> = Decoder {
            when {
                it.isObject -> {
                    val obj = it.asObject()
                    // TODO: Rewrite these so they short-circuit on failure instead of failing after parsing the whole list
                    val results = obj.toList().map { member ->
                        d1(member.value).map { Pair(member.name, it) }
                    }
                    val validation = Validation(*results.toTypedArray())
                    if (!validation.hasFailure) {
                        Result.of { results.map { result -> result.get() } }
                    } else {
                        Result.error(validation.failures.first())
                    }
                }
                else -> Result.error(Exception("Expected Object but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A : Any> mapOf(d1: Decoder<A>): Decoder<Map<String, A>> = keyValuePairs(d1).map { it.toMap() }

        fun <A : Any, B : Any> pairOf(left: Decoder<A>, right: Decoder<B>): Decoder<Pair<A, B>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    if (array.size() == 2) {
                        Result.all(left(array[0]), right(array[1])).map { vals ->
                            (vals[0] as A) to (vals[1] as B)
                        }
                    } else {
                        Result.error(Exception("Expected Pair but got array with ${array.size()} items"))
                    }
                }
                else -> Result.error(Exception("Expected Pair but got ${it.javaClass.simpleName}"))
            }
        }

        fun <A : Any, B : Any, C : Any> triple(d1: Decoder<A>, d2: Decoder<B>, d3: Decoder<C>): Decoder<Triple<A, B, C>> = Decoder {
            when {
                it.isArray -> {
                    val array = it.asArray()
                    if (array.size() == 3) {
                        Result.all(d1(array[0]), d2(array[1]), d3(array[2])).map { vals ->
                            Triple(vals[0] as A, vals[1] as B, vals[2] as C)
                        }
                    } else {
                        Result.error(Exception("Expected Triple but got array with ${array.size()} items"))
                    }
                }
                else -> Result.error(Exception("Expected Pair but got ${it.javaClass.simpleName}"))
            }
        }


        fun <A : Any> getIn(keys: List<String>, d1: Decoder<A>): Decoder<A> {
            return keys.foldRight(d1, { k, a -> get(k ,a) })
        }


        fun <A : Any> index(i: Int, d1: Decoder<A>): Decoder<A> {
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
        fun oneOf(vararg ds: Decoder<*>): Decoder<Any> = Decoder { value ->
            var result: Result<Any, Exception> = Result.of(Exception("None of the decoders matched"))
            var bail = false
            for (decoder in ds.iterator()) {
                decoder(value).map { decoded ->
                    result = Result.of { decoded }
                    // break - can't break here
                    bail = true
                }
                if (bail) { break }
            }
            result
        }

        // TODO: Generalize `object` function and allow chaining.
        fun <A : Any, Z : Any> object1(f: (A) -> Z, d1: Decoder<A>): Decoder<*> {
            return Decoder { value ->
                Result.all(d1(value)).map { vals ->
                    f(vals[0] as A)
                }
            }
        }

        fun <A : Any, B : Any, Z : Any> object2(f: (A, B) -> Z, d1: Decoder<A>, d2: Decoder<B>): Decoder<*> {
            return Decoder { value ->
                Result.all(d1(value), d2(value)).map { vals ->
                    f(vals[0] as A, vals[1] as B)
                }
            }
        }

        fun <T : Any> succeed(value: T): Decoder<T> = Decoder {
            Result.of { value }
        }

        fun <T : Any> fail(message: String): Decoder<T> = Decoder {
            Result.error(Exception(message))
        }
    }
}
