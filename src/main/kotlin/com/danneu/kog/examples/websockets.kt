package com.danneu.kog.examples

import com.danneu.kog.Handler
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.SafeRouter
import com.danneu.kog.Server
import com.danneu.kog.WebSocket
import com.danneu.kog.batteries.logger
import com.danneu.kog.websocket

fun main(args: Array<String>) {
    val router = SafeRouter {
        get("/", fun(): Handler = {
            Response().html("""
                <p>open the browser's javascript console</p>

                <script>
                    var socket = new WebSocket("ws://localhost:3000/ws/echo")

                    socket.onopen = function (event) {
                        console.log('open:', event)
                        socket.send('hello world')
                    }

                    socket.onclose = function (event) {
                        console.log('close:', event)
                    }

                    socket.onerror = function (event) {
                        console.log('error:', event)
                    }

                    socket.onmessage = function (event) {
                        console.log('server said:', event.data)
                    }
                </script>

                <script>
                    var socket2 = new WebSocket("ws://localhost:3000/ws/42")

                    socket2.onmessage = function (event) {
                        console.log(socket2.url, 'said:', event.data)
                    }
                </script>
            """)
        })

        // This endpoint will open a websocket connection that echos back any text the client sends it.
        get("/ws/echo", fun(): Handler = {
            // Current limitation: The first argument to Response.websocket() must be a static url path.
            // It does *not* accept route patterns like "/ws/<num>". (#willfix)
            Response.websocket("/ws/echo", fun(request: Request, websocket: WebSocket) {
                // Upon each websocket connection at this endpoint, generate a random id for it
                val id = java.util.UUID.randomUUID()
                println("[$id] a client connected")

                websocket.onText = { message: String ->
                    println("[$id] client sent us: $message")
                    websocket.session.remote.sendString(message)
                }

                websocket.onError = { cause: Throwable ->
                    println("[$id] onError: ${cause.message}")
                }

                websocket.onClose = { statusCode: Int, reason: String? ->
                    println("[$id] onClose: $statusCode ${reason ?: "<no reason>"}")
                }
            })
        })

        // This endpoint demonstrates how to mount a websocket handler on a dynamic URL that accepts request
        // paths like /ws/1, /ws/2, /ws/3, etc.
        //
        // It also demonstrates the current limitation that the first argument to `Response.websocket()` must
        // be a static path. (#willfix)
        //
        // Due to this limitation, dynamic path websocket handlers by default will currently cause unbounded growth of
        // the internal jetty mapping of path to websocket handler until I find a better way to work with jetty.
        get("/ws/<>", fun(n: Int): Handler = {
            Response.websocket("/ws/$n", fun(request: Request, websocket: WebSocket) {
                websocket.session.remote.sendString("you connected to /ws/$n")
            })
        })
    }

    val middleware = logger()
    val handler = middleware(router.handler())

    Server(middleware(handler)).listen(3000)
}

