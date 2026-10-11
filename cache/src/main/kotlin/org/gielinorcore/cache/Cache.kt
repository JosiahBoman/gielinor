package org.gielinorcore.cache

import org.gielinorcore.cache.js5.Container
import org.gielinorcore.cache.js5.GroupFiles
import org.gielinorcore.cache.js5.ReferenceTable
import org.gielinorcore.cache.store.Store

/**
 * Typed view over a [Store]: decodes containers, caches reference tables and
 * splits groups into files. The layer every reader/writer above this uses.
 */
class Cache(val store: Store) : AutoCloseable {

    private val tables = HashMap<Int, ReferenceTable>()

    fun referenceTable(archive: Int): ReferenceTable = tables.getOrPut(archive) {
        val raw = store.read(Store.REFERENCE_ARCHIVE, archive) ?: error("archive $archive has no reference table")
        ReferenceTable.decode(Container.decode(raw).data)
    }

    fun group(archive: Int, group: Int): ByteArray? = store.read(archive, group)?.let { Container.decode(it).data }

    fun files(archive: Int, group: Int): Map<Int, ByteArray> {
        val info = referenceTable(archive).groups[group] ?: error("archive $archive has no group $group")
        val data = group(archive, group) ?: error("archive $archive group $group missing from store")
        return GroupFiles.split(data, info.files.keys.toIntArray())
    }

    fun file(archive: Int, group: Int, file: Int): ByteArray? = files(archive, group)[file]

    override fun close() = store.close()
}
