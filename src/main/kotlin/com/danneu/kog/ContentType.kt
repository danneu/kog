package com.danneu.kog

import com.danneu.kog.mime.database
import com.danneu.kog.util.Util
import java.nio.charset.Charset

/** A content-type holds a mime-type and possibly
 *  a charset and additional parameters.
 *
 */
data class ContentType(val mime: Mime, var charset: Charset? = null, val params: MutableMap<String, String> = mutableMapOf()) {
    init {
        // try to take from params
        if (charset == null) {
            params["charset"]?.also { str ->
                Util.charsetOrNull(str)?.let { charset ->
                    this.charset = charset
                }
            }
        }

        // then try to take from database
        if (charset == null) {
            database.getCharset(mime)?.also { charset ->
                this.charset = charset
            }
        }
    }

    override fun toString(): String {
        val builder = StringBuilder(mime.toString())

        if (charset != null) {
            // Ensure params has no charset
            params.remove("charset")
            builder.append("; charset=${charset.toString().toLowerCase()}")
        }

        // Append params
        params.keys.sorted().forEach { key ->
            if (!TOKEN_REGEXP.matches(key)) {
                return@forEach
            }

            // skip invalid values
            qstring(params[key]!!)?.let { quotedValue ->
                builder.append("; $key=$quotedValue")
            }
        }

        return builder.toString()
    }

    companion object {
        fun parse(string: String): ContentType? {
            val index = string.indexOf(';')
            val media = if (index == -1) {
                string
            } else {
                string.substring(0, index)
            }.trim().toLowerCase()

            if (!TYPE_REGEXP.matches(media)) {
                // Invalid type
                return null
            }

            val params = mutableMapOf<String, String>()
            var charset: Charset? = null

            PARAM_REGEXP.findAll(string, startIndex = maxOf(index, 0)).forEach { result ->
                val (k, v) = result.destructured

                // Scoop charset from params if exists
                if (k == "charset") {
                    charset = Util.charsetOrNull(v)
                    return@forEach
                }

                params.put(
                    k.toLowerCase(),
                    v.run {
                        // Remove quotes and escapes
                        if (startsWith('"')) {
                            this.substring(1, v.length - 1).replace(QESC_REGEXP, "$1")
                        } else {
                            this
                        }
                    }.toLowerCase()
                )
            }

            return ContentType(Mime.fromString(media), charset, params)
        }

    }
}

private val PARAM_REGEXP = Regex("""; *([!#$%&'*+.^_`|~0-9A-Za-z-]+) *= *("(?:[\u000b\u0020\u0021\u0023-\u005b\u005d-\u007e\u0080-\u00ff]|\\[\u000b\u0020-\u00ff])*"|[!#$%&'*+.^_`|~0-9A-Za-z-]+) *""")
private val TEXT_REGEXP = Regex("""^[\u000b\u0020-\u007e\u0080-\u00ff]+$""")
private val TOKEN_REGEXP = Regex("""^[!#$%&'*+.^_`|~0-9A-Za-z-]+$""")
private val QESC_REGEXP = Regex("""\\([\u000b\u0020-\u00ff])""")
private val QUOTE_REGEXP = Regex("""([\\"])""")
private val TYPE_REGEXP = Regex("""^[!#$%&'*+.^_`|~0-9A-Za-z-]+\/[!#$%&'*+.^_`|~0-9A-Za-z-]+$""")

private fun qstring(str: String): String? {
    // No need to quote tokens
    if (TOKEN_REGEXP.matches(str)) {
        return str
    }

    if (str.length > 0 && !TEXT_REGEXP.matches(str)) {
        // invalid param value
        return null
    }

    return "\"${str.replace(QUOTE_REGEXP, """\\$1""")}\""
}
