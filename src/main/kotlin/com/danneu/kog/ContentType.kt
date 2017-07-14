package com.danneu.kog

// TODO: Test the content-type parser.

// Experimental. Extended on an as-needed basis for now.
//
// TODO: Reuse/unify with other mime-related stuff around kog
//
sealed class Mime (protected val string: String) {
    override fun toString() = string

    /** Get mime namespace.E.g. check if it's one of the "image" mime types.
     *
     * E.g. check if it's one of the "image" mime types.
     *
     *     contentType.prefix == "image"
     */
    val prefix = string.split('/', limit = 2).first()

    // Note: Mime.Raw("video/mp4") == Mime.VideoMp4
    class Raw(value: String) : Mime(value.toLowerCase()) {
        override fun equals(other: Any?) = other is Mime && string == other.string
        override fun hashCode() = string.hashCode()
    }

    // Common
    object Html : Mime(TEXT_HTML)
    object Text : Mime(TEXT_PLAIN)
    object Json : Mime(APPLICATION_JSON)
    object OctetStream : Mime(APPLICATION_OCTET_STREAM)
    object FormUrlEncoded : Mime(APPLICATION_X_WWW_FORM_URLENCODED)
    object FormMultipart : Mime(MULTIPART_FORM_DATA)
    // Images
    object ImageGif : Mime(IMAGE_GIF)
    object ImageJpeg : Mime(IMAGE_JPEG)
    object ImagePng : Mime(IMAGE_PNG)
    object ImageWebp : Mime(IMAGE_WEBP)
    // Video
    object VideoMp4 : Mime(VIDEO_MP4)
    object VideoMpeg : Mime(VIDEO_MPEG)
    object VideoOgg : Mime(VIDEO_OGG)
    object VideoWebm : Mime(VIDEO_WEBM)
    // Audio
    object AudioMp3 : Mime(AUDIO_MP3)
    object AudioMp4 : Mime(AUDIO_MP4)
    object AudioMpeg : Mime(AUDIO_MPEG)
    object AudioOgg : Mime(AUDIO_OGG)
    object AudioWav : Mime(AUDIO_WAV)
    object AudioWebm : Mime(AUDIO_WEBM)

    companion object {
        private const val TEXT_HTML = "text/html"
        private const val TEXT_PLAIN = "text/plain"
        private const val APPLICATION_JSON = "application/json"
        private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
        private const val APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private const val MULTIPART_FORM_DATA = "multipart/form-data"
        private const val IMAGE_GIF = "image/gif"
        private const val IMAGE_JPEG = "image/jpeg"
        private const val IMAGE_PNG = "image/png"
        private const val IMAGE_WEBP = "image/webp"
        private const val VIDEO_MP4 = "video/mp4"
        private const val VIDEO_MPEG = "video/mpeg"
        private const val VIDEO_OGG = "video/ogg"
        private const val VIDEO_WEBM = "video/webm"
        private const val AUDIO_MP3 = "audio/mp3"
        private const val AUDIO_MP4 = "audio/mp4"
        private const val AUDIO_MPEG = "audio/mpeg"
        private const val AUDIO_OGG = "audio/ogg"
        private const val AUDIO_WAV = "audio/wav"
        private const val AUDIO_WEBM = "audio/webm"

        fun fromString(string: String): Mime {
            return when (string.toLowerCase()) {
                TEXT_HTML -> Html
                TEXT_PLAIN -> Text
                APPLICATION_JSON -> Json
                APPLICATION_OCTET_STREAM -> OctetStream
                APPLICATION_X_WWW_FORM_URLENCODED -> FormUrlEncoded
                MULTIPART_FORM_DATA -> FormMultipart
                IMAGE_GIF -> ImageGif
                IMAGE_JPEG -> ImageJpeg
                IMAGE_PNG -> ImagePng
                IMAGE_WEBP -> ImageWebp
                VIDEO_MP4 -> VideoMp4
                VIDEO_MPEG -> VideoMpeg
                VIDEO_OGG -> VideoOgg
                VIDEO_WEBM -> VideoWebm
                AUDIO_MP3 -> AudioMp3
                AUDIO_MP4 -> AudioMp4
                AUDIO_MPEG -> AudioMpeg
                AUDIO_OGG -> AudioOgg
                AUDIO_WAV -> AudioWav
                AUDIO_WEBM -> AudioWebm
                else -> Raw(string)
            }
        }
    }
}

/** A content-type is a mime-type with additional parameters like charset.
 *
 */
data class ContentType(val mime: Mime, val params: MutableMap<String, String> = mutableMapOf()) {
    val charset = params["charset"]

    override fun toString(): String {
        val builder = StringBuilder(mime.toString())

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

            PARAM_REGEXP.findAll(string, startIndex = maxOf(index, 0)).forEach { result ->
                val (k, v) = result.destructured

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

            return ContentType(Mime.fromString(media), params)
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
