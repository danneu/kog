package com.danneu.kog.negotiation

// ""
// "q=0.2"
// "q=0.2;a=3.3"
// "q=0"
internal object QValue {
    val regex = Regex("""q=([0-9]+(?:\.[0-9]+)?)""")

    fun parse(string: String): Double? {
        val match = regex.find(string) ?: return null
        return match.groupValues[1].toDouble()
    }
}


