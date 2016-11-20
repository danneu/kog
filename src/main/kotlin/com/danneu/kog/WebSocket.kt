package com.danneu.kog

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler

open abstract class WebSocket() {
    abstract fun onConnect(session: Session)
    // These are optional
    open fun onClose(statusCode: Int, reason: String?) { /* noop */ }
    open fun onError(cause: Throwable) { /* noop */ }
    open fun onText(message: String) { /* noop */ }
    open fun onBinary(payload: ByteArray, offset: Int, len: Int) { /* noop */ }

    companion object
}

fun WebSocket.Companion.adapter(socket: WebSocket): WebSocketAdapter {
    return object : WebSocketAdapter() {
        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
            socket.onConnect(session)
        }
        override fun onWebSocketError(cause: Throwable) {
            super.onWebSocketError(cause)
            socket.onError(cause)
        }
        override fun onWebSocketClose(statusCode: Int, reason: String?) {
            super.onWebSocketClose(statusCode, reason)
            socket.onClose(statusCode, reason)
        }
        override fun onWebSocketText(message: String) {
            super.onWebSocketText(message)
            socket.onText(message)
        }
        override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
            super.onWebSocketBinary(payload, offset, len)
            socket.onBinary(payload, offset, len)
        }
    }
}

fun WebSocket.Companion.handler(newSocket: () -> WebSocket): org.eclipse.jetty.websocket.server.WebSocketHandler {
    return object : org.eclipse.jetty.websocket.server.WebSocketHandler() {
        override fun configure(factory: WebSocketServletFactory) {
            // TODO: Allow idletimeout config. factory.policy.idleTimeout = idleTimeout
            factory.creator = WebSocketCreator { req, res ->
                val socket = newSocket()
                WebSocket.adapter(socket)
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
