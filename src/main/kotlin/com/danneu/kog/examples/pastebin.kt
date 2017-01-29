package com.danneu.kog.examples


import com.danneu.kog.ByteLength
import com.danneu.kog.Handler
import com.danneu.kog.Response
import com.danneu.kog.SafeRouter
import com.danneu.kog.Server
import com.danneu.kog.util.CopyLimitExceeded
import com.danneu.kog.util.limitedCopyTo
import java.io.File
import java.util.UUID


val uploadLimit = ByteLength.ofMegabytes(5)

val router = SafeRouter {

    // Display API usage

    get("/",  fun(): Handler = {
        Response().html("""
            <pre>
            Pastebin API

            [POST /]: Upload file
                * Body must be under ${java.text.DecimalFormat("#,###").format(uploadLimit.byteLength)} bytes

            [GET /]: Fetch file
            </pre>
        """.trimIndent())
    })

    // Upload file

    post("/", fun(): Handler = handler@ { req ->
        // Generate random ID for user's upload
        val id = UUID.randomUUID()

        // Ensure "pastes" directory is created
        val destFile = File(File("pastes").apply { mkdir() }, id.toString())

        // Move user's upload into "pastes", bailing if their upload size is too large.
        try {
            req.body.limitedCopyTo(uploadLimit, destFile.outputStream())
        } catch(e: CopyLimitExceeded) {
            destFile.delete()
            return@handler Response.badRequest().text("Cannot upload more than ${uploadLimit.byteLength} bytes")
        }

        // If stream was empty, delete the file and scold user
        if (destFile.length() == 0L) {
            destFile.delete()
            return@handler Response.badRequest().text("Paste file required")
        }

        // Posterity
        println("A client uploaded ${destFile.length()} bytes to ${destFile.absolutePath}")

        // Tell user where they can find their uploaded file
        Response().jsonObject("url" to "http://localhost:${req.serverPort}/$id")
    })

    // Fetch file

    get("/<id>", fun(id: UUID): Handler = handler@ { _ ->
        val file = File("pastes/$id")
        if (!file.exists()) return@handler Response.notFound()
        Response().file(file)
    })
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(3000)
}


