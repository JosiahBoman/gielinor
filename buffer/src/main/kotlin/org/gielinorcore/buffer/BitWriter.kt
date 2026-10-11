package org.gielinorcore.buffer

/** Big-endian bit packer used by entity update blocks. */
class BitWriter(private val out: WriteBuffer) {
    private var bitBuffer = 0L
    private var bitCount = 0

    fun bits(count: Int, value: Int): BitWriter {
        require(count in 1..32)
        bitBuffer = (bitBuffer shl count) or (value.toLong() and ((1L shl count) - 1))
        bitCount += count
        while (bitCount >= 8) {
            bitCount -= 8
            out.u8(((bitBuffer shr bitCount) and 0xFF).toInt())
        }
        return this
    }

    fun bit(flag: Boolean): BitWriter = bits(1, if (flag) 1 else 0)

    /** Pad the final partial byte with zero bits and return to byte mode. */
    fun finish(): WriteBuffer {
        if (bitCount > 0) {
            out.u8(((bitBuffer shl (8 - bitCount)) and 0xFF).toInt())
            bitCount = 0
            bitBuffer = 0
        }
        return out
    }
}

/** Big-endian bit reader mirroring [BitWriter]. */
class BitReader(private val input: ReadBuffer) {
    private var bitBuffer = 0L
    private var bitCount = 0

    fun bits(count: Int): Int {
        require(count in 1..32)
        while (bitCount < count) {
            bitBuffer = (bitBuffer shl 8) or input.u8().toLong()
            bitCount += 8
        }
        bitCount -= count
        return ((bitBuffer shr bitCount) and ((1L shl count) - 1)).toInt()
    }

    fun bit(): Boolean = bits(1) == 1

    fun finish(): ReadBuffer {
        bitCount = 0
        bitBuffer = 0
        return input
    }
}
