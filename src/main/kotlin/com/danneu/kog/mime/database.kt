package com.danneu.kog.mime

import com.danneu.kog.json.Decoder
import com.danneu.kog.mime.MimeDatabase.MimeRecord
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import org.funktionale.option.getOrElse
import java.io.File
import java.io.Reader

/** Wraps the mime-db data to expose convenient lookup functions.
 */
class MimeDatabase(underlying: Map<String, MimeRecord>) {
    // mapping of extensions ("gif") to mime types
    private val extLookup = underlying.flatMap { (mime, record) ->
        record.extensions.map { ext -> ext to mime } }.toMap()

    private val compressibleLookup: Set<String> =
        underlying.filterValues(MimeRecord::compressible).keys.toSet()

    fun compressible(key: String) = compressibleLookup.contains(key)

    fun fromExtension(ext: String): String? = extLookup[ext]

    class MimeRecord(val extensions: List<String> = emptyList(), val compressible: Boolean)
}


val database: MimeDatabase = run {
    val file = File("./node_modules/mime-db/db.json")
    println("Reading db.json from ${file.absolutePath}")

    if (!file.exists()) {
        System.err.println("Could not find db.json at expected path ${file.absolutePath}")
    }

    parseDatabase(file.reader())
        .fold({ it }, { emptyMap<String, MimeRecord>() })
        .let(::MimeDatabase)
}

private fun parseDatabase(reader: Reader): Result<Map<String, MimeRecord>, Exception> {
    val extensions: Decoder<List<String>> = Decoder.oneOf(
        Decoder.get("extensions", Decoder.nullable(Decoder.listOf(Decoder.string)).map { option ->
            option.getOrElse { emptyList() }
        }),
        Decoder.succeed(listOf<String>())
    )

    val compressible: Decoder<Boolean> = Decoder.oneOf(
        Decoder.get("compressible", Decoder.nullable(Decoder.bool).map { option ->
            option.getOrElse { false }
        }),
        Decoder.succeed(false)
    )

    val decoder = Decoder.mapOf(Decoder.object2(::MimeRecord, extensions, compressible))

    return Decoder.tryParse(reader).flatMap { decoder(it) }
}
