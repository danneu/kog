package com.danneu.kog

import com.danneu.kog.adapters.Servlet
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler
import org.eclipse.jetty.server.Request as JettyServerRequest


class WebSocket(val session: Session) {
    var onClose: (statusCode: Int, reason: String?) -> Unit = { a, b -> /* noop */ }
    var onError: (cause: Throwable) -> Unit = { /* noop */ }
    var onText: (message: String) -> Unit = { /* noop */ }
    var onBinary: (payload: ByteArray, offset: Int, len: Int) -> Unit = { a, b, c -> /* noop */ }

    companion object
}


fun WebSocket.Companion.adapter(onConnect: (WebSocket) -> Unit): WebSocketAdapter {
    return object : WebSocketAdapter() {
        var socket: WebSocket? = null

        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
            this.socket = WebSocket(session)
            onConnect(this.socket!!)
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

fun WebSocket.Companion.handler(config: WebSocketConfig): org.eclipse.jetty.websocket.server.WebSocketHandler? {
    val (validationHandler, onConnect) = config
    // HACK: request is set in handle() so that it's then accessible in configure()
    var request: Request? = null

    return object : org.eclipse.jetty.websocket.server.WebSocketHandler() {
        override fun configure(factory: WebSocketServletFactory) {
            // TODO: Allow idletimeout config. factory.policy.idleTimeout = idleTimeout
            factory.creator = WebSocketCreator { req: ServletUpgradeRequest, res: ServletUpgradeResponse ->
                WebSocket.adapter({ socket -> onConnect(request!!, socket) })
            }
        }
        override fun handle(target: String, baseReq: JettyServerRequest, req: HttpServletRequest, res: HttpServletResponse) {
            val factory = this.webSocketFactory
            // If it's not a websocket upgrade request, pass request to the next handler (the kog handler)
            if (!factory.isUpgradeRequest(req, res)) return super.handle(target, baseReq, req, res)
            // Check request against our wsrequesthandler to see if we should upgrade
            request = Servlet.intoKogRequest(req)
            val response = Server.middleware()(validationHandler)(request!!)
            if (response.status != Status.switchingProtocols) {
                return Servlet.updateServletResponse(res, response)
            }
            if (factory.acceptWebSocket(req, res)) {
                baseReq.isHandled = true
            } else {
                if (res.isCommitted) { baseReq.isHandled = true }
            }
        }
    }
}
