
package com.danneu.kog

import com.danneu.kog.cookies.Cookie
import com.danneu.json.JsonValue
import com.danneu.kog.mime.database
import org.eclipse.jetty.websocket.api.Session
import com.danneu.json.Encoder as JE
import java.io.File
import java.io.InputStream

/**
 * The acceptor is a function that takes the request and websocket session and returns a handler.
 * The session (websocket connection) is connected and ready by the time the acceptor is run.
 */
typealias WebSocketAcceptor = (Request, Session) -> WebSocketHandler

/**
 * HACK: If a response has the status 101 Switching Protocols, Kog registers the websocket handler with Jetty.
 */
fun Response.Companion.websocket(key: String, accept: WebSocketAcceptor): Response {
    return Response.switchingProtocols().apply {
        this.webSocket = key to accept
    }
}

class Response(
    var status: Status = Status.Ok,
    var body: ResponseBody = ResponseBody.None,
    var webSocket: Pair<String, WebSocketAcceptor>? = null
) : HasHeaders<Response>() {
    override fun toType() = this

    val cookies by lazy { mutableMapOf<String, Cookie>() }

    override var headers = mutableListOf<HeaderPair>()

    fun setStatus(status: Status) = apply { this.status = status }

    // BODIES

    fun html(html: String) = apply {
        setHeader(Header.ContentType, ContentType.Html.toString())
        body = ResponseBody.String(html)
    }

    fun text(text: String) = apply {
        setHeader(Header.ContentType, ContentType.Text.toString())
        body = ResponseBody.String(text)
    }

    fun bytes(bytes: ByteArray) = apply {
        body = ResponseBody.ByteArray(bytes)
    }

    fun none() = apply {
        removeHeader(Header.ContentType)
        body = ResponseBody.None
    }

    fun json(value: JsonValue) = apply {
        setHeader(Header.ContentType, ContentType.Json.toString())
        body = ResponseBody.String(value.toString())
    }

    fun stream(input: InputStream, contentType: String = ContentType.OctetStream.toString()) = apply {
        setHeader(Header.ContentType, contentType)
        body = ResponseBody.InputStream(input)
    }

    fun file(file: File, contentType: String? = null) = apply {
        body = ResponseBody.File(file)
        // Hmm, already doing it at finalize time. TODO: Rethink streamable interface. need length?
        setHeader(Header.ContentLength, file.length().toString())
        setHeader(Header.ContentType, contentType ?: database.fromExtension(file.extension) ?: ContentType.OctetStream.toString())
    }

    fun writer(contentType: String, writeTo: (java.io.Writer) -> Unit): Response = apply {
        setHeader(Header.ContentType, contentType)
        body = ResponseBody.Writer(writeTo)
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
            null -> setHeader(Header.TransferEncoding, "chunked")
            else -> setHeader(Header.ContentLength, body.length.toString())
        }

        return this
    }

    // REDIRECT

    // 301: .MovedPermanently
    // 302: .Found
    fun redirect(uri: String, permanent: Boolean = false) = apply {
        setHeader(Header.Location, uri)
        setStatus(if (permanent) Status.MovedPermanently else Status.Found)
    }

    fun redirectBack(request: Request, altUri: String) = redirect(request.getHeader(Header.Referer) ?: altUri)

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
