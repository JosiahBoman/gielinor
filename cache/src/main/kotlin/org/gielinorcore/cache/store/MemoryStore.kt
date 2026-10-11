package org.gielinorcore.cache.store

/** In-memory store, used for tests and as the output target of the content overlay compiler. */
class MemoryStore : Store {
    private val groups = sortedMapOf<Int, SortedMap<Int, ByteArray>>()

    override fun archives(): List<Int> = groups.keys.filter { it != Store.REFERENCE_ARCHIVE }

    override fun read(archive: Int, group: Int): ByteArray? = groups[archive]?.get(group)?.copyOf()

    override fun write(archive: Int, group: Int, data: ByteArray) {
        groups.getOrPut(archive) { sortedMapOf() }[group] = data.copyOf()
    }

    fun groups(archive: Int): Set<Int> = groups[archive]?.keys ?: emptySet()
}

private typealias SortedMap<K, V> = java.util.SortedMap<K, V>
