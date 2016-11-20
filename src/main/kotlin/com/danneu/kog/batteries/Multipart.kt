package com.danneu.kog.batteries

import com.danneu.kog.Request
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.UploadContext
import java.io.File
import java.io.InputStream

private fun fileSequence(iter: FileItemIterator): Sequence<FileItemStream> = generateSequence {
    if (iter.hasNext()) {
        iter.next()
    } else {
        null
    }
}

private fun Request.context() = object : UploadContext {
    // RequestContext
    override fun getCharacterEncoding(): String = this@context.charset ?: "utf-8"
    override fun getContentLength(): Int = this@context.length ?: -1
    override fun getContentType(): String = this@context.type!! // guarantee this exists before trying to get context
    override fun getInputStream(): InputStream = this@context.body
    // UploadContext (Note: Jetty's Request has Int content-length so this can't actually be a Long in practice)
    override fun contentLength(): Long = this@context.length?.toLong() ?: -1
}


private fun backgroundThread(f: Runnable) {
    val thread = Thread(f)
    thread.isDaemon = true
    thread.start()
}

// ttl = delete upload temp files after `ttl` milliseconds (default = 1 hour)
// interval = check for expired temp files every `interval` milliseconds (default = 10 seconds)
// TODO: field whitelist?
fun multipart(ttl: Long = 3600 * 1000, interval: Long = 10000): Middleware = { handler ->
    // Set of temp files that we need to delete once expired
    val fileSet: MutableSet<File> = mutableSetOf()

    Runtime.getRuntime().addShutdownHook(Thread(Runnable { for (file in fileSet.iterator()) { file.delete() } }))

    fun expired(file: File): Boolean = file.lastModified() < System.currentTimeMillis() - ttl

    // TODO: Instead of starting a fileSet + concurrent cleanup every time this is invoked, use some
    // sort of central store / schedule.
    backgroundThread(Runnable {
        while (true) {
            Thread.sleep(interval)
            val iter = fileSet.iterator()
            for (file in iter) {
                if (expired(file)) {
                    file.delete()
                    iter.remove()
                }
            }
        }
    })

    fun(req: Request): Response {
        if (req.type != "multipart/form-data") {
            return handler(req)
        }

        val upload = FileUpload()
        val iter: FileItemIterator = upload.getItemIterator(req.context())

        fileSequence(iter).map { item ->
            if (item.isFormField) {
                // If we're in this branch, it means ${Streams.asString(item.openStream(), "utf-8")} is going
                // to be the name of the uploaded file. Not sure we need this.
                null
            } else {
                val file = File.createTempFile("kog-multipart-", null)
                file.deleteOnExit()
                fileSet.add(file)
                item.openStream().copyTo(file.outputStream())
                val savedUpload = SavedUpload(file, item.name, item.contentType, file.length())
                Pair(item.fieldName, savedUpload)
            }
        }.filterNotNull().forEach { pair ->
            req.uploads[pair.first] = pair.second
        }

        return handler(req)
    }
}


class SavedUpload(val file: File, val filename: String, val contentType: String, val length: Long) {
    override fun toString(): String {
        return "[SavedUpload filename=$filename contentType=$contentType length=$length]"
    }
}

