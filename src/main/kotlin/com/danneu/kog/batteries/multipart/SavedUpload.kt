package com.danneu.kog.batteries.multipart


import java.io.File


class SavedUpload(val file: File, val filename: String, val contentType: String, val length: Long) {
    override fun toString(): String {
        return "[SavedUpload filename=$filename contentType=$contentType length=$length]"
    }
}
