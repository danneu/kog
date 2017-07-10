package com.danneu.kog.mime

import com.danneu.json.Decoder
import com.danneu.kog.mime.MimeDatabase.MimeRecord
import com.danneu.result.Result
import com.danneu.result.flatMap
import com.danneu.result.getOrElse
import java.io.Reader

/** Wraps the mime-db data to expose convenient lookup functions.
 */
class MimeDatabase(underlying: Map<String, MimeRecord>) {
    // mapping of extensions ("gif") to mime types
    private val extLookup = underlying.flatMap { (mime, record) ->
        record.extensions.map { ext -> ext to mime }
    }.toMap()

    // set of all extensions that can be compressed
    private val compressibleLookup: Set<String> =
        underlying.filterValues(MimeRecord::compressible).keys.toSet()

    fun compressible(key: String) = compressibleLookup.contains(key)

    fun fromExtension(ext: String): String? = extLookup[ext]

    class MimeRecord(val extensions: List<String> = emptyList(), val compressible: Boolean)
}


val database: MimeDatabase = run {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("mime-db/db.json")

    if (stream == null) {
        System.err.println("Error initializing MimeDatabase: Could not find resource mime-db/db.json")
        System.exit(1)
    }

    parseDatabase(stream.bufferedReader())
        .getOrElse(emptyMap<String, MimeRecord>())
        .let(::MimeDatabase)
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

    val decoder = Decoder.mapOf(Decoder.map2(::MimeRecord, extensions, compressible))

    return Decoder.parse(reader).flatMap { decoder(it) }
}
