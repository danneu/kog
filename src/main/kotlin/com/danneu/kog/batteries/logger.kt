package com.danneu.kog.batteries

import com.danneu.kog.Request
import com.danneu.kog.Middleware
import com.danneu.kog.Response


fun logger(): Middleware = { handler -> { request ->
    logRequest(request)
    val start = System.currentTimeMillis()
    val response = handler(request)
    logResponse(request, response, start)
    response
}}


// HELPERS


private fun logRequest(req: Request) {
    println("${Color.gray.wrap("-->")} ${req.method.name.toUpperCase()} ${Color.gray.wrap(req.path)}")
}


private fun logResponse(req: Request, res: Response, start: Long) {
    val color: Color = when (res.status.code) {
        in 500..600-1 -> Color.red
        in 400..500-1 -> Color.yellow
        in 300..400-1 -> Color.cyan
        in 200..300-1 -> Color.green
        in 100..200-1 -> Color.green
        else -> Color.red
    }

    println("${Color.gray.wrap("<--")} ${req.method.name.toUpperCase()} ${Color.gray.wrap(req.path)} ${color.wrap(res.status.code.toString())} ${time(start)}")
}


private fun time(start: Long): String {
    return (System.currentTimeMillis() - start).toString() + "ms"
}


private enum class Color(val code: String) {
    none("0m"),
    red("0;31m"),
    green("0;32m"),
    yellow("0;33m"),
    blue("0;34m"),
    magenta("0;35m"),
    cyan("0;36m"),
    white("0;37m"),
    gray("0;90m");

    val escape = "${String(Character.toChars(0x001B))}["

    fun wrap (str: String): String {
        return "$escape${this.code}$str$escape${none.code}"
    }
}
