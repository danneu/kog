
package com.danneu.kog

import com.danneu.kog.batteries.multipart
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
import javax.servlet.http.HttpServlet
import org.eclipse.jetty.io.EofException

class MyServlet(val serviceMethod: (HttpServlet, HttpServletRequest, HttpServletResponse) -> Unit) : HttpServlet() {
    override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        //super.service(req, resp)
        serviceMethod(this, req!!, resp!!)

    }

    companion object {
        fun servlet(handler: Handler): MyServlet {
            // Make blocking service method
            val serviceMethod = { servlet: HttpServlet, servletReq: HttpServletRequest, servletRes: HttpServletResponse ->
                val request = Request.fromServletRequest(servletReq)
                val response = handler(request).finalize()
                updateServletResponse(servletRes, response)
            }
            return MyServlet(serviceMethod)
        }

        fun updateServletResponse(servletResponse: HttpServletResponse, response: Response) {
            servletResponse.status = response.status.code

            // Set headers
            for ((k, v) in response.headers.iterator()) {
                servletResponse.addHeader(k, v)
            }

            // Some headers have special setters
            val type = response.getHeader("Content-Type")
            if (type != null) {
                servletResponse.contentType = type
            }

            response.body.pipe(servletResponse.outputStream)
        }
    }
}


class JettyProxyHandler(val handler: Handler) : AbstractHandler() {
    override fun handle(target: String?, baseRequest: org.eclipse.jetty.server.Request?, servletRequest: HttpServletRequest?, servletResponse: HttpServletResponse?) {
        val request = Request.fromServletRequest(servletRequest!!)
        val response = handler(request).finalize()
        MyServlet.updateServletResponse(servletResponse!!, response)
        baseRequest!!.isHandled = true
    }
}



class Server(val handler: Handler) {
    lateinit var jettyServer: org.eclipse.jetty.server.Server

    fun listen(port: Int): Server {
        val threadPool = QueuedThreadPool(50)
        val server = org.eclipse.jetty.server.Server(threadPool)
        val httpConfig = HttpConfiguration()
        httpConfig.sendServerVersion = false
        val httpFactory = HttpConnectionFactory(httpConfig)
        val serverConnector = ServerConnector(server, httpFactory)
        serverConnector.port = port
        serverConnector.idleTimeout = 200000
        server.addConnector(serverConnector)
        jettyServer = server

        val wrapErrorHandler: Middleware = { handler ->
            { req ->
                try {
                    handler(req)
                } catch (ex: EofException) {
                    Response(Status.internalError).text("Internal Error")
                } catch (ex: Exception) {
                    System.err.print("Unhandled error: ")
                    ex.printStackTrace(System.err)
                    Response(Status.internalError).text("Internal Error")
                }
            }
        }

        val proxiedHandler = JettyProxyHandler(wrapErrorHandler(handler))
        jettyServer.handler = proxiedHandler
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
