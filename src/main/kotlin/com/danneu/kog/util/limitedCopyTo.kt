package com.danneu.kog.util

import com.danneu.kog.ByteLength
import java.io.InputStream
import java.io.OutputStream

class CopyLimitExceeded : Exception()

/** Copies InputStream to OutputStream, but bails with CopyLimitExceeded if input stream is longer than `limit` bytes.
 *
 * Based on Kotlin's built-in extension function `InputStream.copyTo`.
 */
fun InputStream.limitedCopyTo(limit: ByteLength, out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes

        if (bytesCopied > limit.byteLength) {
            throw CopyLimitExceeded()
        }

        bytes = read(buffer)
    }
    return bytesCopied
}



