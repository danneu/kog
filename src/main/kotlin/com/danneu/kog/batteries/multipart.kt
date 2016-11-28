package com.danneu.kog.batteries

import com.danneu.kog.Header
import com.danneu.kog.Request
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import com.danneu.kog.batteries.multipart.SavedUpload
import com.danneu.kog.batteries.multipart.Whitelist
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
    // needs the full header (i.e. with the boundary) which is why we can't just use this.type
    // TODO: What happens when content-type is not of a format UploadContext expects?
    override fun getContentType(): String = this@context.getHeader(Header.ContentType) ?: ""
    override fun getInputStream(): InputStream = this@context.body
    // UploadContext (Note: Jetty's Request has Int content-length so this can't actually be a Long in practice)
    override fun contentLength(): Long = this@context.length?.toLong() ?: -1
}


private fun backgroundThread(f: Runnable) {
    val thread = Thread(f)
    thread.isDaemon = true
    thread.start()
}

// whitelist = set of fieldnames to handle so that we don't do unnecessary work
// ttl = delete upload temp files after `ttl` milliseconds (default = 1 hour)
// interval = check for expired temp files every `interval` milliseconds (default = 10 seconds)
fun multipart(whitelist: Whitelist, ttl: Long = 3600 * 1000, interval: Long = 10000): Middleware = { handler ->
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

        fileSequence(iter).filter {
            // only parse fields in our whitelist
            whitelist.contains(it.fieldName)
        }.map { item ->
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
