
package com.danneu.kog

import com.danneu.kog.json.Encoder as JE
import com.danneu.kog.json.Decoder as JD
import org.eclipse.jetty.server.Handler as JettyHandler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Server as JettyServer
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.io.EofException
import com.danneu.kog.adapters.Servlet
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.websocket.server.WebSocketServerFactory
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler
import org.eclipse.jetty.server.Request as JettyServerRequest


// Lift a kog handler into a jetty handler
class JettyHandler(val handler: Handler, val insertContextHandler: (String, WebSocketAcceptor) -> Unit) : AbstractHandler() {
    val installedSocketHandlers: MutableSet<String> = mutableSetOf()

    override fun handle(target: String, baseRequest: JettyServerRequest, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) {
        val request = Servlet.intoKogRequest(servletRequest)
        val response = handler(request)
        if (response.status == Status.switchingProtocols && response.webSocket != null && WebSocketServerFactory().isUpgradeRequest(servletRequest, servletResponse)) {
            val (key, accept) = response.webSocket!!
            if (!installedSocketHandlers.contains(key)) {
                installedSocketHandlers.add(key)
                insertContextHandler(key, accept)
            }
            // HACK: Store info in this Any bucket
            servletRequest.setAttribute("kog-request", request)
        } else {
            Servlet.updateServletResponse(servletResponse, response)
            baseRequest.isHandled = true
        }
    }
}


class Server(val handler: Handler = { Response(Status.notFound) }, val websockets: Map<String, WebSocketAcceptor> = emptyMap()) {
    val jettyServer: JettyServer = makeJettyServer()

    fun stop() {
        println("kog server stopping...")
        jettyServer.stop()
        jettyServer.handler = null
        println("kog server stopped")
    }

    fun listen(port: Int, wait: Boolean = true, onStart: (Server) -> Unit = {}): Server {
        (jettyServer.connectors.first() as ServerConnector).port = port

        val handlers = HandlerCollection(true)

        val insertContextHandler: (key: String, accept: WebSocketAcceptor) -> Unit = { key, accept ->
            val context = ContextHandler(key)
            context.handler = WebSocket.handler(accept)
            context.allowNullPathInfo = true  // don't redirect /foo to /foo/
            context.server = jettyServer
            context.start()
            handlers.addHandler(context)
        }

        handlers.addHandler(JettyHandler(Server.middleware()(handler), insertContextHandler))

        jettyServer.handler = handlers

        try {
            println("kog server starting...")
            jettyServer.start()
            println("kog server started on port $port")
            onStart(this)
            if (wait) {
                jettyServer.join()
            }
            return this
        } catch (e: Exception) {
            jettyServer.stop()
            throw e
        }
    }

    companion object {
        fun makeJettyServer(): JettyServer {
            val threadPool = QueuedThreadPool(50)
            val server = JettyServer(threadPool)
            val httpConfig = HttpConfiguration()
            httpConfig.sendServerVersion = false
            val httpFactory = HttpConnectionFactory(httpConfig)
            val serverConnector = ServerConnector(server, httpFactory)
            serverConnector.idleTimeout = 200000
            server.addConnector(serverConnector)
            return server
        }
    }
}


// MIDDLEWARE


// The server's stack of top-level middleware. This should always wrap the user's final handler.
fun Server.Companion.middleware(): Middleware = composeMiddleware(
    // First middleware in this list touches request first and response last
    wrapHead(),
    wrapFinalize(),
    wrapErrorHandler(),
    wrapCookies()
)


// Must be last middleware to touch the response
internal fun Server.Companion.wrapFinalize(): Middleware = { handler -> { req ->
    handler(req).finalize()
}}


// NOTE: Jetty already handles HEAD requests, but we'll apply this anyways.
// This middleware assumes that a downstream router handled the HEAD request
// like a GET request. But now we remove the body.
//
// NOTE: We can't treat a HEAD as a GET at this stage since we want to
// let the user perceive HEAD requests in their own middleware, so that's
// why we assume a downstream router routed it as a GET.
//
// TODO: Is there a way to implement this without relying on downstream routers to route HEAD->GET?
internal fun Server.Companion.wrapHead(): Middleware {
    fun headResponse(request: Request, response: Response): Response {
        return when (request.method) {
            Method.Head -> response.setBody(ResponseBody.None)
            else -> response
        }
    }

    return { handler -> { request -> headResponse(request, handler(request)) }}
}


// Catches uncaught errors and lifts them into 500 responses
internal fun Server.Companion.wrapErrorHandler(): Middleware = { handler -> { req ->
    try {
        handler(req)
    } catch (ex: EofException) {
        // We can't do anything about early client hangup
        Response(Status.internalError)
    } catch (ex: Exception) {
        System.err.print("Unhandled error: ")
        ex.printStackTrace(System.err)
        Response(Status.internalError)
    }
}}


// TODO: Doesn't really make sense as battery middleware at the moment since request cookies are currently lazy-parsed and
// it's silly to need middleware just to flush the Request#cookies data-structure. But I'm going to leave
// this here as a stub since in the future I'd like to lean more heavily on middleware. Though right now
// I'm not sure how to do that without turning everything into MutableMap<String, Any?> which I don't want to do.
//
// Til I think of something better, this middleware is part of the default Server#middleware stack
// so that the user doesn't need to plug it since the distinction is weak.
internal fun Server.Companion.wrapCookies(): Middleware {
    fun cookieResponse(response: Response): Response {
        for ((name, cookie) in response.cookies.iterator()) {
            response.appendHeader(Header.SetCookie, cookie.serialize(name))
        }
        return response
    }

    return { handler -> { request -> cookieResponse(handler(request)) }}
}





