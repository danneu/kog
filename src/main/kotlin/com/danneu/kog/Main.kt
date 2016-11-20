package com.danneu.kog


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val router = Router {
                get("/") {
                    Response()
                      .setHeader("Access-Control-Allow-Origin", "*")
                      .text("Hello, World!")
                }
                websocket("/foo") { request: Request, socket: WebSocket ->
                    println("[foo] a client connected")
                    socket.onClose = { statusCode, reason -> println("[foo] a client disconnected") }
                    socket.onText = { text ->
                        println("[foo] said $text")
                        socket.session.remote.sendString(text)
                    }
                }
                websocket("/bar") { request: Request, socket: WebSocket ->
                    println("[bar] a client connected")
                    socket.onClose = { statusCode, reason -> println("[bar] a client disconnected") }
                    socket.onText = { text ->
                        println("[bar] said $text")
                        socket.session.remote.sendString(text)
                    }
                }
            }
            val server = Server(router.handler())
            server.listen(9000)
        }
    }
}

