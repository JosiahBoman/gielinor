package org.gielinorcore.cache

import org.gielinorcore.buffer.WriteBuffer
import org.gielinorcore.cache.js5.Compression
import org.gielinorcore.cache.js5.Container
import org.gielinorcore.cache.js5.GroupFiles
import org.gielinorcore.cache.js5.ReferenceTable
import org.gielinorcore.cache.store.DiskStore
import org.gielinorcore.cache.store.MemoryStore
import org.gielinorcore.cache.store.Store
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CacheTest {

    @Test
    fun `container round trips through gzip and none`() {
        val payload = ByteArray(5000) { (it * 7).toByte() }
        for (c in listOf(Compression.NONE, Compression.GZIP)) {
            val decoded = Container.decode(Container(payload, c, version = 42).encode())
            assertContentEquals(payload, decoded.data)
            assertEquals(42, decoded.version)
        }
    }

    @Test
    fun `multi-file group splits and joins`() {
        val files = mapOf(0 to byteArrayOf(1, 2, 3), 1 to byteArrayOf(), 2 to byteArrayOf(9, 9))
        val split = GroupFiles.split(GroupFiles.join(files), intArrayOf(0, 1, 2))
        files.forEach { (id, bytes) -> assertContentEquals(bytes, split[id]) }
    }

    @Test
    fun `reference table protocol 6 decodes`() {
        // Two groups (ids 3 and 7); group 7 has two files.
        val raw = WriteBuffer()
            .u8(6).i32(99).u8(0)
            .u16(2).u16(3).u16(4)          // count, delta ids 3, 7
            .i32(0x11).i32(0x22)           // crcs
            .i32(1).i32(2)                 // versions
            .u16(1).u16(2)                 // file counts
            .u16(0).u16(0).u16(5)          // file delta ids: [0], [0,5]
            .toByteArray()
        val table = ReferenceTable.decode(raw)
        assertEquals(99, table.version)
        assertEquals(setOf(3, 7), table.groups.keys)
        assertEquals(setOf(0, 5), table.groups[7]!!.files.keys)
        assertEquals(0x22, table.groups[7]!!.crc)
    }

    @Test
    fun `disk store reads a multi-sector group`() {
        val dir = Files.createTempDirectory("gielinorcore-store")
        val archive = 2
        val group = 1
        val payload = ByteArray(1300) { it.toByte() } // spans 3 sectors
        val dat = WriteBuffer(DiskStore.SECTOR_SIZE * 4)
        dat.bytes(ByteArray(DiskStore.SECTOR_SIZE)) // sector 0 is never used
        var written = 0
        var chunk = 0
        var sector = 1
        while (written < payload.size) {
            val take = minOf(512, payload.size - written)
            val next = if (written + take < payload.size) sector + 1 else 0
            dat.u16(group).u16(chunk).u24(next).u8(archive)
            dat.bytes(payload, written, take).bytes(ByteArray(512 - take))
            written += take; chunk++; sector++
        }
        Files.write(dir.resolve(DiskStore.DATA_FILE), dat.toByteArray())
        val idx = WriteBuffer().bytes(ByteArray(6)).u24(payload.size).u24(1).toByteArray()
        Files.write(dir.resolve(DiskStore.INDEX_PREFIX + archive), idx)
        Files.write(dir.resolve(DiskStore.INDEX_PREFIX + Store.REFERENCE_ARCHIVE), ByteArray(0))

        DiskStore.open(dir).use { store ->
            assertEquals(listOf(archive), store.archives())
            assertContentEquals(payload, store.read(archive, group))
            assertEquals(null, store.read(archive, 0))
        }
    }

    @Test
    fun `cache resolves files through reference table`() {
        val store = MemoryStore()
        val table = WriteBuffer().u8(6).i32(1).u8(0).u16(1).u16(0).i32(0).i32(0).u16(2).u16(0).u16(1).toByteArray()
        store.write(Store.REFERENCE_ARCHIVE, 19, Container(table, Compression.NONE).encode())
        val group = GroupFiles.join(mapOf(0 to byteArrayOf(1), 1 to byteArrayOf(2, 3)))
        store.write(19, 0, Container(group).encode())

        Cache(store).use { cache ->
            assertContentEquals(byteArrayOf(2, 3), cache.file(19, 0, 1))
        }
    }
}
