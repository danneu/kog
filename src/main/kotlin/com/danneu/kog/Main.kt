package com.danneu.kog


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val handler: Handler = { Response().text("Hello, world!") }
            Server(handler).listen(Env.int("PORT") ?: 3000)
        }
    }
}

