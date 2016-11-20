
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
import org.eclipse.jetty.server.handler.HandlerList


// Lift a kog handler into a jetty handler
class JettyHandler(val handler: Handler) : AbstractHandler() {
    override fun handle(target: String, baseRequest: org.eclipse.jetty.server.Request, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) {
        val request = Servlet.intoKogRequest(servletRequest)
        val response = handler(request)
        Servlet.updateServletResponse(servletResponse, response)
        baseRequest.isHandled = true
    }
}


class Server(val handler: Handler = { Response(Status.notFound) }, val websocket: ((WebSocket) -> Unit)?) {
    val jettyServer: org.eclipse.jetty.server.Server

    init {
        val threadPool = QueuedThreadPool(50)
        val server = org.eclipse.jetty.server.Server(threadPool)
        val httpConfig = HttpConfiguration()
        httpConfig.sendServerVersion = false
        val httpFactory = HttpConnectionFactory(httpConfig)
        val serverConnector = ServerConnector(server, httpFactory)
        serverConnector.idleTimeout = 200000
        server.addConnector(serverConnector)
        jettyServer = server
    }

    fun listen(port: Int): Server {
        (jettyServer.connectors.first() as ServerConnector).port = port

        val kogHandler = run {
            val middleware: Middleware = composeMiddleware(
              ::finalizer,
              ::errorHandler
            )
            JettyHandler(middleware(handler))
        }

        val handlers = HandlerList().apply {
            if (websocket == null) {
                handlers = arrayOf(kogHandler)
            } else {
                val wsHandler = ContextHandler().apply { handler = WebSocket.handler(websocket) }
                handlers = arrayOf(wsHandler, kogHandler)
            }
        }

        jettyServer.handler = handlers

        try {
            jettyServer.start()
            jettyServer.join()
            return this
        } catch (ex: Exception) {
            jettyServer.stop()
            throw ex
        }
    }
}


// TOP-LEVEL SERVER MIDDLEWARE


// Must be last middleware to touch the response
fun finalizer(handler: Handler): Handler {
    return { req -> handler(req).finalize() }
}


// Catches uncaught errors and lifts them into 500 responses
fun errorHandler(handler: Handler): Handler {
    return { req ->
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
    }
}
