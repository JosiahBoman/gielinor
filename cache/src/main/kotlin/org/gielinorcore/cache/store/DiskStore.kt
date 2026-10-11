package org.gielinorcore.cache.store

import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Sector-based on-disk store: one data file plus one index file per archive.
 *
 * Layout (publicly documented):
 *  - index entry: 6 bytes = u24 length, u24 first sector
 *  - sector: 520 bytes = 8-byte header (u16 group, u16 chunk, u24 next sector, u8 archive) + 512 data
 *  - groups with id > 0xFFFF use a 10-byte header (i32 group) and 510 data bytes
 *
 * Only reads are implemented. DELTA C1 in docs/DELTAS.md covers writes and sector reuse.
 */
class DiskStore private constructor(
    private val data: RandomAccessFile,
    private val indexes: Map<Int, RandomAccessFile>,
) : Store {

    override fun archives(): List<Int> = indexes.keys.filter { it != Store.REFERENCE_ARCHIVE }.sorted()

    override fun read(archive: Int, group: Int): ByteArray? {
        val index = indexes[archive] ?: return null
        val entryOffset = group.toLong() * INDEX_ENTRY_SIZE
        if (entryOffset + INDEX_ENTRY_SIZE > index.length()) return null

        val entry = ByteArray(INDEX_ENTRY_SIZE)
        index.seek(entryOffset)
        index.readFully(entry)
        val length = u24(entry, 0)
        var sector = u24(entry, 3)
        if (length <= 0 || sector <= 0 || sector.toLong() * SECTOR_SIZE > data.length()) return null

        val extended = group > 0xFFFF
        val headerSize = if (extended) EXTENDED_HEADER_SIZE else HEADER_SIZE
        val dataSize = SECTOR_SIZE - headerSize

        val out = ByteArray(length)
        var written = 0
        var chunk = 0
        val sectorBuf = ByteArray(SECTOR_SIZE)
        while (written < length) {
            if (sector == 0) error("unexpected end of sector chain for $archive/$group")
            data.seek(sector.toLong() * SECTOR_SIZE)
            data.readFully(sectorBuf)

            val (sectorGroup, sectorChunk, next, sectorArchive) = if (extended) {
                Quad(i32(sectorBuf, 0), u16(sectorBuf, 4), u24(sectorBuf, 6), sectorBuf[9].toInt() and 0xFF)
            } else {
                Quad(u16(sectorBuf, 0), u16(sectorBuf, 2), u24(sectorBuf, 4), sectorBuf[7].toInt() and 0xFF)
            }
            check(sectorGroup == group && sectorChunk == chunk && sectorArchive == archive) {
                "sector header mismatch reading $archive/$group chunk $chunk (got $sectorArchive/$sectorGroup chunk $sectorChunk)"
            }

            val take = minOf(dataSize, length - written)
            sectorBuf.copyInto(out, written, headerSize, headerSize + take)
            written += take
            chunk++
            sector = next
        }
        return out
    }

    override fun write(archive: Int, group: Int, data: ByteArray) {
        TODO("DELTA C1: sector allocation and index update")
    }

    override fun close() {
        data.close()
        indexes.values.forEach { it.close() }
    }

    private data class Quad(val a: Int, val b: Int, val c: Int, val d: Int)

    companion object {
        const val SECTOR_SIZE = 520
        const val HEADER_SIZE = 8
        const val EXTENDED_HEADER_SIZE = 10
        const val INDEX_ENTRY_SIZE = 6
        const val DATA_FILE = "main_file_cache.dat2"
        const val INDEX_PREFIX = "main_file_cache.idx"

        fun open(directory: Path, writable: Boolean = false): DiskStore {
            val mode = if (writable) "rw" else "r"
            val data = RandomAccessFile(directory.resolve(DATA_FILE).toFile(), mode)
            val indexes = Files.list(directory).use { files ->
                files.filter { it.fileName.toString().startsWith(INDEX_PREFIX) }
                    .toList()
                    .associate { path ->
                        val id = path.fileName.toString().removePrefix(INDEX_PREFIX).toInt()
                        id to RandomAccessFile(path.toFile(), mode)
                    }
            }
            return DiskStore(data, indexes)
        }

        private fun u16(b: ByteArray, o: Int) = ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)
        private fun u24(b: ByteArray, o: Int) = (u16(b, o) shl 8) or (b[o + 2].toInt() and 0xFF)
        private fun i32(b: ByteArray, o: Int) = (u16(b, o) shl 16) or u16(b, o + 2)
    }
}
