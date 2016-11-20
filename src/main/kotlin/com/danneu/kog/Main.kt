package com.danneu.kog


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val handler: Handler = { Response().text("Hello world") }
            Server(handler).listen(9000)
        }
    }
}

