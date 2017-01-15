package com.danneu.kog.mime

import com.danneu.kog.json.Decoder
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.getOrElse
import org.funktionale.option.getOrElse
import java.io.File
import java.io.Reader


/** A look-up table of data mime-types that can be compressed by our gzip middleware.
 */
val compressibles: Set<String> = run {
    val file = File("./node_modules/mime-db/db.json")
    println("db.json exists: ${file.exists()}")

    if (!file.exists()) {
        System.err.println("Could not find db.json at expected path ${file.absolutePath}")
    }

    val compressibles = parseCompressibles(file.reader()).getOrElse(emptySet())

    println("Compressible mime-types found: ${compressibles.size}")

    compressibles
}

private fun parseCompressibles(reader: Reader): Result<Set<String>, Exception> {
    val compressible: Decoder<Boolean> = run {
        Decoder.get("compressible", Decoder.nullable(Decoder.bool).map { option ->
            option.getOrElse { false }
        })
    }

    val decoder: Decoder<Set<String>> = Decoder.mapOf(
        Decoder.oneOf(compressible, Decoder.succeed(false)) as Decoder<Boolean>
    ).map { map -> map.filterValues { it }.keys }

    return Decoder.tryParse(reader).flatMap { json -> decoder(json) }
}


