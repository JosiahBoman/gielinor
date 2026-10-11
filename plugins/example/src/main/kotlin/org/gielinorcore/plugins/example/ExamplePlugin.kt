package org.gielinorcore.plugins.example

import org.gielinorcore.game.Tile
import org.gielinorcore.script.Plugin
import org.gielinorcore.script.ScriptContext

/** Smallest possible content plugin: a `::home` command and an NPC "Talk-to". */
class ExamplePlugin : Plugin {
    override val id = "example"

    override fun load(ctx: ScriptContext) {
        ctx.onCommand("home") { it.player.tile = Tile(3222, 3218) }
        ctx.onNpcOption(definitionId = 0, option = 1) { /* DELTA S3: dialogue DSL */ }
    }
}
