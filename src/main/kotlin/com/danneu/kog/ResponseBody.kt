package com.danneu.kog

import java.security.MessageDigest
import java.util.Base64
import javax.servlet.ServletOutputStream


// etag() is null if there's no sensible way to generate a tag from an entity
interface ETaggable {
    fun etag(): String?

    companion object {
        fun empty(): String = "\"0-1B2M2Y8AsgTpgAmY7PhCfg\""
    }
}

interface OutStreamable {
    fun pipe(output: ServletOutputStream): Any
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
            val hash64 = body.md5().base64(padding = false).utf8()
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


