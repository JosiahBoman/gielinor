package org.gielinorcore.tools

import org.gielinorcore.cache.Cache
import org.gielinorcore.cache.store.DiskStore
import org.gielinorcore.content.ContentPack
import org.gielinorcore.content.PackResolver
import java.nio.file.Path

private const val USAGE = """usage:
  tools inspect <cacheDir>                 list archives, group counts and reference-table versions
  tools pack <cacheDir> <outDir> <pack>... compile content packs into a delta store (DELTA N1)
  tools transcode <from> <to> <in> <out>   convert a model between revisions (DELTA T1)
  tools verify <pack>...                   validate pack manifests and id ranges"""

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "inspect" -> inspect(Path.of(args[1]))
        "verify" -> verify(args.drop(1).map(Path::of))
        "pack", "transcode" -> { System.err.println("${args[0]}: not implemented yet, see docs/DELTAS.md"); System.exit(2) }
        else -> { System.err.println(USAGE); System.exit(2) }
    }
}

private fun inspect(dir: Path) {
    Cache(DiskStore.open(dir)).use { cache ->
        for (archive in cache.store.archives()) {
            val table = runCatching { cache.referenceTable(archive) }.getOrNull()
            println("archive %3d  groups=%-6s version=%s protocol=%s".format(
                archive, table?.groups?.size ?: "?", table?.version ?: "?", table?.protocol ?: "?"))
        }
    }
}

private fun verify(paths: List<Path>) {
    val packs = paths.map(ContentPack::load)
    val ordered = PackResolver.order(packs)
    ordered.forEach { p ->
        val m = p.manifest
        println("${m.id} ${m.version} -> ${m.targetRevision} ids ${m.idRangeStart}..${m.idRangeEnd} items=${p.definitionFiles("items").size}")
    }
    val overlaps = ordered.flatMap { a -> ordered.filter { b -> a !== b && a.manifest.idRangeStart <= b.manifest.idRangeEnd && b.manifest.idRangeStart <= a.manifest.idRangeEnd }.map { a.manifest.id to it.manifest.id } }
    if (overlaps.isNotEmpty()) { System.err.println("id range overlap: $overlaps"); System.exit(1) }
}
