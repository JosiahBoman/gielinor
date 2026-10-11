package org.gielinorcore.buffer

import java.nio.charset.Charset

/** The game's text encoding is Windows-1252 with a small set of substitutions in the 0x80..0x9F range. */
object Cp1252 {
    private val charset: Charset = Charset.forName("windows-1252")

    fun decode(bytes: ByteArray, offset: Int, length: Int): String = String(bytes, offset, length, charset)
    fun encode(value: String): ByteArray = value.toByteArray(charset)
}
