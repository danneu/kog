package com.danneu.kog

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request as JettyServerRequest
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler


interface WebSocketHandler {
    fun onOpen() {}
    fun onClose(statusCode: Int, reason: String?) {}
    fun onError(cause: Throwable) {}
    fun onText(message: String) {}
    fun onBinary(bytes: ByteArray, offset: Int, length: Int) {}

    companion object {
        fun adapter(request: Request, accept: WebSocketAcceptor): WebSocketAdapter {
            return object : WebSocketAdapter() {
                var handler: WebSocketHandler? = null

                override fun onWebSocketConnect(session: Session) {
                    super.onWebSocketConnect(session)
                    this.handler = accept(request, session).apply { onOpen() }
                }

                // TODO: Find a better way to guarantee socket.session non-nullable downstream instead of with `!!`

                override fun onWebSocketError(cause: Throwable) {
                    super.onWebSocketError(cause)
                    this.handler!!.onError(cause)
                }

                override fun onWebSocketClose(statusCode: Int, reason: String?) {
                    super.onWebSocketClose(statusCode, reason)
                    this.handler!!.onClose(statusCode, reason)
                }

                override fun onWebSocketText(message: String) {
                    super.onWebSocketText(message)
                    this.handler!!.onText(message)
                }

                override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
                    super.onWebSocketBinary(payload, offset, len)
                    this.handler!!.onBinary(payload, offset, len)
                }
            }
        }

        fun handler(accept: WebSocketAcceptor): JettyWebSocketHandler {
            var request: Request? = null
            return object : JettyWebSocketHandler() {
                override fun configure(factory: WebSocketServletFactory) {
                    // TODO: Allow idletimeout config. factory.policy.idleTimeout = idleTimeout
                    factory.creator = WebSocketCreator { _, _ -> adapter(request!!, accept) }
                }
                override fun handle(target: String, baseReq: JettyServerRequest, req: HttpServletRequest, res: HttpServletResponse) {
                    val factory = this.webSocketFactory

                    // HACK: Saving into upstream variable so configure() can read it
                    request = req.getAttribute("kog-request") as Request

                    if (factory.acceptWebSocket(req, res)) {
                        baseReq.isHandled = true
                    } else {
                        if (res.isCommitted) { baseReq.isHandled = true }
                    }
                }
            }
        }
    }
}

