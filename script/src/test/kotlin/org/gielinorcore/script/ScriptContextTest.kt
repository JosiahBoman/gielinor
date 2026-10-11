package org.gielinorcore.script

import org.gielinorcore.game.CommandEvent
import org.gielinorcore.game.World
import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptContextTest {
    @Test
    fun `command handlers filter by name`() {
        val world = World()
        val ctx = ScriptContext(world, "test")
        val seen = ArrayList<String>()
        ctx.onCommand("home") { seen += it.args.joinToString() }
        val p = world.spawnPlayer("a")
        world.events.publish(CommandEvent(p, "HOME", listOf("1")))
        world.events.publish(CommandEvent(p, "other", emptyList()))
        assertEquals(listOf("1"), seen)
    }
}
