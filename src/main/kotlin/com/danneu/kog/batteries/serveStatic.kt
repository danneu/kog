package com.danneu.kog.batteries

import com.danneu.kog.Request
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import com.danneu.kog.Status
import java.io.File
import java.nio.file.Path

fun serveStatic(publicRootString: String, maxAge: Long = 0): Middleware = { handler ->
    val publicRoot = File(publicRootString).toPath().normalize().toAbsolutePath()

    fun(request: Request): Response {
        // Only serve assets to HEAD or GET
        if (request.method != Method.head && request.method != Method.get) {
            return handler(request)
        }

        val assetPath = publicRoot.resolve(File(request.path.drop(1)).toPath())

        // Ensure request path is downstream from public root
        // Note: This is actually handled by Jetty which throws a 400 on a malicious path and kog doesn't even
        //       get the request. But I left this logic in anyways.
        if (!publicRoot.isParentOf(assetPath)) {
            return Response(Status.badRequest)
        }

        val asset = assetPath.toFile()

        // We can only serve files (which is also the existence check)
        if (!asset.isFile) {
            return handler(request)
        }

        return Response()
          .file(asset)
          .setHeader("Cache-Control", "public, max-age=$maxAge")
    }
}


private fun Path.isParentOf(other: Path): Boolean {
    return other.startsWith(this)
}

