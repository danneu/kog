package com.danneu.kog.batteries

import com.danneu.kog.Request
import com.danneu.kog.Middleware
import com.danneu.kog.Response


fun logger(): Middleware = { handler -> { request ->
    logRequest(request)
    val start = System.currentTimeMillis()
    val response = handler(request)
    logResponse(request, response, start)
}}


// HELPERS


private fun logRequest(req: Request) {
    println("${Color.Gray.wrap("-->")} ${req.method.name.toUpperCase()} ${Color.Gray.wrap(req.path)}")
}


private fun logResponse(request: Request, response: Response, start: Long): Response {
    val color: Color = when (response.status.code) {
        in 500..600-1 -> Color.Red
        in 400..500-1 -> Color.Yellow
        in 300..400-1 -> Color.Cyan
        in 200..300-1 -> Color.Green
        in 100..200-1 -> Color.Green
        else -> Color.Red
    }

    println("${Color.Gray.wrap("<--")} ${request.method.name.toUpperCase()} ${Color.Gray.wrap(request.path)} ${color.wrap(response.status.code.toString())} ${time(start)}")

    return response
}


private fun time(start: Long): String {
    return (System.currentTimeMillis() - start).toString() + "ms"
}


private enum class Color(val code: String) {
    None("0m"),
    Red("0;31m"),
    Green("0;32m"),
    Yellow("0;33m"),
    Blue("0;34m"),
    Magenta("0;35m"),
    Cyan("0;36m"),
    White("0;37m"),
    Gray("0;90m");

    val escape = "${String(Character.toChars(0x001B))}["

    fun wrap(str: String) = "$escape$code$str$escape${None.code}"
}
