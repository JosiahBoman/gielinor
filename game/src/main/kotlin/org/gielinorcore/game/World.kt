package org.gielinorcore.game

import java.util.concurrent.atomic.AtomicInteger

data class Tile(val x: Int, val y: Int, val plane: Int = 0) {
    val regionId get() = ((x shr 6) shl 8) or (y shr 6)
    fun translate(dx: Int, dy: Int, dz: Int = 0) = Tile(x + dx, y + dy, plane + dz)
}

abstract class Entity(val index: Int) {
    var tile: Tile = Tile(3222, 3218)
    open fun tick() {}
}

class Player(index: Int, val username: String) : Entity(index)

class Npc(index: Int, val definitionId: Int) : Entity(index)

/**
 * Single-threaded tick loop. All game state mutation happens on the game thread
 * in [tick]; networking hands messages over through [GameQueue].
 */
class World(val tickMillis: Long = 600) {
    val players = ArrayList<Player>()
    val npcs = ArrayList<Npc>()
    val events = EventBus()
    val queue = GameQueue()
    private val nextIndex = AtomicInteger(1)
    var tickCount = 0L
        private set

    fun spawnPlayer(username: String): Player = Player(nextIndex.getAndIncrement(), username).also {
        players += it
        events.publish(PlayerLoggedIn(it))
    }

    fun spawnNpc(definitionId: Int, tile: Tile): Npc = Npc(nextIndex.getAndIncrement(), definitionId).also {
        it.tile = tile
        npcs += it
    }

    fun tick() {
        queue.drain()
        npcs.forEach(Entity::tick)
        players.forEach(Entity::tick)
        // DELTA G2: movement/pathfinding, DELTA G3: combat, DELTA G4: entity sync snapshots
        tickCount++
    }
}

/** Runnables posted from other threads, drained at the start of each tick. */
class GameQueue {
    private val queue = java.util.concurrent.ConcurrentLinkedQueue<Runnable>()
    fun post(r: Runnable) { queue += r }
    fun drain() { while (true) { (queue.poll() ?: return).run() } }
}

interface GameEvent
data class PlayerLoggedIn(val player: Player) : GameEvent
data class CommandEvent(val player: Player, val name: String, val args: List<String>) : GameEvent
data class NpcOptionEvent(val player: Player, val npc: Npc, val option: Int) : GameEvent
data class ItemOptionEvent(val player: Player, val itemId: Int, val slot: Int, val option: Int) : GameEvent

/** Minimal typed pub/sub. Scripts subscribe through :script, never directly. */
class EventBus {
    private val handlers = HashMap<Class<out GameEvent>, MutableList<(GameEvent) -> Unit>>()

    fun <E : GameEvent> subscribe(type: Class<E>, handler: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(type) { ArrayList() } += handler as (GameEvent) -> Unit
    }

    inline fun <reified E : GameEvent> subscribe(noinline handler: (E) -> Unit) = subscribe(E::class.java, handler)

    fun publish(event: GameEvent) {
        handlers[event.javaClass]?.forEach { it(event) }
    }
}
