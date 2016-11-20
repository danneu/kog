
package com.danneu.kog

import com.danneu.kog.json.JsonValue
import java.io.File
import java.io.InputStream


typealias WebSocketAcceptor = (Request, WebSocket) -> Unit


fun Response.Companion.websocket(key: String): Response {
    return Response(Status.switchingProtocols).apply { this.webSocketKey = key }
}

class Response(var status: Status = Status.ok, var body: ResponseBody = ResponseBody.None, var webSocketKey: String? = null) : HasHeaders<Response> {

    override var headers: MutableList<Pair<String, String>> = mutableListOf()

    fun setStatus(status: Status): Response {
        this.status = status
        return this
    }

    // BODIES

    fun setBody(body: ResponseBody): Response {
        this.body = body
        return this
    }

    fun html(html: String): Response {
        return setHeader("Content-Type", "text/html")
            .setBody(ResponseBody.String(html))
    }

    fun text(text: String): Response {
        return setHeader("Content-Type", "text/plain")
            .setBody(ResponseBody.String(text))
    }

    fun none(): Response {
        return removeHeader("Content-Type")
            .setBody(ResponseBody.None)
    }

    fun json(value: JsonValue): Response {
        return setHeader("Content-Type", "application/json")
            .setBody(ResponseBody.String(value.toString()))
    }

    fun stream(input: InputStream, contentType: String = "application/octet-stream"): Response {
        return setHeader("Content-Type", contentType)
            .setBody(ResponseBody.InputStream(input))
    }

    fun file(file: File, contentType: String? = null): Response {
        return setBody(ResponseBody.File(file))
            // Hmm, already doing it at finalize time. TODO: Rethink streamable interface. need length?
            .setHeader("Content-Length", file.length().toString())
            .setHeader("Content-Type", contentType ?: Mime.fromExtension(file.extension))
    }

    // FINALIZE

    // This method ties up loose ends.
    // Call this right before flushing the response.
    fun finalize(): Response {
        if (status.empty) {
            this.none()
        }

        // TODO: Only set this if response type is text/*
        if (body is ResponseBody.None) {
            if (status in setOf(Status.notFound, Status.internalError)) {
                text(status.phrase)
            }
        }

        when (body.length) {
            null -> setHeader("Transfer-Encoding", "chunked")
            else -> setHeader("Content-Length", body.length.toString())
        }

        return this
    }

    // REDIRECT

    // 301: .movedPermanently
    // 302: .found
    fun redirect(uri: String, permanent: Boolean = false): Response {
        return setHeader("Location", uri)
            .setStatus(if (permanent) {
                Status.movedPermanently
            } else {
                Status.found
            })
    }

    fun redirectBack(request: Request, altUri: String): Response {
        return redirect(getHeader("Referer") ?: altUri)
    }

    // MISC

    override fun toString(): String {
        return "Response (status=${status.code}, headers=$headers, body=$body)"
    }

    companion object
}