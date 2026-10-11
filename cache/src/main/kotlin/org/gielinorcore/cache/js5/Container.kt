package org.gielinorcore.cache.js5

import org.gielinorcore.buffer.ReadBuffer
import org.gielinorcore.buffer.WriteBuffer
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class Compression(val id: Int) {
    NONE(0), BZIP2(1), GZIP(2), LZMA(3);

    companion object {
        fun of(id: Int) = entries.firstOrNull { it.id == id } ?: error("unknown compression $id")
    }
}

/**
 * A group as stored on disk: compression byte, compressed length, optional
 * uncompressed length, payload, optional trailing u16 version.
 *
 * Encrypted containers (XTEA over the payload) are DELTA C2.
 */
data class Container(val data: ByteArray, val compression: Compression = Compression.GZIP, val version: Int? = null) {

    fun encode(): ByteArray {
        val out = WriteBuffer(data.size + 16)
        out.u8(compression.id)
        when (compression) {
            Compression.NONE -> out.i32(data.size).bytes(data)
            Compression.GZIP -> {
                val compressed = ByteArrayOutputStream().also { bos -> GZIPOutputStream(bos).use { it.write(data) } }.toByteArray()
                out.i32(compressed.size).i32(data.size).bytes(compressed)
            }
            Compression.BZIP2, Compression.LZMA -> TODO("DELTA C2: ${compression.name} encoding")
        }
        version?.let { out.u16(it) }
        return out.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): Container {
            val r = ReadBuffer(bytes)
            val compression = Compression.of(r.u8())
            val length = r.i32()
            require(length >= 0 && length <= r.remaining) { "bad container length $length" }
            val data: ByteArray
            when (compression) {
                Compression.NONE -> data = r.bytes(length)
                Compression.GZIP -> {
                    val uncompressedLength = r.i32()
                    val payload = r.bytes(length)
                    data = GZIPInputStream(payload.inputStream()).use { it.readBytes() }
                    require(data.size == uncompressedLength) { "gzip length mismatch" }
                }
                Compression.BZIP2, Compression.LZMA -> TODO("DELTA C2: ${compression.name} decoding")
            }
            val version = if (r.remaining >= 2) r.u16() else null
            return Container(data, compression, version)
        }
    }

    override fun equals(other: Any?) = other is Container && data.contentEquals(other.data) && compression == other.compression && version == other.version
    override fun hashCode() = data.contentHashCode() * 31 + compression.hashCode()
}
