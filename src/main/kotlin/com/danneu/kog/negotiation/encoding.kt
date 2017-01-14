package com.danneu.kog.negotiation


import kotlin.comparisons.compareBy

// TODO: https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

// ""
// "q=0.2"
// "q=0.2;a=3.3"
// "q=0"
private object Q {
    val regex = Regex("""q=([0-9]+(?:\.[0-9]+)?)""")

    fun parse(string: String): Double? {
        val match = regex.find(string) ?: return null
        return match.groupValues[1].toDouble()
    }
}

class Encoding(val name: String, val q: Double = 1.0) {
    // Whether the client's specified encoding matches the available encoding name
    fun acceptable(available: String): Boolean {
        // If client mentioned it, then they either accept it or they blacklisted it
        if (this.name == available) {
            return this.q > 0.0
        }

        // If client mentioned wildcard, then they either accept or blacklist the rest
        if (this.name == "*") {
            return this.q > 0.0
        }

        // If we made it this far, then user hasn't blacklisted identity;q=0 nor *;q=0
        if (available == "identity") return true

        return false
    }

    override fun toString(): String {
        return "Encoding(name='$name', q=$q)"
    }

    companion object {
        val simpleEncodingRegex = Regex("""^\s*([^\s;]+)\s*(?:;(.*))?$""")

        // TODO: Test malformed header
        fun parse(header: String): List<Encoding> {
            return header
                .split(",")
                .map(String::trim)
                .map { segment ->
                    val vals = simpleEncodingRegex.find(segment)?.groupValues?.drop(1)!!
                    val name = vals[0]
                    val q = Q.parse(vals[1]) ?: 1.0
                    Encoding(name, q)
                }
        }

        fun prioritize(encodings: List<Encoding>): List<Encoding> {
            return encodings.sortedWith(compareBy(
                { -it.q },
                { if (it.name == "identity") 1 else -1 }
            ))
        }
    }
}


fun List<Encoding>.prioritize(): List<Encoding> {
    return this.sortedWith(compareBy(
        { -it.q },
        { if (it.name == "identity") 1 else -1 }
    ))
}
