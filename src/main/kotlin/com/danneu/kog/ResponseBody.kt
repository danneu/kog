package com.danneu.kog

import javax.servlet.ServletOutputStream

interface OutStreamable {
    fun pipe(output: ServletOutputStream): Any
    val length: Long?
}

sealed class ResponseBody : OutStreamable {
    object None : ResponseBody() {
        override val length: Long = 0
        override fun pipe(output: ServletOutputStream) = output.close()
    }
    class String(val body: kotlin.String) : ResponseBody() {
        override val length: Long = body.length.toLong()
        override fun pipe(output: ServletOutputStream) = body.byteInputStream().copyTo(output)
        override fun toString(): kotlin.String = body.toString()
    }
    class ByteArray(val body: kotlin.ByteArray) : ResponseBody() {
        override val length: Long = body.size.toLong()
        override fun pipe(output: ServletOutputStream) = body.inputStream().copyTo(output)
    }
    class InputStream(val body: java.io.InputStream) : ResponseBody() {
        override val length: Long? = null
        override fun pipe(output: ServletOutputStream) = body.copyTo(output)
    }
    class File(val body: java.io.File) : ResponseBody() {
        override val length: Long = body.length().toLong()
        override fun pipe(output: ServletOutputStream) = body.inputStream().copyTo(output)
    }
}


