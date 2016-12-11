package com.danneu.kog.cookies


import com.danneu.kog.urlDecode


fun parse(header: String?): Map<String, String> {
    if (header == null) { return emptyMap() }
    val pairs = header.split(";").map(String::trim).filter(String::isNotEmpty).map(::parsePair)
    return pairs.filterNotNull().filter { (_, v) -> v.isNotEmpty() }.toMap()
}


private fun parsePair(str: String): Pair<String, String>? {
    val pair = str.split("=", limit = 2)
    if (pair.size != 2) return null
    return pair[0].trim() to urlDecode(unwrapQuotes(pair[1].trim()))
}


private fun unwrapQuotes(string: String): String {
    return string.replace(Regex("""(^"|"$)"""), "")
}
