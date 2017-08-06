package com.danneu.kog.examples

import com.danneu.kog.Handler
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.Router
import com.danneu.kog.Server
import com.danneu.kog.WebSocketHandler
import com.danneu.kog.batteries.logger
import com.danneu.kog.websocket
import org.eclipse.jetty.websocket.api.Session

fun main(args: Array<String>) {
    val router = Router {
        get("/", fun(): Handler = {
            Response().html("""
                <!doctype html>
                <head>
                    <meta charset="utf-8">
                </head>
                <body>
                    <p>open the browser's javascript console</p>

                    <ul id="log"></ul>

                    <script>
                        var socket = new WebSocket("ws://localhost:3000/ws/echo")

                        socket.onopen = function (event) {
                            console.log('open:', event)
                            socket.send('hello world')

                            // Every 10 seconds, ping the server to avoid idle timeout.
                            setInterval(function () {
                                socket.send('ping')
                            }, 10000)
                        }

                        socket.onclose = function (event) {
                            console.log('close:', event)
                        }

                        socket.onerror = function (event) {
                            console.log('error:', event)
                        }

                        socket.onmessage = function (event) {
                            console.log('server said:', event.data)
                            var ul = document.querySelector('#log')
                            var li = document.createElement('li')
                            li.appendChild(document.createTextNode('server said: ' + event.data))
                            ul.appendChild(li)
                        }
                    </script>
                </body>
            """)
        })

        // This endpoint will open a websocket connection that echos back any text the client sends it.
        get("/ws/echo", fun(): Handler = {
            // Current limitation: The first argument to Response.websocket() must be a static url path.
            // It does *not* accept route patterns like "/ws/<num>". (#willfix)
            Response.websocket("/ws/echo", fun(_: Request, session: Session): WebSocketHandler {
                // Upon each websocket connection at this endpoint, generate a random id for it
                val id = java.util.UUID.randomUUID()

                return object : WebSocketHandler {
                    override fun onOpen() {
                        println("[$id] a client connected")
                    }

                    override fun onText(message: String) {
                        // Client sends us "ping" messages to keep the connection alive. Don't echo them.
                        if (message == "ping") return

                        println("[$id] client sent us: $message")
                        session.remote.sendString(message)
                    }

                    override fun onError(cause: Throwable) {
                        println("[$id] onError: ${cause.message}")
                    }

                    override fun onClose(statusCode: Int, reason: String?) {
                        println("[$id] onClose: $statusCode ${reason ?: "<no reason>"}")
                    }
                }
            })
        })
    }

    val middleware = logger()
    val handler = middleware(router.handler())

    Server(middleware(handler)).listen(3000)
}

// WARNING

// This endpoint demonstrates how to mount a websocket handler on a dynamic URL that accepts request
// paths like /ws/1, /ws/2, /ws/3, etc.
//
// It also demonstrates the current limitation that the first argument to `Response.websocket()` must
// be a static path. (#willfix)
//
// Due to this limitation, dynamic path websocket handlers by default will currently cause unbounded growth of
// the internal jetty mapping of path to websocket handler until I find a better way to work with jetty.
//
// get("/ws/<>", fun(n: Int): Handler = {
//     Response.websocket("/ws/$n", fun(_: Request, session: Session) = object : WebSocketHandler {
//         override fun onOpen() {
//             session.remote.sendString("you connected to /ws/$n")
//         }
//     })
// })
