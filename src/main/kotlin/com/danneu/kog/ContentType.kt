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
        private val TEXT_HTML = "text/html"
        private val TEXT_PLAIN = "text/plain"
        private val APPLICATION_JSON = "application/json"
        private val APPLICATION_OCTET_STREAM = "application/octet-stream"
        private val APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private val MULTIPART_FORM_DATA = "multipart/form-data"

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


