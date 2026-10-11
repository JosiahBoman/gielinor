package org.gielinorcore.content

import org.gielinorcore.cache.config.ItemDefinition
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContentPackTest {

    private val root: Path = Path.of("../packs/example-pack")

    @Test
    fun `manifest and definition files load`() {
        val pack = ContentPack.load(root)
        assertEquals("example-pack", pack.manifest.id)
        assertEquals(listOf("50000.json"), pack.definitionFiles("items").map { it.fileName.toString() })
    }

    @Test
    fun `item override merges over base`() {
        val pack = ContentPack.load(root)
        val override = ContentPack.json.decodeFromString<ItemOverride>(pack.definitionFiles("items").single().readText())
        val merged = OverlayCompiler(base = FakeCache.empty(), codecs = FakeCache.codecs()).mergeItem(ItemDefinition(50000, name = "null", value = 1), override)
        assertEquals("Reforged blade", merged.name)
        assertEquals(125000, merged.value)
        assertEquals("Wield", merged.inventoryOptions[1])
        assertEquals(false, merged.stackable) // untouched
    }

    @Test
    fun `dependency order and cycles`() {
        fun pack(id: String, vararg deps: String) = ContentPack(root, PackManifest(id, id, "1", "667", deps.toList()))
        val ordered = PackResolver.order(listOf(pack("c", "b"), pack("a"), pack("b", "a")))
        assertEquals(listOf("a", "b", "c"), ordered.map { it.manifest.id })
        assertFailsWith<IllegalStateException> { PackResolver.order(listOf(pack("x", "y"), pack("y", "x"))) }
    }
}
