package org.gielinorcore.cache.store

/**
 * Raw access to compressed groups in a cache, addressed by (archive, group).
 *
 * Archive 255 is reserved for reference tables (one per archive). Everything above
 * this interface works in terms of decoded [org.gielinorcore.cache.js5.Container]s.
 */
interface Store : AutoCloseable {
    /** Archive ids present in this store, excluding the reference-table archive. */
    fun archives(): List<Int>

    /** Compressed bytes of a group, or null if absent. */
    fun read(archive: Int, group: Int): ByteArray?

    /** Replace a group's compressed bytes. */
    fun write(archive: Int, group: Int, data: ByteArray)

    override fun close() {}

    companion object {
        const val REFERENCE_ARCHIVE = 255
    }
}
