package org.gielinorcore.content

import org.gielinorcore.cache.Cache
import org.gielinorcore.cache.config.ConfigCodec
import org.gielinorcore.cache.config.ItemDefinition
import org.gielinorcore.cache.config.NpcDefinition
import org.gielinorcore.cache.config.ObjectDefinition
import org.gielinorcore.cache.store.MemoryStore
import org.gielinorcore.cache.store.Store

/**
 * Codecs for the revision a pack targets. Supplied by a revision module
 * (DELTA C4); the compiler itself is revision agnostic.
 */
class RevisionCodecs(
    val items: ConfigCodec<ItemDefinition>,
    val npcs: ConfigCodec<NpcDefinition>,
    val objects: ConfigCodec<ObjectDefinition>,
)

/**
 * Applies one or more [ContentPack]s to a base [Cache] and emits a delta
 * [Store] containing only the groups that changed. At runtime the server reads
 * the delta store first and falls back to the base cache ([LayeredStore]).
 *
 * The compile pipeline is the clean-room equivalent of "packing" OSRS content
 * into a 667 cache: the base cache stays untouched on the player's disk, and
 * the repo only ever holds the delta inputs.
 */
class OverlayCompiler(private val base: Cache, private val codecs: RevisionCodecs) {

    fun compile(packs: List<ContentPack>): MemoryStore {
        val delta = MemoryStore()
        val ordered = PackResolver.order(packs)
        for (pack in ordered) {
            for (file in pack.definitionFiles("items")) {
                val id = file.fileName.toString().removeSuffix(".json").toInt()
                val override = ContentPack.json.decodeFromString<ItemOverride>(java.nio.file.Files.readString(file))
                val merged = mergeItem(loadItem(id), override)
                TODO("DELTA N1: encode $merged with codecs.items and write group/file into delta (archive ${codecs.items.archive})")
            }
            // npcs, objects, sequences, models, maps follow the same shape (DELTA N1..N4)
        }
        return delta
    }

    private fun loadItem(id: Int): ItemDefinition = ItemDefinition(id) // DELTA N1: decode from base via codecs.items

    internal fun mergeItem(base: ItemDefinition, o: ItemOverride): ItemDefinition {
        o.name?.let { base.name = it }
        o.modelId?.let { base.modelId = it }
        o.stackable?.let { base.stackable = it }
        o.value?.let { base.value = it }
        o.inventoryOptions?.let { base.inventoryOptions = it.toTypedArray() }
        o.groundOptions?.let { base.groundOptions = it.toTypedArray() }
        o.recolorFrom?.let { base.recolorFrom = it.toIntArray() }
        o.recolorTo?.let { base.recolorTo = it.toIntArray() }
        o.params?.forEach { (k, v) -> base.params[k] = v.toIntOrNull() ?: v }
        return base
    }
}

/** Topologically orders packs by `dependsOn` so later packs override earlier ones. */
object PackResolver {
    fun order(packs: List<ContentPack>): List<ContentPack> {
        val byId = packs.associateBy { it.manifest.id }
        val out = LinkedHashMap<String, ContentPack>()
        val visiting = HashSet<String>()
        fun visit(p: ContentPack) {
            if (p.manifest.id in out) return
            check(visiting.add(p.manifest.id)) { "dependency cycle at ${p.manifest.id}" }
            p.manifest.dependsOn.forEach { dep -> visit(byId[dep] ?: error("${p.manifest.id} depends on missing pack $dep")) }
            visiting.remove(p.manifest.id)
            out[p.manifest.id] = p
        }
        packs.forEach(::visit)
        return out.values.toList()
    }
}

/** Read-through store: delta first, then base. Writes go to the delta. */
class LayeredStore(private val delta: Store, private val base: Store) : Store {
    override fun archives() = (delta.archives() + base.archives()).distinct().sorted()
    override fun read(archive: Int, group: Int) = delta.read(archive, group) ?: base.read(archive, group)
    override fun write(archive: Int, group: Int, data: ByteArray) = delta.write(archive, group, data)
    override fun close() { delta.close(); base.close() }
}
