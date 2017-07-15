package com.danneu.kog.mime

import com.danneu.json.Decoder
import com.danneu.kog.Mime
import com.danneu.kog.util.Util
import com.danneu.result.Result
import com.danneu.result.getOrElse
import java.io.Reader
import java.nio.charset.Charset

class MimeRecord(
    val extensions: List<String> = emptyList(),
    val compressible: Boolean,
    val charset: Charset?
)

/** Wraps the mime-db data to expose convenient lookup functions.
 */
class MimeDatabase(val underlying: Map<String, MimeRecord>) {
    // mapping of extensions ("gif") to mime types
    private val extLookup = underlying.flatMap { (mime, record) ->
        record.extensions.map { ext ->
            ext to Mime.fromString(mime)
        }
    }.toMap()

    fun compressible(mime: Mime) = underlying[mime.toString()]?.compressible ?: false

    // Remove leading dot if there is one
    fun fromExtension(ext: String): Mime? = ext.trim().let {
        if (it.startsWith('.')) {
            extLookup[it.drop(1)]
        } else {
            extLookup[it]
        }
    }

    // text/* is utf-8 by default
    fun getCharset(mime: Mime): Charset? = if (mime.prefix == "text") {
        Charsets.UTF_8
    } else {
        underlying[mime.toString()]?.charset
    }

    fun getExtensions(mime: Mime): List<String> = underlying[mime.toString()]?.extensions ?: emptyList()
}


val database: MimeDatabase = run {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("mime-db/db.json")

    if (stream == null) {
        System.err.println("Error initializing MimeDatabase: Could not find resource mime-db/db.json")
        return@run MimeDatabase(emptyMap())
    }

    val start = System.currentTimeMillis()
    parseDatabase(stream.bufferedReader())
        .getOrElse(emptyMap())
        .let(::MimeDatabase)
        .apply {
            println("mime-db loaded in ${System.currentTimeMillis() - start}ms")
        }
}

private fun parseDatabase(reader: Reader): Result<Map<String, MimeRecord>, String> {
    val extensions: Decoder<List<String>> = Decoder.oneOf(
        Decoder.get("extensions", Decoder.nullable(Decoder.listOf(Decoder.string)).map { it ?: emptyList() }),
        Decoder.succeed(emptyList())
    )

    val compressible: Decoder<Boolean> = Decoder.oneOf(
        Decoder.get("compressible", Decoder.nullable(Decoder.bool).map { it ?: false }),
        Decoder.succeed(false)
    )

    val charset: Decoder<Charset?> = Decoder.getOrMissing("charset", null, Decoder.string.map { Util.charsetOrNull(it) })

    val decoder = Decoder.mapOf(Decoder.map(::MimeRecord, extensions, compressible, charset))

    return Decoder.decode(reader, decoder)
}

