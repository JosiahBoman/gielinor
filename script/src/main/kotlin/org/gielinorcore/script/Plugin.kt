package org.gielinorcore.script

import org.gielinorcore.game.CommandEvent
import org.gielinorcore.game.ItemOptionEvent
import org.gielinorcore.game.NpcOptionEvent
import org.gielinorcore.game.World
import java.util.ServiceLoader

/**
 * The content API. Plugins are discovered with [ServiceLoader]; a plugin is a
 * Gradle module under `plugins/` that registers handlers through [ScriptContext].
 * Hot reload (DELTA S2) swaps the classloader and replays [Plugin.load].
 */
interface Plugin {
    val id: String
    fun load(ctx: ScriptContext)
}

class ScriptContext(val world: World, val pluginId: String) {
    fun onCommand(name: String, handler: (CommandEvent) -> Unit) =
        world.events.subscribe<CommandEvent> { if (it.name.equals(name, ignoreCase = true)) handler(it) }

    fun onNpcOption(definitionId: Int, option: Int, handler: (NpcOptionEvent) -> Unit) =
        world.events.subscribe<NpcOptionEvent> { if (it.npc.definitionId == definitionId && it.option == option) handler(it) }

    fun onItemOption(itemId: Int, option: Int, handler: (ItemOptionEvent) -> Unit) =
        world.events.subscribe<ItemOptionEvent> { if (it.itemId == itemId && it.option == option) handler(it) }
}

object PluginLoader {
    fun loadAll(world: World, loader: ClassLoader = Thread.currentThread().contextClassLoader): List<Plugin> =
        ServiceLoader.load(Plugin::class.java, loader).toList().onEach { it.load(ScriptContext(world, it.id)) }
}
