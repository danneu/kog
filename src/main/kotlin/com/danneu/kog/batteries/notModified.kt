package com.danneu.kog.batteries

import com.danneu.kog.Header
import com.danneu.kog.Method
import com.danneu.kog.Middleware
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.ResponseBody
import com.danneu.kog.Status
import com.danneu.kog.util.HttpDate
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId


fun notModified(etag: Boolean): Middleware = { handler -> handler@ { request ->
    val response = handler(request)

    // only consider HEAD and GET requests
    if (request.method != Method.Get && request.method != Method.Head) {
        return@handler response
    }

    // only consider 200 responses
    if (response.status != Status.Ok) {
        return@handler response
    }

    // add etag header
    if (etag) {
        response.setHeader(Header.Etag, response.body.etag)
    }

    // add last-modified header if body has that info
    response.body.apply {
        if (this is ResponseBody.File) {
            val instant = Instant.ofEpochMilli(body.lastModified())
            val datetime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault())
            response.setHeader(Header.LastModified, HttpDate.toString(datetime))
        }
    }

    // only consider stale requests
    if (!isCached(request, response)) {
        return@handler response
    }

    // tell client that their cache is still valid
    Response.notModified()
}}


private fun isCached(request: Request, response: Response): Boolean {
    return etagsMatch(request, response) || notModifiedSince(request, response)
}


private fun notModifiedSince(request: Request, response: Response): Boolean {
    // ensure headers exist
    val modifiedAtString = response.getHeader(Header.LastModified) ?: return false
    val targetString = request.getHeader(Header.IfModifiedSince) ?: return false

    // ensure headers parse into dates
    val modifiedAt = HttpDate.fromString(modifiedAtString) ?: return false
    val target = HttpDate.fromString(targetString) ?: return false

    // has entity not been touched since client's target?
    return modifiedAt < target
}


private fun etagsMatch(request: Request, response: Response): Boolean {
    val etag = response.getHeader(Header.Etag) ?: return false
    val target = request.getHeader(Header.IfNoneMatch) ?: return false
    return etag == target
}
