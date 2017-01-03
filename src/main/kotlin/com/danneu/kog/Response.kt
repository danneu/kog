
package com.danneu.kog

import com.danneu.kog.cookies.Cookie
import com.danneu.kog.json.JsonValue
import com.danneu.kog.json.Encoder as JE
import java.io.File
import java.io.InputStream


typealias WebSocketAcceptor = (Request, WebSocket) -> Unit


/**
 * HACK: If a response has the status 101 Switching Protocols, Kog registers the websocket handler with Jetty.
 */
internal fun Response.Companion.websocket(key: String, accept: WebSocketAcceptor): Response {
    return Response.switchingProtocols().apply {
        this.webSocket = key to accept
    }
}

class Response(
    var status: Status = Status.Ok,
    var body: ResponseBody = ResponseBody.None,
    var webSocket: Pair<String, WebSocketAcceptor>? = null
) : HasHeaders<Response> {

    val cookies by lazy { mutableMapOf<String, Cookie>() }

    override var headers: MutableList<HeaderPair> = mutableListOf()

    fun setStatus(status: Status) = apply { this.status = status }

    // BODIES

    fun setBody(body: ResponseBody) = apply { this.body = body }

    fun html(html: String) = apply {
        setHeader(Header.ContentType, "text/html")
        setBody(ResponseBody.String(html))
    }

    fun text(text: String) = apply {
        setHeader(Header.ContentType, "text/plain")
        setBody(ResponseBody.String(text))
    }

    fun none() = apply {
        removeHeader(Header.ContentType)
        setBody(ResponseBody.None)
    }

    fun json(value: JsonValue) = apply {
        setHeader(Header.ContentType, "application/json")
        setBody(ResponseBody.String(value.toString()))
    }

    fun jsonObject(vararg pairs: Pair<String, *>) = json(JE.jsonObject(*pairs))
    fun jsonObject(pairs: Iterable<Pair<String, *>>) = json(JE.jsonObject(pairs))
    fun jsonObject(pairs: Sequence<Pair<String, *>>) = json(JE.jsonObject(pairs))

    fun jsonArray(vararg values: Any) = json(JE.jsonArray(values))
    fun jsonArray(values: Iterable<*>) = json(JE.jsonArray(values))
    fun jsonArray(values: Sequence<*>) = json(JE.jsonArray(values))

    fun stream(input: InputStream, contentType: String = "application/octet-stream") = apply {
        setHeader(Header.ContentType, contentType)
        setBody(ResponseBody.InputStream(input))
    }

    fun file(file: File, contentType: String? = null) = apply {
        setBody(ResponseBody.File(file))
        // Hmm, already doing it at finalize time. TODO: Rethink streamable interface. need length?
        setHeader(Header.ContentLength, file.length().toString())
        setHeader(Header.ContentType, contentType ?: Mime.fromExtension(file.extension))
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