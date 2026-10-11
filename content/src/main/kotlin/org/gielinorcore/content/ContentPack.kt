package org.gielinorcore.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A content pack is a directory of *deltas* over a base cache: definition
 * overrides, new models/animations, new map chunks. Packs never contain base
 * game data, only additions and overrides, and they are compiled onto the
 * player's own cache at build time (see [OverlayCompiler]).
 *
 * Layout:
 *   pack.json
 *   definitions/items/<id>.json      (partial ItemDefinition, merged over base)
 *   definitions/npcs/<id>.json
 *   definitions/objects/<id>.json
 *   definitions/sequences/<id>.json
 *   models/<id>.obj | .gcm           (your own meshes; DELTA N2 imports OBJ)
 *   animations/<id>.json
 *   maps/<regionX>_<regionY>.json
 */
@Serializable
data class PackManifest(
    val id: String,
    val name: String,
    val version: String,
    /** Revision family whose config opcodes the pack targets, e.g. "667", "718", "osrs". */
    val targetRevision: String,
    val dependsOn: List<String> = emptyList(),
    /** First id this pack may allocate when it creates new definitions; avoids collisions between packs. */
    val idRangeStart: Int = 50_000,
    val idRangeEnd: Int = 59_999,
)

data class ContentPack(val root: Path, val manifest: PackManifest) {

    fun definitionFiles(kind: String): List<Path> {
        val dir = root.resolve("definitions").resolve(kind)
        if (!dir.exists() || !dir.isDirectory()) return emptyList()
        return Files.list(dir).use { s -> s.filter { it.name.endsWith(".json") }.sorted().toList() }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        fun load(root: Path): ContentPack {
            val manifest = json.decodeFromString<PackManifest>(root.resolve("pack.json").readText())
            require(manifest.idRangeStart <= manifest.idRangeEnd) { "pack ${manifest.id} has an inverted id range" }
            return ContentPack(root, manifest)
        }
    }
}
