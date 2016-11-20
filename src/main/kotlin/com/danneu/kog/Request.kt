package com.danneu.kog

import com.danneu.kog.batteries.SavedUpload
import com.danneu.kog.cookies.parse
import com.danneu.kog.json.Decoder
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import java.nio.charset.Charset
import java.util.Locale
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest


class Request(
  val serverPort: Int,
  val serverName: String,
  val remoteAddr: String,
  val href: String,
  var queryString: String?,
  val scheme: String,
  var method: Method,
  val protocol: String,
  override val headers: MutableList<Header>,
  val type: String?,
  val length: Int?, // Either >= 0 or null
  val charset: String?,
  // TODO: val sslClientCert
  val body: ServletInputStream,
  var path: String
) : HasHeaders<Request> {

    val query: MutableMap<String, String> by lazy { formDecode(queryString).mutableCopy() }

    // multipart middleware populates this with name -> file mappings for multipart uploads
    val uploads: MutableMap<String, SavedUpload> = mutableMapOf()

    val cookies: MutableMap<String, String> by lazy { parse(this.getHeader("cookie")).mutableCopy() }

    // TODO: At framework level, need to avoid reading stream when it is already being/been consumed or come up with a deliberate gameplan.
    fun <T : Any> json(decoder: Decoder<T>): Result<T, Exception> {
        val bodyString = this.body.readBytes().toString(Charset.defaultCharset())
        return Decoder.tryParse(bodyString).flatMap { jsonValue -> decoder(jsonValue) }
    }

    override fun toString(): String {
        return listOf(
          Pair("serverPort", serverPort),
          Pair("serverName", serverName),
          Pair("remoteAddr", remoteAddr),
          Pair("href", href),
          Pair("queryString", queryString),
          Pair("scheme", scheme),
          Pair("method", method.toString()),
          Pair("protocol", protocol),
          Pair("headers", headers),
          Pair("type", type),
          Pair("length", length),
          Pair("charset", charset),
          Pair("path", path)
        ).map{ pair -> pair.toString() }.joinToString("\n")
    }

    companion object {
        fun fromServletRequest(r: HttpServletRequest): Request {
            return Request(
                serverPort = r.serverPort,
                serverName = r.serverName,
                remoteAddr = r.remoteAddr,
                href = r.requestURL.toString() + if (r.queryString != null) { "?" + r.queryString } else { "" },
                queryString = r.queryString,
                scheme = r.scheme,
                method = Method.Companion.fromString(r.method.toLowerCase(Locale.ENGLISH)),
                protocol = r.protocol,
                headers = expandHeaders(r),
                //type = notNullThen(r.contentType) { it.split(";", limit = 1)[0].toLowerCase() },
                type = r.contentType?.let { it.split(";", limit = 1)[0].toLowerCase() },
                length = if (r.contentLength >= 0) { r.contentLength } else { null },
                charset = notNullThen(r.characterEncoding, String::toLowerCase),
                //sslClientCert = r.getAttribute("javax.servlet.request.X509Certificate").first()
                body = r.inputStream,
                path = r.pathInfo ?: "/"
            )
        }
        fun toy(method: Method = Method.get, path: String = "/", queryString: String = "foo=bar"): Request {
            return Request(
              serverPort = 3000,
              serverName = "name",
              remoteAddr = "1.2.3.4",
              href = path + if (queryString.isNotBlank()) { "?" + queryString } else { "" },
              queryString = queryString,
              scheme = "http",
              method = method,
              protocol = "http",
              headers = mutableListOf(),
              type = null,
              length = 0,
              charset = null,
              body = ToyStream(),
              path = path
            )
        }

    }
}


fun expandHeaders(r: HttpServletRequest): MutableList<Pair<String, String>> {
    val headers = mutableListOf<Pair<String, String>>()
    for (name in r.headerNames.iterator()) {
        for (value in r.getHeaders(name).iterator()) {
            headers.add(Pair(name.toLowerCase(Locale.ENGLISH), value))
        }
    }
    return headers
}



// Lets us mock a Request with a noop input stream
class ToyStream: ServletInputStream() {
    override fun isReady(): Boolean = true
    override fun isFinished(): Boolean = true
    override fun read(): Int = -1
    override fun setReadListener(readListener: ReadListener?) {}
}
