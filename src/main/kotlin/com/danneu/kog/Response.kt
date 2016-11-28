
package com.danneu.kog

import com.danneu.kog.cookies.Cookie
import com.danneu.kog.json.JsonValue
import java.io.File
import java.io.InputStream


typealias WebSocketAcceptor = (Request, WebSocket) -> Unit


internal fun Response.Companion.websocket(key: String, accept: WebSocketAcceptor): Response {
    return Response.switchingProtocols().apply {
        this.webSocket = key to accept
    }
}

class Response(var status: Status = Status.Ok, var body: ResponseBody = ResponseBody.None, var webSocket: Pair<String, WebSocketAcceptor>? = null) : HasHeaders<Response> {

    val cookies by lazy { mutableMapOf<String, Cookie>() }

    override var headers: MutableList<HeaderPair> = mutableListOf()

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
        return setHeader(Header.ContentType, "text/html")
            .setBody(ResponseBody.String(html))
    }

    fun text(text: String): Response {
        return setHeader(Header.ContentType, "text/plain")
            .setBody(ResponseBody.String(text))
    }

    fun none(): Response {
        return removeHeader(Header.ContentType)
            .setBody(ResponseBody.None)
    }

    fun json(value: JsonValue): Response {
        return setHeader(Header.ContentType, "application/json")
            .setBody(ResponseBody.String(value.toString()))
    }

    fun stream(input: InputStream, contentType: String = "application/octet-stream"): Response {
        return setHeader(Header.ContentType, contentType)
            .setBody(ResponseBody.InputStream(input))
    }

    fun file(file: File, contentType: String? = null): Response {
        return setBody(ResponseBody.File(file))
            // Hmm, already doing it at finalize time. TODO: Rethink streamable interface. need length?
            .setHeader(Header.ContentLength, file.length().toString())
            .setHeader(Header.ContentType, contentType ?: Mime.fromExtension(file.extension))
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
            if (status in setOf(Status.NotFound, Status.InternalError)) {
                text(status.toString())
            }
        }

        when (body.length) {
            null -> setHeader(Header.TransferEncoding, "Chunked")
            else -> setHeader(Header.ContentLength, body.length.toString())
        }

        return this
    }

    // REDIRECT

    // 301: .MovedPermanently
    // 302: .Found
    fun redirect(uri: String, permanent: Boolean = false): Response {
        return setHeader(Header.Location, uri)
            .setStatus(if (permanent) {
                Status.MovedPermanently
            } else {
                Status.Found
            })
    }

    fun redirectBack(request: Request, altUri: String): Response {
        return redirect(request.getHeader(Header.Referer) ?: altUri)
    }

    // MISC

    override fun toString(): String {
        return "Response (status=${status.code}, headers=$headers, body=$body)"
    }

    companion object {
        // Sugar for common status codes
        fun ok() = Response(Status.Ok)
        fun noContent() = Response(Status.NoContent)
        fun notModified() = Response(Status.NotModified)
        fun badRequest() = Response(Status.BadRequest)
        fun unauthorized() = Response(Status.Unauthorized)
        fun forbidden() = Response(Status.Forbidden)
        fun notFound() = Response(Status.NotFound)
        fun gone() = Response(Status.Gone)
        fun internalError() = Response(Status.InternalError)
        fun notImplemented() = Response(Status.NotImplemented)
        fun serviceUnavailble() = Response(Status.ServiceUnavailable)
        fun gatewayTimeout() = Response(Status.GatewayTimeout)
        fun switchingProtocols() = Response(Status.SwitchingProtocols)

        // The two most common redirects could use some sugar
        // since we tend to remember their status rather
        // than their names

        // 301 permanent
        fun movedPermanently() = Response(Status.MovedPermanently)
        fun redirect301() = Response(Status.MovedPermanently)

        // 302 temporary redirect
        fun found() = Response(Status.Found)
        fun redirect302() = Response(Status.Found)
    }
}