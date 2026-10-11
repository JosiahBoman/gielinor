package org.gielinorcore.buffer

/**
 * Cursor over a byte array with the integer encodings the game data formats use.
 *
 * Encodings are described from publicly documented format specifications, not
 * from any client or server source. Keep it that way (see docs/CLEAN_ROOM.md).
 */
class ReadBuffer(val array: ByteArray, var position: Int = 0, val limit: Int = array.size) {

    val remaining: Int get() = limit - position
    val hasRemaining: Boolean get() = remaining > 0

    fun u8(): Int {
        require(position < limit) { "read past end at $position" }
        return array[position++].toInt() and 0xFF
    }

    fun i8(): Int = array[position++].toInt()

    fun u16(): Int = (u8() shl 8) or u8()
    fun i16(): Int = u16().toShort().toInt()
    fun u24(): Int = (u8() shl 16) or (u8() shl 8) or u8()
    fun i32(): Int = (u8() shl 24) or (u8() shl 16) or (u8() shl 8) or u8()
    fun i64(): Long = (i32().toLong() shl 32) or (i32().toLong() and 0xFFFFFFFFL)

    /** One byte if < 128, otherwise two bytes with the high bit set. */
    fun smart(): Int {
        val peek = array[position].toInt() and 0xFF
        return if (peek < 128) u8() else u16() - 0x8000
    }

    /** Signed variant of [smart] centred on zero. */
    fun signedSmart(): Int {
        val peek = array[position].toInt() and 0xFF
        return if (peek < 128) u8() - 64 else u16() - 0xC000
    }

    /** Two bytes if the high bit is clear, otherwise four bytes with the high bit set. */
    fun bigSmart(): Int {
        val peek = array[position].toInt()
        return if (peek >= 0) u16() else i32() and 0x7FFFFFFF
    }

    /** Sequence of [smart] values summed until one is 0x7FFF (used by some config lists). */
    fun smartSum(): Int {
        var total = 0
        var value = smart()
        while (value == 0x7FFF) {
            total += 0x7FFF
            value = smart()
        }
        return total + value
    }

    fun bytes(length: Int): ByteArray {
        require(length in 0..remaining) { "cannot read $length bytes, $remaining remaining" }
        val out = array.copyOfRange(position, position + length)
        position += length
        return out
    }

    fun skip(length: Int) {
        require(length in 0..remaining)
        position += length
    }

    /** String terminated by [terminator] (0 for newer revisions, 10 for older ones). */
    fun string(terminator: Int = 0): String {
        val start = position
        while (u8() != terminator) { /* advance */ }
        return Cp1252.decode(array, start, position - 1 - start)
    }

    /** Same as [string] but preceded by a version byte that must be 0. */
    fun versionedString(): String {
        val version = u8()
        require(version == 0) { "unsupported string version $version" }
        return string()
    }

    fun slice(length: Int): ReadBuffer {
        require(length in 0..remaining)
        val b = ReadBuffer(array, position, position + length)
        position += length
        return b
    }
}
