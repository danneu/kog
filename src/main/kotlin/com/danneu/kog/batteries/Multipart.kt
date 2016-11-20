package com.danneu.kog.batteries

import com.danneu.kog.Request
import com.danneu.kog.Middleware
import com.danneu.kog.Response
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.RequestContext
import org.apache.commons.fileupload.util.Streams
import java.io.File
import java.io.InputStream

private fun fileSequence(iter: FileItemIterator): Sequence<FileItemStream> = generateSequence {
    if (iter.hasNext()) {
        iter.next()
    } else {
        null
    }
}

private fun Request.context() = object : RequestContext {
    // TODO: parse char encoding myself
    override fun getCharacterEncoding(): String = this@context.charset ?: "utf-8"
    override fun getContentLength(): Int = this@context.length ?: -1
    override fun getContentType(): String = this@context.type!! // guarantee this exists before trying to get context
    override fun getInputStream(): InputStream = this@context.body
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
                // this is going to be the name of the uploaded file, not sure we need
                //println("form field ${item.fieldName} with value ${Streams.asString(item.openStream())} detected")
                //Pair(item.fieldName, Streams.asString(item.openStream(), "UTF-8"))
                println("isFOrmFireld === ${Streams.asString(item.openStream(), "UTF-8")}")
                null
            } else {
                val file = File.createTempFile("klobb-multipart-", null)
                file.deleteOnExit()
                fileSet.add(file)
                item.openStream().copyTo(file.outputStream())
                val savedUpload = SavedUpload(file, item.name, item.contentType, file.length())
                Pair(item.fieldName, savedUpload)
            }
        }.filterNotNull().forEach { pair ->
            req.uploads.set(pair.first, pair.second)
        }

        return handler(req)
    }
}


class SavedUpload(val file: File, val filename: String, val contentType: String, val length: Long) {
    override fun toString(): String {
        return "[SavedUpload filename=$filename contentType=$contentType length=$length]"
    }
}

