package com.danneu.kog.batteries

import com.danneu.kog.ByteLength
import com.danneu.kog.Header
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.ResponseBody
import com.danneu.kog.Status
import com.danneu.kog.mime.database
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.RegexOption.IGNORE_CASE


// TODO: Consider a Vary tool for appending it instead of assoc'ing it

// https://www.fastly.com/blog/best-practices-for-using-the-vary-header


fun compress(
    threshold: ByteLength = ByteLength.ofBytes(1024),
    predicate: (String?) -> Boolean = ::isCompressible
): Middleware = { handler ->
    fun(request: Request): Response {
        val response = handler(request)

        // Vary should appear in the response for this request even if we short-circuit
        response.setHeader(Header.Vary, "Accept-Encoding")

        // SHORT CIRCUITS

        if (request.method == Method.Head) return response
        // Body is empty
        if (response.body is ResponseBody.None) return response
        if (response.status.empty) return response
        // Body already encoded
        if (response.getHeader(Header.ContentEncoding) != null) return response
        // Body length is not-null and it doesn't meet threshold
        if (response.body.length?.let { it < threshold.byteLength} ?: false) return response
        // Ensure it's a Content-Type we want to compress
        if (!predicate(response.getHeader(Header.ContentType))) return response

        // ACCEPT

        val encoding = request.negotiate.acceptableEncoding(listOf("gzip", "identity"))
            ?: return Response(Status.NotAcceptable).text("supported encodings: gzip, identity")
        if (encoding == "identity") return response

        // COMPRESS

        return response.apply {
            setHeader(Header.ContentEncoding, "gzip")
            removeHeader(Header.ContentLength)
            body = ResponseBody.InputStream(compressBody(response.body))
        }
    }
}

private val compressibleTypeRegex = Regex("""^text/|\+json$|\+text$|\+xml$""", IGNORE_CASE)
private val extractTypeRegex = Regex("""^\s*([^;\s]*)(?:;|\s|$)""")

fun isCompressible(header: String?): Boolean {
    if (header == null) return false

    // strip params
    val mime = extractTypeRegex.find(header)?.groupValues?.get(1)?.toLowerCase() ?: return false

    // check against mime-db
    if (database.compressible(mime)) return true

    // fallback to regex
    return compressibleTypeRegex.containsMatchIn(mime)
}

// TODO: Don't wait on the compression.
private fun compressBody(body: ResponseBody): InputStream {
    val pipeIn = PipedInputStream()
    val pipeOut = PipedOutputStream(pipeIn)
    GZIPOutputStream(pipeOut).use { gzipOut ->
        body.pipe(gzipOut)
    }
    return pipeIn
}


