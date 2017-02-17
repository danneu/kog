package com.danneu.kog

// TODO: This is only stubbed out. Right now, I'm just adding to it on an as-needed basis

sealed class ContentType(private val string: String) {
    override fun toString() = string

    // TODO: Impl equality
    class Other(value: String) : ContentType(value.toLowerCase())
    object Html : ContentType(TEXT_HTML)
    object Text : ContentType(TEXT_PLAIN)
    object Json : ContentType(APPLICATION_JSON)
    object OctetStream : ContentType(APPLICATION_OCTET_STREAM)
    object UrlEncodedForm : ContentType(APPLICATION_X_WWW_FORM_URLENCODED)
    object MultipartForm : ContentType(MULTIPART_FORM_DATA)

    companion object {
        private const val TEXT_HTML = "text/html"
        private const val TEXT_PLAIN = "text/plain"
        private const val APPLICATION_JSON = "application/json"
        private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
        private const val APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private const val MULTIPART_FORM_DATA = "multipart/form-data"

        // FIXME: Assumes charset is not attached
        fun fromString(string: String): ContentType {
            @Suppress("NAME_SHADOWING")
            val string = string.toLowerCase()

            return when (string) {
                TEXT_HTML -> Html
                TEXT_PLAIN -> Text
                APPLICATION_JSON -> Json
                APPLICATION_OCTET_STREAM -> OctetStream
                APPLICATION_X_WWW_FORM_URLENCODED -> UrlEncodedForm
                MULTIPART_FORM_DATA -> MultipartForm
                else -> Other(string)
            }
        }
    }
}


