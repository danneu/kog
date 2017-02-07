package com.danneu.kog.batteries

import com.danneu.kog.Header
import com.danneu.kog.Request
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import com.danneu.kog.clamp
import java.io.File
import java.nio.file.Path
import java.time.Duration


fun serveStatic(publicFolderPath: String, maxAge: Duration): Middleware = handler@ { handler ->
    @Suppress("NAME_SHADOWING")
    val maxAge = maxAge.clamp(Duration.ZERO, Duration.ofDays(365))

    val publicRoot = File(publicFolderPath)

    if (!publicRoot.isDirectory) {
        System.err.println("WARN [serveStatic] Could not find public resource folder: \"$publicFolderPath\". serveStatic skipped...")
        return@handler { req -> handler(req) }
    }

    println("[serveStatic] will be serving from: ${publicRoot.absolutePath}")

    fun(request: Request): Response {
        // Only serve assets to HEAD or GET
        if (request.method != Method.Head && request.method != Method.Get) {
            return handler(request)
        }

        if (request.path == "/") {
            return handler(request)
        }

        val asset = publicRoot.resolve(File(request.path.drop(1)))

        // TODO: Test this since I made blind changes to it.
        // Ensure request path is downstream from public root
        // Note: This is actually handled by Jetty which throws a 400 on a malicious path and kog doesn't even
        //       get the request. But I left this logic in anyways.
        if (!publicRoot.toPath().isParentOf(asset.toPath())) {
            return Response.badRequest()
        }

        // We can only serve files (which is also the existence check)
        if (!asset.isFile) {
            return handler(request)
        }

        return Response()
          .file(asset)
          .setHeader(Header.CacheControl, "public, max-age=${maxAge.seconds}")
    }
}


/**
 * Returns true if `other` path is downstream from this path.
 *
 * i.e. If this path is a prefix of `other` path.
 */
private fun Path.isParentOf(other: Path): Boolean {
    return other.startsWith(this)
}

