package org.gielinorcore.cache.js5

import org.gielinorcore.buffer.ReadBuffer
import org.gielinorcore.buffer.WriteBuffer

/**
 * Splits a decompressed group into its files.
 *
 * A single-file group is the raw file. Multi-file groups end with a chunk count byte,
 * preceded by a (chunks x files) table of i32 size deltas, with file data interleaved by chunk.
 */
object GroupFiles {

    fun split(data: ByteArray, fileIds: IntArray): Map<Int, ByteArray> {
        if (fileIds.size == 1) return mapOf(fileIds[0] to data)

        val chunks = data[data.size - 1].toInt() and 0xFF
        val tableSize = chunks * fileIds.size * 4
        val table = ReadBuffer(data, data.size - 1 - tableSize)
        val sizes = IntArray(fileIds.size)
        val chunkSizes = Array(chunks) { IntArray(fileIds.size) }
        for (c in 0 until chunks) {
            var size = 0
            for (f in fileIds.indices) {
                size += table.i32()
                chunkSizes[c][f] = size
                sizes[f] += size
            }
        }

        val outputs = Array(fileIds.size) { ByteArray(sizes[it]) }
        val offsets = IntArray(fileIds.size)
        var pos = 0
        for (c in 0 until chunks) {
            for (f in fileIds.indices) {
                val n = chunkSizes[c][f]
                data.copyInto(outputs[f], offsets[f], pos, pos + n)
                offsets[f] += n
                pos += n
            }
        }
        return fileIds.indices.associate { fileIds[it] to outputs[it] }
    }

    /** Single-chunk inverse of [split]. */
    fun join(files: Map<Int, ByteArray>): ByteArray {
        if (files.size == 1) return files.values.first()
        val out = WriteBuffer()
        var previous = 0
        files.values.forEach { out.bytes(it) }
        files.values.forEach { out.i32(it.size - previous); previous = it.size }
        out.u8(1)
        return out.toByteArray()
    }
}
