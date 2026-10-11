package org.gielinorcore.buffer

/** Growable byte sink mirroring [ReadBuffer]. */
class WriteBuffer(initialCapacity: Int = 64) {

    private var array = ByteArray(initialCapacity)
    var position: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (position + extra > array.size) {
            array = array.copyOf(maxOf(array.size * 2, position + extra))
        }
    }

    fun u8(value: Int): WriteBuffer { ensure(1); array[position++] = value.toByte(); return this }
    fun u16(value: Int): WriteBuffer = u8(value shr 8).u8(value)
    fun u24(value: Int): WriteBuffer = u8(value shr 16).u8(value shr 8).u8(value)
    fun i32(value: Int): WriteBuffer = u8(value shr 24).u8(value shr 16).u8(value shr 8).u8(value)
    fun i64(value: Long): WriteBuffer = i32((value shr 32).toInt()).i32(value.toInt())

    fun smart(value: Int): WriteBuffer {
        require(value in 0..0x7FFF) { "smart out of range: $value" }
        return if (value < 128) u8(value) else u16(value + 0x8000)
    }

    fun signedSmart(value: Int): WriteBuffer {
        require(value in -0x4000..0x3FFF) { "signed smart out of range: $value" }
        return if (value in -64..63) u8(value + 64) else u16(value + 0xC000)
    }

    fun bigSmart(value: Int): WriteBuffer {
        require(value >= 0)
        return if (value < 0x8000) u16(value) else i32(value or Int.MIN_VALUE)
    }

    fun bytes(value: ByteArray, offset: Int = 0, length: Int = value.size - offset): WriteBuffer {
        ensure(length)
        value.copyInto(array, position, offset, offset + length)
        position += length
        return this
    }

    fun string(value: String, terminator: Int = 0): WriteBuffer = bytes(Cp1252.encode(value)).u8(terminator)
    fun versionedString(value: String): WriteBuffer = u8(0).string(value)

    fun toByteArray(): ByteArray = array.copyOf(position)
}
