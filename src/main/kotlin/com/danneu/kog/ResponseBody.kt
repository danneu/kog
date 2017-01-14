package com.danneu.kog

import java.io.OutputStream
import java.util.Base64


/**
 * A response body is ETaggable if it knows how to generate its own etag.
 */
interface ETaggable {
    /**
     * Returns null if there's no sensible way to generate a tag from an entity.
     */
    fun etag(): kotlin.String?

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
    abstract fun pipe(output: OutputStream): Any

    /**
     * Byte length is not always known ahead of time, like when response body is an input stream.
     */
    abstract val length: Long?

    object None : ResponseBody() {
        override val length: Long = 0
        override fun pipe(output: OutputStream) = output.close()
        override fun inputStream() = "".byteInputStream()
        override fun etag(): kotlin.String = ETaggable.empty()
    }
    class String(val body: kotlin.String) : ResponseBody() {
        override val length: Long = body.length.toLong()
        override fun pipe(output: OutputStream) = body.byteInputStream().copyTo(output)
        override fun inputStream() = body.byteInputStream()
        override fun toString(): kotlin.String = body
        override fun etag(): kotlin.String = ByteArray(body.toByteArray()).etag()
        override fun hashCode() = body.hashCode()
        override fun equals(other: Any?) = when (other) {
            is ResponseBody.String -> body == other.body
            else -> false
        }
    }
    class ByteArray(val body: kotlin.ByteArray) : ResponseBody() {
        override val length: Long = body.size.toLong()
        override fun pipe(output: OutputStream) = body.inputStream().copyTo(output)
        override fun inputStream() = body.inputStream()
        override fun etag(): kotlin.String {
            if (body.isEmpty()) return ETaggable.empty()
            val hash64 = Base64.getEncoder().withoutPadding().encode(body.md5()).utf8()
            return "\"${body.size.toHexString()}-$hash64\""
        }
    }
    class File(val body: java.io.File) : ResponseBody() {
        override val length: Long = body.length()
        override fun pipe(output: OutputStream) = body.inputStream().copyTo(output)
        override fun inputStream() = body.inputStream()
        override fun etag(): kotlin.String {
            return "\"${body.length().toHexString()}-${body.lastModified().toHexString()}\""
        }
    }
    class InputStream(val body: java.io.InputStream) : ResponseBody() {
        override val length: Long? = null
        override fun pipe(output: OutputStream) = body.copyTo(output)
        override fun inputStream() = body
        override fun etag(): kotlin.String? = null
    }
}


