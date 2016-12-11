package com.danneu.kog.batteries

import com.danneu.kog.Header
import com.danneu.kog.Request
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import java.io.File
import java.nio.file.Path
import java.time.Duration


fun serveStatic(publicRootString: String, maxAge: Duration = Duration.ZERO): Middleware = { handler ->
    val publicRoot = File(publicRootString).toPath().normalize().toAbsolutePath()

    fun(request: Request): Response {
        // Only serve assets to HEAD or GET
        if (request.method != Method.Head && request.method != Method.Get) {
            return handler(request)
        }

        val assetPath = publicRoot.resolve(File(request.path.drop(1)).toPath())

        // Ensure request path is downstream from public root
        // Note: This is actually handled by Jetty which throws a 400 on a malicious path and kog doesn't even
        //       get the request. But I left this logic in anyways.
        if (!publicRoot.isParentOf(assetPath)) {
            return Response.badRequest()
        }

        val asset = assetPath.toFile()

        // We can only serve files (which is also the existence check)
        if (!asset.isFile) {
            return handler(request)
        }

        return Response()
          .file(asset)
          .setHeader(Header.CacheControl, "public, max-age=${maxAge.seconds}")
    }
}


private fun Path.isParentOf(other: Path): Boolean {
    return other.startsWith(this)
}

