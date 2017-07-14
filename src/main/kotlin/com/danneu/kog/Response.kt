
package com.danneu.kog

import com.danneu.kog.cookies.Cookie
import com.danneu.json.JsonValue
import com.danneu.kog.mime.database
import org.eclipse.jetty.websocket.api.Session
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
    var webSocket: Pair<String, WebSocketAcceptor>? = null,
    override var contentType: ContentType? = null
) : HasHeaders<Response>(), HasContentType {
    override fun toType() = this

    val cookies by lazy { mutableMapOf<String, Cookie>() }

    override var headers = mutableListOf<HeaderPair>()

    // BODIES

    fun html(html: String) = apply {
        contentType = ContentType(Mime.Html)
        body = ResponseBody.String(html)
    }

    fun text(text: String) = apply {
        contentType = ContentType(Mime.Text)
        body = ResponseBody.String(text)
    }

    fun bytes(bytes: ByteArray, mime: Mime) = apply {
        contentType = ContentType(mime)
        body = ResponseBody.ByteArray(bytes)
    }

    fun none() = apply {
        removeHeader(Header.ContentType)
        contentType = null
        body = ResponseBody.None
    }

    fun json(value: JsonValue) = apply {
        contentType = ContentType(Mime.Json)
        body = ResponseBody.String(value.toString())
    }

    fun stream(input: InputStream, mime: Mime = Mime.OctetStream) = apply {
        contentType = ContentType(mime)
        body = ResponseBody.InputStream(input)
    }

    fun file(file: File, mime: Mime? = null) = apply {
        body = ResponseBody.File(file)
        // Hmm, already doing it at finalize time. TODO: Rethink streamable interface. need length?
        setHeader(Header.ContentLength, file.length().toString())
        contentType = ContentType(mime
            ?: database.fromExtension(file.extension)
            ?: Mime.OctetStream
        )
    }

    fun writer(mime: Mime, writeTo: (java.io.Writer) -> Unit): Response = apply {
        contentType = ContentType(mime)
        body = ResponseBody.Writer(writeTo)
    }

    // FINALIZE

    // This method ties up loose ends.
    // Call this right before flushing the response.
    internal fun finalize(): Response {
        if (status.empty) {
            this.none()
        }

        if (body is ResponseBody.None && contentType?.mime?.prefix == "text") {
            if (status in setOf(Status.NotFound, Status.InternalError)) {
                text(status.toString())
            }
        }

        when (body.length) {
            null -> setHeader(Header.TransferEncoding, "chunked")
            else -> setHeader(Header.ContentLength, body.length.toString())
        }

        // If contentType is set, then content-type header would be redundant and more likely wrong
        if (contentType != null) {
            removeHeader(Header.ContentType)
        }

        return this
    }

    // REDIRECT

    // 301: .MovedPermanently
    // 302: .Found
    fun redirect(uri: String, permanent: Boolean = false) = apply {
        setHeader(Header.Location, uri)
        status = if (permanent) Status.MovedPermanently else Status.Found
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
