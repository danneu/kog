package com.danneu.kog

import java.util.Base64
import javax.servlet.ServletOutputStream


/**
 * A response body is ETaggable if it knows how to generate its own etag.
 */
interface ETaggable {
    /**
     * Returns null if there's no sensible way to generate a tag from an entity.
     */
    fun etag(): String?

    companion object {
        /**
         * The ETag for 0-length empty body.
         */
        fun empty(): String = "\"0-1B2M2Y8AsgTpgAmY7PhCfg\""
    }
}


/**
 * An object can be streamed to the response output stream if it knows how to pipe itself.
 */
interface OutStreamable {
    fun pipe(output: ServletOutputStream): Any

    /**
     * Byte length is not always known ahead of time, like when response body is an input stream.
     */
    val length: Long?
}


sealed class ResponseBody : OutStreamable, ETaggable {
    object None : ResponseBody() {
        override val length: Long = 0
        override fun pipe(output: ServletOutputStream) = output.close()
        override fun etag(): kotlin.String = ETaggable.empty()
    }
    class String(val body: kotlin.String) : ResponseBody() {
        override val length: Long = body.length.toLong()
        override fun pipe(output: ServletOutputStream) = body.byteInputStream().copyTo(output)
        override fun toString(): kotlin.String = body.toString()
        override fun etag(): kotlin.String = ByteArray(body.toByteArray()).etag()
    }
    class ByteArray(val body: kotlin.ByteArray) : ResponseBody() {
        override val length: Long = body.size.toLong()
        override fun pipe(output: ServletOutputStream) = body.inputStream().copyTo(output)
        override fun etag(): kotlin.String {
            if (body.isEmpty()) return ETaggable.empty()
            val hash64 = Base64.getEncoder().withoutPadding().encode(body.md5()).utf8()
            return "\"${body.size.toHexString()}-$hash64\""
        }
    }
    class File(val body: java.io.File) : ResponseBody() {
        override val length: Long = body.length().toLong()
        override fun pipe(output: ServletOutputStream) = body.inputStream().copyTo(output)
        override fun etag(): kotlin.String {
            return "\"${body.length().toHexString()}-${body.lastModified().toHexString()}\""
        }
    }
    class InputStream(val body: java.io.InputStream) : ResponseBody() {
        override val length: Long? = null
        override fun pipe(output: ServletOutputStream) = body.copyTo(output)
        override fun etag(): kotlin.String? = null
    }
}


