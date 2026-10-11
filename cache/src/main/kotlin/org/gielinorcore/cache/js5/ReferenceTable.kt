package org.gielinorcore.cache.js5

import org.gielinorcore.buffer.ReadBuffer

/**
 * Metadata for one archive, stored in archive 255 group N for archive N.
 *
 * Protocol 5/6/7 layouts are publicly documented; protocol 7 switches ids to
 * [ReadBuffer.bigSmart]. Encoding back to bytes is DELTA C3.
 */
data class ReferenceTable(
    val protocol: Int,
    val version: Int,
    val flags: Int,
    val groups: Map<Int, GroupInfo>,
) {
    data class GroupInfo(
        val id: Int,
        val nameHash: Int,
        val crc: Int,
        val version: Int,
        val files: Map<Int, FileInfo>,
        val whirlpool: ByteArray? = null,
        val compressedLength: Int? = null,
        val uncompressedLength: Int? = null,
    )

    data class FileInfo(val id: Int, val nameHash: Int)

    val hasNames get() = flags and FLAG_NAMES != 0

    companion object {
        const val FLAG_NAMES = 0x1
        const val FLAG_WHIRLPOOL = 0x2
        const val FLAG_LENGTHS = 0x4
        const val FLAG_UNCOMPRESSED_CRC = 0x8

        fun decode(bytes: ByteArray): ReferenceTable {
            val r = ReadBuffer(bytes)
            val protocol = r.u8()
            require(protocol in 5..7) { "unsupported reference table protocol $protocol" }
            val version = if (protocol >= 6) r.i32() else 0
            val flags = r.u8()
            val readId: () -> Int = if (protocol >= 7) r::bigSmart else r::u16

            val count = readId()
            val ids = IntArray(count)
            var last = 0
            for (i in 0 until count) { last += readId(); ids[i] = last }

            val names = if (flags and FLAG_NAMES != 0) IntArray(count) { r.i32() } else null
            val crcs = IntArray(count) { r.i32() }
            if (flags and FLAG_UNCOMPRESSED_CRC != 0) repeat(count) { r.i32() }
            val whirlpools = if (flags and FLAG_WHIRLPOOL != 0) Array(count) { r.bytes(64) } else null
            val lengths = if (flags and FLAG_LENGTHS != 0) Array(count) { r.i32() to r.i32() } else null
            val versions = IntArray(count) { r.i32() }
            val fileCounts = IntArray(count) { readId() }

            val fileIds = Array(count) { g ->
                var fl = 0
                IntArray(fileCounts[g]) { fl += readId(); fl }
            }
            val fileNames = if (flags and FLAG_NAMES != 0) Array(count) { g -> IntArray(fileCounts[g]) { r.i32() } } else null

            val groups = LinkedHashMap<Int, GroupInfo>(count)
            for (g in 0 until count) {
                val files = LinkedHashMap<Int, FileInfo>(fileCounts[g])
                for (f in 0 until fileCounts[g]) {
                    val fid = fileIds[g][f]
                    files[fid] = FileInfo(fid, fileNames?.get(g)?.get(f) ?: -1)
                }
                groups[ids[g]] = GroupInfo(
                    id = ids[g],
                    nameHash = names?.get(g) ?: -1,
                    crc = crcs[g],
                    version = versions[g],
                    files = files,
                    whirlpool = whirlpools?.get(g),
                    compressedLength = lengths?.get(g)?.first,
                    uncompressedLength = lengths?.get(g)?.second,
                )
            }
            return ReferenceTable(protocol, version, flags, groups)
        }
    }
}
