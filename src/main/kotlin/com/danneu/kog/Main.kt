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
                websocket("/foo", key = "foo")
                websocket("/bar", key = "bar")
            }
            val server = Server(router.handler(), websockets = mapOf(
              "foo" to { request: Request, socket: WebSocket ->
                  println("[foo] a client connected")
                  socket.onClose = { statusCode, reason ->
                      println("[foo] a client disconnected")
                  }
              },
              "bar" to { request: Request, socket: WebSocket ->
                  println("[bar] a client connected")
                  socket.onClose = { statusCode, reason ->
                      println("[bar] a client disconnected")
                  }
              }
            ))
            server.listen(9000)
        }
    }
}

