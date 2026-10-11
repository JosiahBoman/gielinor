package org.gielinorcore.game

import kotlin.test.Test
import kotlin.test.assertEquals

class WorldTest {
    @Test
    fun `queue drains on tick and events fire`() {
        val world = World()
        var logins = 0
        world.events.subscribe<PlayerLoggedIn> { logins++ }
        world.queue.post { world.spawnPlayer("josiah") }
        assertEquals(0, logins)
        world.tick()
        assertEquals(1, logins)
        assertEquals(1L, world.tickCount)
        assertEquals(12850, world.players.single().tile.regionId)
    }
}
