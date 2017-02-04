
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
import com.danneu.kog.middleware.composeMiddleware
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.websocket.server.WebSocketServerFactory
import java.time.Duration
import org.eclipse.jetty.websocket.server.WebSocketHandler as JettyWebSocketHandler
import org.eclipse.jetty.server.Request as JettyServerRequest


// Lift a kog handler into a jetty handler
class JettyHandler(val handler: Handler, val insertContextHandler: (String, WebSocketAcceptor) -> Unit) : AbstractHandler() {
    val installedSocketHandlers: MutableSet<String> = mutableSetOf()

    override fun handle(target: String, baseRequest: JettyServerRequest, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) {
        val request = Servlet.intoKogRequest(servletRequest)
        val response = handler(request)
        if (response.status == Status.SwitchingProtocols && response.webSocket != null && request.isUpgrade()) {
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


class Server @JvmOverloads constructor(
    val handler: Handler = { Response.notFound() },
    val websockets: Map<String, WebSocketAcceptor> = emptyMap(),
    val minThreads: Int = 8,
    val maxThreads: Int = 50,
    val idleTimeout: Duration = Duration.ofSeconds(30)
) {
    val jettyServer = makeJettyServer()

    fun stop(timeout: Duration = Duration.ofSeconds(3)) {
        println(":: <kog> stopping (timeout = ${timeout.toMillis()}ms)...")
        jettyServer.stopTimeout = timeout.toMillis()
        jettyServer.stop()
        jettyServer.handler = null
        println(":: <kog> stopped")
    }

    @JvmOverloads fun listen(port: Int, wait: Boolean = true, onStart: (Server) -> Unit = {}): Server {
        if (port == 0) {
            println(":: <kog> requesting random port since port == 0")
        }

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
            jettyServer.start()
            val actualPort = (jettyServer.connectors[0] as ServerConnector).localPort
            println(":: <kog> listening on http://localhost:$actualPort")
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

    fun makeJettyServer(): JettyServer {
        val threadPool = QueuedThreadPool(maxThreads).apply {
            this.minThreads = this@Server.minThreads
        }
        val server = JettyServer(threadPool)
        val httpConfig = HttpConfiguration().apply {
            this.sendServerVersion = false
        }
        val httpFactory = HttpConnectionFactory(httpConfig)
        val serverConnector = ServerConnector(server, httpFactory).apply {
            this.idleTimeout = this@Server.idleTimeout.toMillis()
        }
        server.addConnector(serverConnector)
        return server
    }

    companion object
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
            Method.Head -> response.apply { body = ResponseBody.None }
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
        Response.internalError()
    } catch (ex: Exception) {
        System.err.print("Unhandled error: ")
        ex.printStackTrace(System.err)
        Response.internalError()
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





