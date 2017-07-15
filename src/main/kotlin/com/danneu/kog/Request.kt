package com.danneu.kog

import com.danneu.kog.Protocol.Http_1_1
import com.danneu.kog.batteries.multipart.SavedUpload
import com.danneu.kog.cookies.parse
import com.danneu.result.Result
import com.danneu.json.Decoder as JsonDecoder
import com.danneu.kog.form.Decoder as FormDecoder
import com.danneu.kog.negotiation.Negotiator
import com.danneu.kog.util.Util
import com.danneu.kog.util.mutableCopy
import org.eclipse.jetty.websocket.api.util.QuoteUtil
import javax.servlet.ServletInputStream

class Request(
  val serverPort: Int,
  val serverName: String,
  val remoteAddr: String,
  val href: String,
  var queryString: String?,
  val scheme: String,
  var method: Method,
  val protocol: Protocol,
  override val headers: MutableList<HeaderPair>,
  override var contentType: ContentType?,
  val length: Int?, // Either >= 0 or null
  val body: ServletInputStream, // Note: Could just be InputStream if I'm never going to use ServerInputStream methods
  var path: String
) : HasHeaders<Request>(), HasContentType {
    override fun toType() = this

    val query by lazy {
        Util.formDecode(queryString).mutableCopy()
    }

    val params by lazy {
        mutableMapOf<String, Any>()
    }

    // multipart middleware populates this with name -> file mappings for multipart uploads
    val uploads by lazy {
        mutableMapOf<String, SavedUpload>()
    }

    val negotiate by lazy {
        Negotiator(this)
    }

    val cookies by lazy {
        parse(getHeader(Header.Cookie)).mutableCopy()
    }

    // TODO: Avoid consuming body that's already been drained
    fun <T> json(decoder: JsonDecoder<T>): Result<T, String> {
        return JsonDecoder.decode(utf8, decoder)
    }

    // TODO: Avoid consuming body that's already been drained
    fun <T> form(decoder: FormDecoder<T>): Result<T, String> {
        return FormDecoder.decode(utf8, decoder)
    }

    // TODO: Avoid consuming body that's already been drained
    val utf8: String by lazy {
        body.readBytes().toString(Charsets.UTF_8)
    }

    /**
     * Is request a websocket upgrade request?
     *
     * Based on org.eclipse.jetty.websocket.server.WebSocketServerFactory#isUpgradeRequest()
     */
    fun isUpgrade(): Boolean {
        // Tests sorted by least common to most common in effort to fail asap
        // Check for Upgrade: websocket
        (this.getHeader(Header.Upgrade) ?: return false).apply {
            if (this.toLowerCase() != "websocket") return false
        }
        // Check for Connection: [... ,] upgrade [, ...]
        (this.getHeader(Header.Connection) ?: return false).apply {
            QuoteUtil.splitAt(this, ",").asSequence().find { it.trim().toLowerCase() == "upgrade" } ?: return false
        }
        // Check method
        if (this.method != Method.Get) return false
        // Check protocol
        if (this.protocol != Http_1_1) return false
        return true
    }

    override fun toString(): String {
        return listOf(
          "serverPort" to serverPort,
          "serverName" to serverName,
          "remoteAddr" to remoteAddr,
          "href" to href,
          "queryString" to queryString,
          "scheme" to scheme,
          "method" to method.toString(),
          "protocol" to protocol,
          "headers" to headers,
          "contentType" to contentType,
          "length" to length,
          "path" to path
        ).map { it.toString() }.joinToString("\n")
    }

    companion object
}


