package org.gielinorcore.cache.config

import org.gielinorcore.buffer.ReadBuffer
import org.gielinorcore.buffer.WriteBuffer

/**
 * Config types are opcode-tagged property lists. Field *meaning* is revision
 * independent; the *opcode numbers* are not, so each revision supplies a
 * [ConfigCodec] (DELTA C4).
 */
interface ConfigCodec<T : Any> {
    val archive: Int
    fun decode(id: Int, buffer: ReadBuffer): T
    fun encode(value: T, out: WriteBuffer)
}

data class ItemDefinition(
    val id: Int,
    var name: String = "null",
    var modelId: Int = 0,
    var zoom2d: Int = 2000,
    var stackable: Boolean = false,
    var value: Int = 1,
    var members: Boolean = false,
    var maleModel0: Int = -1,
    var femaleModel0: Int = -1,
    var groundOptions: Array<String?> = arrayOfNulls(5),
    var inventoryOptions: Array<String?> = arrayOfNulls(5),
    var recolorFrom: IntArray = IntArray(0),
    var recolorTo: IntArray = IntArray(0),
    var noteTemplateId: Int = -1,
    var params: MutableMap<Int, Any> = mutableMapOf(),
)

data class NpcDefinition(
    val id: Int,
    var name: String = "null",
    var models: IntArray = IntArray(0),
    var size: Int = 1,
    var standAnimation: Int = -1,
    var walkAnimation: Int = -1,
    var combatLevel: Int = -1,
    var options: Array<String?> = arrayOfNulls(5),
    var params: MutableMap<Int, Any> = mutableMapOf(),
)

data class ObjectDefinition(
    val id: Int,
    var name: String = "null",
    var models: IntArray = IntArray(0),
    var sizeX: Int = 1,
    var sizeY: Int = 1,
    var solid: Boolean = true,
    var options: Array<String?> = arrayOfNulls(5),
    var params: MutableMap<Int, Any> = mutableMapOf(),
)

data class SequenceDefinition(
    val id: Int,
    var frameIds: IntArray = IntArray(0),
    var frameDelays: IntArray = IntArray(0),
    var loopOffset: Int = -1,
    var priority: Int = 5,
    var replayMode: Int = 2,
)
