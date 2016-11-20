package com.danneu.kog

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler


class WebSocket(val session: Session) {
    var onClose: (statusCode: Int, reason: String?) -> Unit = { a, b -> /* noop */ }
    var onError: (cause: Throwable) -> Unit = { /* noop */ }
    var onText: (message: String) -> Unit = { /* noop */ }
    var onBinary: (payload: ByteArray, offset: Int, len: Int) -> Unit = { a, b, c -> /* noop */ }

    companion object
}


fun WebSocket.Companion.adapter(handleSocket: (WebSocket) -> Unit): WebSocketAdapter {
    return object : WebSocketAdapter() {
        var socket: WebSocket? = null

        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
            this.socket = WebSocket(session)
            handleSocket(this.socket!!)
        }

        // TODO: Find a better way to guarantee socket.session non-nullable downstream instead of with `!!`

        override fun onWebSocketError(cause: Throwable) {
            super.onWebSocketError(cause)
            this.socket!!.onError(cause)
        }

        override fun onWebSocketClose(statusCode: Int, reason: String?) {
            super.onWebSocketClose(statusCode, reason)
            this.socket!!.onClose(statusCode, reason)
        }

        override fun onWebSocketText(message: String) {
            super.onWebSocketText(message)
            this.socket!!.onText(message)
        }

        override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
            super.onWebSocketBinary(payload, offset, len)
            this.socket!!.onBinary(payload, offset, len)
        }
    }
}

fun WebSocket.Companion.handler(handleSocket: (socket: WebSocket) -> Unit): org.eclipse.jetty.websocket.server.WebSocketHandler {
    return object : org.eclipse.jetty.websocket.server.WebSocketHandler() {
        override fun configure(factory: WebSocketServletFactory) {
            // TODO: Allow idletimeout config. factory.policy.idleTimeout = idleTimeout
            factory.creator = WebSocketCreator { req, res ->
                WebSocket.adapter(handleSocket)
            }
        }
        override fun handle(target: String, baseReq: Request, req: HttpServletRequest, res: HttpServletResponse) {
            val factory = this.webSocketFactory
            // If it's not a websocket upgrade request, pass request to the next handler (the kog handler)
            if (!factory.isUpgradeRequest(req, res)) return super.handle(target, baseReq, req, res)
            if (factory.acceptWebSocket(req, res)) {
                baseReq.isHandled = true
            } else {
                if (res.isCommitted) { baseReq.isHandled = true }
            }
        }
    }
}
