package com.danneu.kog

import java.io.OutputStream
import java.io.StringWriter
import java.util.Base64

// TODO: Should ResponseBody also hold the content-type?

/**
 * A response body is ETaggable if it knows how to generate its own etag.
 */
interface ETaggable {
    /**
     * Returns null if there's no sensible way to generate a tag from an entity.
     */
    val etag: kotlin.String?

    companion object {
        /**
         * The ETag for 0-length empty body.
         */
        fun empty() = "\"0-1B2M2Y8AsgTpgAmY7PhCfg\""
    }
}


sealed class ResponseBody : ETaggable {
    /** Bodies can be streamed
     */
    abstract fun inputStream(): java.io.InputStream

    /** Bodies can be piped into ServletOutputStream and other streams
     */
    abstract fun pipe(output: OutputStream): OutputStream

    /**
     * Byte length is not always known ahead of time, like when response body is an input stream.
     */
    abstract val length: Long?

    object None : ResponseBody() {
        override val length: Long = 0
        override fun pipe(output: OutputStream) = output.apply { close() }
        override fun inputStream() = "".byteInputStream()
        override val etag = ETaggable.empty()
    }
    class String(val body: kotlin.String) : ResponseBody() {
        override val length: Long = body.length.toLong()
        override fun pipe(output: OutputStream) = body.byteInputStream().copyTo(output).let { output }
        override fun inputStream() = body.byteInputStream()
        override fun toString(): kotlin.String = body
        override val etag = ByteArray(body.toByteArray()).etag
        override fun hashCode() = body.hashCode()
        override fun equals(other: Any?) = when (other) {
            is ResponseBody.String -> body == other.body
            else -> false
        }
    }
    class ByteArray(val body: kotlin.ByteArray) : ResponseBody() {
        override val length: Long = body.size.toLong()
        override fun pipe(output: OutputStream) = body.inputStream().copyTo(output).let { output }
        override fun inputStream() = body.inputStream()
        override val etag: kotlin.String = run {
            if (body.isEmpty()) return@run ETaggable.empty()
            val hash64 = Base64.getEncoder().withoutPadding().encode(body.md5()).utf8()
            "\"${body.size.toHexString()}-$hash64\""
        }
    }
    class File(val body: java.io.File) : ResponseBody() {
        override val length: Long = body.length()
        override fun pipe(output: OutputStream) = body.inputStream().copyTo(output).let { output }
        override fun inputStream() = body.inputStream()
        override val etag = "\"${body.length().toHexString()}-${body.lastModified().toHexString()}\""
    }
    class InputStream(val body: java.io.InputStream) : ResponseBody() {
        override val length: Long? = null
        override fun pipe(output: OutputStream) = body.copyTo(output).let { output }
        override fun inputStream() = body
        override val etag = null
    }
    // ResponseBody.Writer represents a function that takes a writer in order to lazily write the
    // response at some point in the future.
    class Writer(val writeTo: (java.io.Writer) -> Unit) : ResponseBody() {
        // This flag will be set to true if we had to consume the entire stream and buffer it in memory
        // so that we avoid producing the stream twice.
        var realized = false

        // If we have to call this, like when generating an etag, then the writer will be written to
        // and the response will be reified as an intermediate string in memory.
        val bytes: kotlin.ByteArray by lazy {
            realized = true
            StringWriter()
                .apply { writeTo(this) }
                .toString()
                .toByteArray()
        }
        override val length = if (realized) bytes.size.toLong() else null
        override fun pipe(output: OutputStream) = if (realized) {
            bytes.inputStream().copyTo(output).let { Unit }
        } else {
            output.writer().apply { writeTo(this) }.flush()
        }.let { output }
        override fun inputStream() = bytes.inputStream()
        override val etag by lazy { ByteArray(bytes).etag }
    }
}


